package id.ac.ub.athra.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.local.SessionExerciseEntity
import id.ac.ub.athra.data.local.WorkoutSessionEntity
import id.ac.ub.athra.data.local.WorkoutSetEntity
import id.ac.ub.athra.data.repository.WorkoutRepository
import id.ac.ub.athra.debugfixtures.installH1H2Fixtures
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.WeightUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutRepository

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.validationCallback)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(database, context.assets)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun finalizationPersistsSnapshotAndIsIdempotentAcrossAccounts() = runBlocking {
        repository.seedCatalog()
        repository.ensureProfile("user-a", "A", "a@example.test")
        repository.ensureProfile("user-b", "B", "b@example.test")

        val draft = repository.startOrResume("user-a", "Sesi uji", now = 1_000L)
        repository.addExercise("user-a", draft.session.id, "bench_press")
        repository.saveSet("user-a", draft.session.id, "bench_press", null, "10", "32", WeightUnit.KG)
        repository.enterReview("user-a", draft.session.id, endedAt = 2_000L)

        val completed = repository.finalizeWorkout("user-a", draft.session.id)
        val retry = repository.finalizeWorkout("user-a", draft.session.id)
        assertEquals(SessionStatus.COMPLETED, completed.draft.session.status)
        assertEquals(completed.draft.session.id, retry.draft.session.id)
        assertEquals(1, database.workoutDao().getSnapshots(draft.session.id).size)
        assertEquals(BigDecimal("32"), completed.personalRecords.single().currentMaxKg)
        assertNotNull(repository.getWorkoutDetail("user-a", draft.session.id))
        assertNull(repository.getWorkoutDetail("user-b", draft.session.id))

        val userA = repository.analytics("user-a", 7, java.time.ZoneId.of("UTC"), 3_000L)
        val userB = repository.analytics("user-b", 7, java.time.ZoneId.of("UTC"), 3_000L)
        assertEquals(1, userA.sessionCount)
        assertEquals(1, userA.setCount)
        assertEquals(0, userB.sessionCount)
        assertEquals(0, userB.setCount)
    }

    @Test
    fun oneDraftSlotResumesAndDiscardRemovesItsChildren() = runBlocking {
        repository.seedCatalog()
        val first = repository.startOrResume("user-a", "Workout", now = 1_000L)
        repository.addExercise("user-a", first.session.id, "squat")
        repository.saveSet("user-a", first.session.id, "squat", null, "5", "0", WeightUnit.KG)
        repository.saveSet("user-a", first.session.id, "squat", null, "5", "20", WeightUnit.KG)
        val resumed = repository.startOrResume("user-a", "Ignored", now = 9_000L)
        assertEquals(first.session.id, resumed.session.id)
        assertEquals(2, resumed.exercises.single().sets.size)
        repository.deleteSet("user-a", first.session.id, resumed.exercises.single().sets.first().id)
        assertEquals(1, repository.getDraft("user-a")?.exercises?.single()?.sets?.single()?.number)
        repository.discard("user-a", first.session.id)
        assertNull(repository.getDraft("user-a"))
        assertEquals(0, repository.analytics("user-a", 7, java.time.ZoneId.of("UTC"), 10_000L).sessionCount)
    }

    @Test
    fun finalizationRollsBackWhenSnapshotInsertFails() = runBlocking {
        repository.seedCatalog()
        val draft = repository.startOrResume("user-a", "Rollback", now = 1_000L)
        repository.addExercise("user-a", draft.session.id, "bench_press")
        repository.saveSet("user-a", draft.session.id, "bench_press", null, "10", "32", WeightUnit.KG)
        repository.enterReview("user-a", draft.session.id, endedAt = 2_000L)
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER fail_snapshot_insert
            BEFORE INSERT ON personal_record_snapshots
            BEGIN
                SELECT RAISE(ABORT, 'forced snapshot failure');
            END
            """.trimIndent(),
        )

        var failed = false
        try {
            repository.finalizeWorkout("user-a", draft.session.id)
        } catch (_: Exception) {
            failed = true
        }

        assertTrue("The forced snapshot insert should fail", failed)
        assertEquals("REVIEW", database.workoutDao().getSession(draft.session.id)?.status)
        assertNotNull(database.workoutDao().getActive("user-a"))
        assertTrue(database.workoutDao().getSnapshots(draft.session.id).isEmpty())
        assertNotNull(repository.getDraft("user-a"))
    }

    @Test
    fun activeDraftSurvivesDatabaseCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "athra-recovery-${java.util.UUID.randomUUID()}.db"
        var firstDatabase: AppDatabase? = null
        var reopenedDatabase: AppDatabase? = null
        var firstClosed = false
        try {
            val createdDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                .addCallback(AppDatabase.validationCallback)
                .build()
            firstDatabase = createdDatabase
            val firstRepository = WorkoutRepository(createdDatabase, context.assets)
            firstRepository.seedCatalog()
            val draft = firstRepository.startOrResume("recovery-user", "Pulihkan draft", now = 1_000L)
            firstRepository.addExercise("recovery-user", draft.session.id, "bench_press")
            firstRepository.saveSet("recovery-user", draft.session.id, "bench_press", null, "10", "32", WeightUnit.KG)
            createdDatabase.close()
            firstClosed = true

            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                .addCallback(AppDatabase.validationCallback)
                .build()
            reopenedDatabase = reopened
            val recovered = WorkoutRepository(reopened, context.assets).getDraft("recovery-user")

            assertNotNull(recovered)
            assertEquals(draft.session.id, recovered?.session?.id)
            assertEquals(BigDecimal("32"), recovered?.exercises?.single()?.sets?.single()?.weightKg)
        } finally {
            if (!firstClosed) firstDatabase?.close()
            reopenedDatabase?.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun reviewAndEmptyHistoryDoNotCreateAnalyticsData() = runBlocking {
        repository.seedCatalog()
        val review = repository.startOrResume("user-a", "Belum disimpan", now = 1_000L)
        repository.addExercise("user-a", review.session.id, "bench_press")
        repository.saveSet("user-a", review.session.id, "bench_press", null, "10", "32", WeightUnit.KG)
        repository.enterReview("user-a", review.session.id, endedAt = 2_000L)

        val summary = repository.analytics("user-a", 7, java.time.ZoneId.of("UTC"), 3_000L)

        assertEquals(0, summary.sessionCount)
        assertEquals(0, summary.setCount)
        assertEquals(0, summary.repCount)
        assertEquals(BigDecimal.ZERO, summary.volumeKg)
        assertNull(summary.favoriteExercise)
        assertEquals(0, summary.favoriteSessionCount)
    }

    @Test
    fun progressiveReferenceUsesMostRecentSessionRatherThanMaximumLoad() = runBlocking {
        repository.seedCatalog()
        suspend fun complete(load: String, startedAt: Long): String {
            val draft = repository.startOrResume("user-a", "Workout", startedAt)
            repository.addExercise("user-a", draft.session.id, "bench_press")
            repository.saveSet("user-a", draft.session.id, "bench_press", null, "8", load, WeightUnit.KG)
            repository.enterReview("user-a", draft.session.id, startedAt + 500)
            repository.finalizeWorkout("user-a", draft.session.id)
            return draft.session.id
        }
        complete("30", 1_000)
        val latestSessionId = complete("25", 2_000)

        val reference = repository.latestReference("user-a", "bench_press")
        assertNotNull(reference)
        assertEquals(latestSessionId, reference?.session?.id)
        assertEquals(BigDecimal("25"), reference?.sets?.single()?.weightKg)
        assertEquals(BigDecimal("30"), repository.historicalMax("user-a", "bench_press"))
    }

    @Test
    fun h1H2FixturesResetByUidAndPreserveH3AndOrdinaryHistory() = runBlocking {
        repository.seedCatalog()
        val zone = ZoneId.of("Asia/Jakarta")
        fun timestamp(value: String) = LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli()
        val analyticsNow = timestamp("2026-11-14T12:00:00")

        installH1H2Fixtures(database, "user-a")
        installH1H2Fixtures(database, "user-a")

        val h1Id = "athra-demo-user-a-H1"
        val h2Id = "athra-demo-user-a-H2"
        assertEquals(2, database.workoutDao().getCompleted("user-a").size)
        val beforeH3 = repository.analytics("user-a", 7, zone, analyticsNow)
        assertEquals(2, beforeH3.sessionCount)
        assertEquals(4, beforeH3.setCount)
        assertEquals(38, beforeH3.repCount)
        assertEquals(BigDecimal("1190"), beforeH3.volumeKg)
        assertEquals(BigDecimal("30"), repository.historicalMax("user-a", "bench_press"))
        assertEquals(BigDecimal("25"), repository.latestReference("user-a", "bench_press")?.sets?.single()?.weightKg)

        val h1Snapshot = database.workoutDao().getSnapshots(h1Id).single()
        assertEquals("BASELINE", h1Snapshot.status)
        assertNull(h1Snapshot.previousMaxKg)
        assertEquals(BigDecimal("30"), h1Snapshot.currentMaxKg)
        val h2Snapshots = database.workoutDao().getSnapshots(h2Id).associateBy { it.exerciseId }
        assertEquals("BELOW_RECORD", h2Snapshots.getValue("bench_press").status)
        assertEquals(BigDecimal("30"), h2Snapshots.getValue("bench_press").previousMaxKg)
        assertEquals("BASELINE", h2Snapshots.getValue("squat").status)

        val ordinaryStart = timestamp("2026-11-01T17:00:00")
        val ordinary = repository.startOrResume("user-a", "Riwayat lama", ordinaryStart)
        repository.addExercise("user-a", ordinary.session.id, "bench_press")
        repository.saveSet("user-a", ordinary.session.id, "bench_press", null, "2", "5", WeightUnit.KG)
        repository.enterReview("user-a", ordinary.session.id, timestamp("2026-11-01T18:00:00"))
        repository.finalizeWorkout("user-a", ordinary.session.id)

        val h3Start = timestamp("2026-11-14T10:00:00")
        val h3 = repository.startOrResume("user-a", "H3", h3Start)
        repository.addExercise("user-a", h3.session.id, "bench_press")
        repository.saveSet("user-a", h3.session.id, "bench_press", null, "10", "32", WeightUnit.KG)
        repository.saveSet("user-a", h3.session.id, "bench_press", null, "10", "30", WeightUnit.KG)
        repository.addExercise("user-a", h3.session.id, "squat")
        repository.saveSet("user-a", h3.session.id, "squat", null, "10", "40", WeightUnit.KG)
        repository.enterReview("user-a", h3.session.id, timestamp("2026-11-14T11:00:00"))
        val completedH3 = repository.finalizeWorkout("user-a", h3.session.id)
        assertEquals("NEW_RECORD", completedH3.personalRecords.single { it.exerciseId == "bench_press" }.status.name)

        installH1H2Fixtures(database, "user-a")

        val completedIds = database.workoutDao().getCompleted("user-a").map { it.sessionId }.toSet()
        assertEquals(setOf(h1Id, h2Id, h3.session.id, ordinary.session.id), completedIds)
        assertEquals(4, database.workoutDao().getCompleted("user-a").size)
        assertEquals(2, database.workoutDao().getSnapshots(h3.session.id).size)
        assertTrue(completedIds.contains(ordinary.session.id))
        val afterH3 = repository.analytics("user-a", 7, zone, analyticsNow)
        assertEquals(3, afterH3.sessionCount)
        assertEquals(7, afterH3.setCount)
        assertEquals(68, afterH3.repCount)
        assertEquals(BigDecimal("2210"), afterH3.volumeKg)
        assertEquals(BigDecimal("32"), repository.historicalMax("user-a", "bench_press"))
        assertEquals(h3.session.id, repository.latestReference("user-a", "bench_press")?.session?.id)

        assertTrue(database.workoutDao().getCompleted("user-b").isEmpty())
        assertEquals(0, repository.analytics("user-b", 7, zone, analyticsNow).sessionCount)
        assertNull(repository.historicalMax("user-b", "bench_press"))
    }

    @Test
    fun analyticsFixtureTenThousandSetsLogsElapsedTime() = runBlocking {
        repository.seedCatalog()
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.workoutDao().insertSession(
                WorkoutSessionEntity("benchmark-session", "benchmark-user", "Benchmark", now - 60_000, now, "COMPLETED", now - 60_000, now),
            )
            database.workoutDao().insertSessionExercise(
                SessionExerciseEntity("benchmark-exercise", "benchmark-session", "bench_press", 0),
            )
            database.workoutDao().insertSets((1..10_000).map { index ->
                WorkoutSetEntity("benchmark-set-$index", "benchmark-exercise", index, 10, BigDecimal("32"))
            })
        }

        val startedAt = SystemClock.elapsedRealtime()
        val result = repository.analytics("benchmark-user", 7, java.time.ZoneId.systemDefault(), now)
        val elapsedMs = SystemClock.elapsedRealtime() - startedAt
        Log.i("AthraBenchmark", "analytics 10,000 sets elapsedMs=$elapsedMs")
        assertEquals(1, result.sessionCount)
        assertEquals(10_000, result.setCount)
        assertEquals(100_000, result.repCount)
        assertEquals(BigDecimal("3200000"), result.volumeKg)
        assertTrue("Analytics took ${elapsedMs}ms (target < 2000ms)", elapsedMs < 2_000)
    }
}
