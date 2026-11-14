package id.ac.ub.athra.debugfixtures

import androidx.room.withTransaction
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.local.PersonalRecordSnapshotEntity
import id.ac.ub.athra.data.local.SessionExerciseEntity
import id.ac.ub.athra.data.local.WorkoutSessionEntity
import id.ac.ub.athra.data.local.WorkoutSetEntity
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

/** Replaces only the deterministic H1/H2 rows for this UID; real history and H3 are untouched. */
suspend fun installH1H2Fixtures(database: AppDatabase, userId: String) {
    require(userId.isNotBlank()) { "UID tidak boleh kosong." }

    val h1SessionId = fixtureSessionId(userId, "H1")
    val h2SessionId = fixtureSessionId(userId, "H2")
    val bench = requireNotNull(database.exerciseDao().get("bench_press")) { "Katalog Bench Press belum tersedia." }
    val squat = requireNotNull(database.exerciseDao().get("squat")) { "Katalog Squat belum tersedia." }
    val zone = ZoneId.of("Asia/Jakarta")
    val h1StartedAt = LocalDateTime.parse("2026-11-10T17:00:00").atZone(zone).toInstant().toEpochMilli()
    val h1EndedAt = LocalDateTime.parse("2026-11-10T18:00:00").atZone(zone).toInstant().toEpochMilli()
    val h2StartedAt = LocalDateTime.parse("2026-11-12T17:00:00").atZone(zone).toInstant().toEpochMilli()
    val h2EndedAt = LocalDateTime.parse("2026-11-12T18:00:00").atZone(zone).toInstant().toEpochMilli()
    val h1BenchExerciseId = "$h1SessionId-bench"
    val h2BenchExerciseId = "$h2SessionId-bench"
    val h2SquatExerciseId = "$h2SessionId-squat"

    database.withTransaction {
        val dao = database.workoutDao()
        dao.deleteSessionsByIds(userId, listOf(h1SessionId, h2SessionId))

        dao.insertSession(
            WorkoutSessionEntity(
                sessionId = h1SessionId,
                userId = userId,
                name = "H1",
                startedAt = h1StartedAt,
                endedAt = h1EndedAt,
                status = "COMPLETED",
                createdAt = h1EndedAt,
                updatedAt = h1EndedAt,
            ),
        )
        dao.insertSessionExercise(SessionExerciseEntity(h1BenchExerciseId, h1SessionId, bench.exerciseId, 0))
        dao.insertSets(
            listOf(
                WorkoutSetEntity("$h1BenchExerciseId-set-1", h1BenchExerciseId, 1, 10, BigDecimal("30")),
                WorkoutSetEntity("$h1BenchExerciseId-set-2", h1BenchExerciseId, 2, 8, BigDecimal("30")),
            ),
        )
        dao.insertSnapshots(
            listOf(PersonalRecordSnapshotEntity(h1SessionId, bench.exerciseId, null, BigDecimal("30"), "BASELINE")),
        )

        dao.insertSession(
            WorkoutSessionEntity(
                sessionId = h2SessionId,
                userId = userId,
                name = "H2",
                startedAt = h2StartedAt,
                endedAt = h2EndedAt,
                status = "COMPLETED",
                createdAt = h2EndedAt,
                updatedAt = h2EndedAt,
            ),
        )
        dao.insertSessionExercise(SessionExerciseEntity(h2BenchExerciseId, h2SessionId, bench.exerciseId, 0))
        dao.insertSessionExercise(SessionExerciseEntity(h2SquatExerciseId, h2SessionId, squat.exerciseId, 1))
        dao.insertSets(
            listOf(
                WorkoutSetEntity("$h2BenchExerciseId-set-1", h2BenchExerciseId, 1, 10, BigDecimal("25")),
                WorkoutSetEntity("$h2SquatExerciseId-set-1", h2SquatExerciseId, 1, 10, BigDecimal("40")),
            ),
        )
        dao.insertSnapshots(
            listOf(
                PersonalRecordSnapshotEntity(h2SessionId, bench.exerciseId, BigDecimal("30"), BigDecimal("25"), "BELOW_RECORD"),
                PersonalRecordSnapshotEntity(h2SessionId, squat.exerciseId, null, BigDecimal("40"), "BASELINE"),
            ),
        )
    }
}

private fun fixtureSessionId(userId: String, name: String) = "athra-demo-${userId}-$name"
