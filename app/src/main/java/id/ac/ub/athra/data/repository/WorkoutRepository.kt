package id.ac.ub.athra.data.repository

import android.content.res.AssetManager
import androidx.room.withTransaction
import id.ac.ub.athra.data.local.*
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.parseReps
import id.ac.ub.athra.domain.calculation.parseWeightKg
import id.ac.ub.athra.domain.calculation.personalRecord
import id.ac.ub.athra.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.math.BigDecimal
import java.util.UUID

class WorkoutRepository(
    private val database: AppDatabase,
    private val assets: AssetManager,
) {
    private val workout get() = database.workoutDao()
    private val exercises get() = database.exerciseDao()

    suspend fun seedCatalog() = withContext(Dispatchers.IO) {
        val json = assets.open("exercises.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val items = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            ExerciseEntity(
                exerciseId = item.getString("id"),
                name = item.getString("name"),
                primaryMuscleGroup = item.getString("muscle"),
                equipment = item.getString("equipment"),
                instructions = item.getString("instructions"),
            )
        }
        exercises.insertAll(items)
    }

    suspend fun ensureProfile(userId: String, name: String, email: String) {
        val dao = database.profileDao()
        if (dao.get(userId) == null) dao.upsert(LocalProfileEntity(userId, name, email, System.currentTimeMillis()))
    }

    suspend fun startOrResume(userId: String, requestedName: String?, now: Long = System.currentTimeMillis()): WorkoutDraft =
        database.withTransaction {
            workout.getActive(userId)?.let { active ->
                loadDraft(userId, active.sessionId)?.let { return@withTransaction it }
            }
            val name = requestedName?.trim().takeUnless { it.isNullOrEmpty() } ?: "Workout"
            val sessionId = UUID.randomUUID().toString()
            workout.insertSession(WorkoutSessionEntity(sessionId, userId, name, now, null, "ACTIVE", now, now))
            workout.insertActive(ActiveWorkoutEntity(userId, sessionId))
            WorkoutDraft(session(userId, sessionId), emptyList())
        }

    suspend fun getDraft(userId: String): WorkoutDraft? = database.withTransaction {
        workout.getActive(userId)?.let { loadDraft(userId, it.sessionId) }
    }

    suspend fun addExercise(userId: String, sessionId: String, exerciseId: String) = database.withTransaction {
        val session = requireOwned(userId, sessionId, setOf(SessionStatus.ACTIVE))
        require(exercises.get(exerciseId) != null) { "Gerakan tidak tersedia." }
        if (workout.getSessionExercise(sessionId, exerciseId) == null) {
            val position = workout.getSessionExercises(sessionId).size
            workout.insertSessionExercise(SessionExerciseEntity(UUID.randomUUID().toString(), sessionId, exerciseId, position))
        }
        session
    }

    suspend fun saveSet(
        userId: String,
        sessionId: String,
        exerciseId: String,
        setId: String?,
        repsText: String,
        weightText: String,
        unit: WeightUnit,
    ): WorkoutDraft = database.withTransaction {
        requireOwned(userId, sessionId, setOf(SessionStatus.ACTIVE))
        val reps = parseReps(repsText) ?: error("Repetisi harus berupa bilangan bulat lebih dari nol.")
        val weight = parseWeightKg(weightText, unit) ?: error("Beban harus berupa angka nol atau lebih.")
        val exercise = workout.getSessionExercise(sessionId, exerciseId) ?: error("Tambahkan gerakan terlebih dahulu.")
        val sets = workout.getSets(exercise.sessionExerciseId)
        val old = setId?.let { workout.getSet(it) }
        if (old != null) require(old.sessionExerciseId == exercise.sessionExerciseId) { "Set tidak ditemukan." }
        val number = old?.setNumber ?: (sets.maxOfOrNull { it.setNumber } ?: 0) + 1
        workout.upsertSet(WorkoutSetEntity(old?.setId ?: UUID.randomUUID().toString(), exercise.sessionExerciseId, number, reps, weight))
        loadDraft(userId, sessionId)!!
    }

    suspend fun deleteSet(userId: String, sessionId: String, setId: String): WorkoutDraft = database.withTransaction {
        requireOwned(userId, sessionId, setOf(SessionStatus.ACTIVE))
        val set = workout.getSet(setId) ?: error("Set tidak ditemukan.")
        val parent = workout.getSessionExercises(sessionId).firstOrNull { it.sessionExerciseId == set.sessionExerciseId }
            ?: error("Set tidak ditemukan.")
        workout.deleteSet(setId)
        val remaining = workout.getSets(parent.sessionExerciseId)
        remaining.forEachIndexed { index, row -> workout.updateSetNumber(row.setId, -(index + 1)) }
        remaining.forEachIndexed { index, row -> workout.updateSetNumber(row.setId, index + 1) }
        loadDraft(userId, sessionId)!!
    }

    suspend fun removeExercise(userId: String, sessionId: String, exerciseId: String) = database.withTransaction {
        requireOwned(userId, sessionId, setOf(SessionStatus.ACTIVE))
        workout.deleteSessionExercise(sessionId, exerciseId)
        workout.getSessionExercises(sessionId).forEachIndexed { index, item -> workout.updatePosition(item.sessionExerciseId, index) }
    }

    suspend fun enterReview(userId: String, sessionId: String, endedAt: Long): WorkoutDraft = database.withTransaction {
        val current = loadDraft(userId, sessionId) ?: error("Sesi tidak tersedia.")
        require(current.session.status == SessionStatus.ACTIVE) { "Sesi tidak sedang aktif." }
        require(calculateTotals(current).setCount > 0) { "Tambahkan minimal satu set yang valid." }
        workout.updateSession(current.session.copy(endedAt = endedAt, status = SessionStatus.REVIEW).toEntity())
        loadDraft(userId, sessionId)!!
    }

    suspend fun resume(userId: String, sessionId: String): WorkoutDraft = database.withTransaction {
        val current = loadDraft(userId, sessionId) ?: error("Sesi tidak tersedia.")
        require(current.session.status == SessionStatus.REVIEW) { "Sesi tidak dalam ringkasan." }
        workout.updateSession(current.session.copy(endedAt = null, status = SessionStatus.ACTIVE).toEntity())
        loadDraft(userId, sessionId)!!
    }

    suspend fun discard(userId: String, sessionId: String) = database.withTransaction {
        requireOwned(userId, sessionId, setOf(SessionStatus.ACTIVE, SessionStatus.REVIEW))
        workout.deleteActive(userId, sessionId)
        workout.deleteDraft(userId, sessionId)
    }

    private suspend fun requireOwned(userId: String, sessionId: String, states: Set<SessionStatus>): WorkoutSession {
        val session = workout.getSession(sessionId)?.toModel() ?: error("Sesi tidak tersedia.")
        require(session.userId == userId) { "Sesi tidak tersedia." }
        require(session.status in states) { "Sesi tidak dapat diubah pada status ini." }
        return session
    }

    private suspend fun session(userId: String, sessionId: String): WorkoutSession =
        workout.getSession(sessionId)?.toModel()?.takeIf { it.userId == userId } ?: error("Sesi tidak tersedia.")

    private suspend fun loadDraft(userId: String, sessionId: String): WorkoutDraft? {
        val session = session(userId, sessionId)
        if (session.status == SessionStatus.COMPLETED) return null
        val rows = workout.getSessionExercises(sessionId)
        val domainExercises = rows.mapNotNull { row ->
            val exercise = exercises.get(row.exerciseId)?.toModel() ?: return@mapNotNull null
            val sets = workout.getSets(row.sessionExerciseId).map { it.toModel() }
            WorkoutExercise(exercise, row.position, sets)
        }
        return WorkoutDraft(session, domainExercises)
    }
}

private fun WorkoutSession.toEntity() = WorkoutSessionEntity(id, userId, name, startedAt, endedAt, status.name, startedAt, System.currentTimeMillis())
private fun WorkoutSessionEntity.toModel() = WorkoutSession(sessionId, userId, name, startedAt, endedAt, SessionStatus.valueOf(status))
private fun ExerciseEntity.toModel() = Exercise(exerciseId, name, primaryMuscleGroup, equipment, instructions)
private fun WorkoutSetEntity.toModel() = WorkoutSet(setId, setNumber, reps, weightKg)
