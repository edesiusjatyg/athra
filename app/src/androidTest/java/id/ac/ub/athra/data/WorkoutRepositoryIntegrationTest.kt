package id.ac.ub.athra.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.repository.WorkoutRepository
import id.ac.ub.athra.domain.model.SessionStatus
import id.ac.ub.athra.domain.model.WeightUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

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
}
