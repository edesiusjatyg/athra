package id.ac.ub.athra.data.repository

import android.content.res.AssetManager
import androidx.room.withTransaction
import id.ac.ub.athra.data.local.*
import id.ac.ub.athra.domain.calculation.calculateTotals
import id.ac.ub.athra.domain.calculation.parseReps
import id.ac.ub.athra.domain.calculation.parseWeightKg
import id.ac.ub.athra.domain.calculation.personalRecord
import id.ac.ub.athra.domain.calculation.calculateAnalytics
import id.ac.ub.athra.domain.calculation.AnalyticsInputExercise
import id.ac.ub.athra.domain.calculation.AnalyticsInputSession
import id.ac.ub.athra.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.math.BigDecimal
import java.util.UUID

class WorkoutRepository(
    private val database: AppDatabase,
    private val assets: AssetManager,
) {
    private val workout get() = database.workoutDao()
    private val exercises get() = database.exerciseDao()

    fun observeCatalog(): Flow<List<Exercise>> = exercises.observeAll().map { rows -> rows.map { it.toModel() } }

    fun observeHistory(userId: String): Flow<List<WorkoutSession>> =
        workout.observeHistory(userId).map { rows -> rows.map { it.toModel() } }

    suspend fun getProfile(userId: String): LocalProfileEntity? = database.profileDao().get(userId)

    suspend fun updateProfileName(userId: String, name: String) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Nama tidak boleh kosong." }
        val profile = database.profileDao().get(userId) ?: error("Profil tidak tersedia.")
        database.profileDao().upsert(profile.copy(displayName = cleanName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun analytics(userId: String, periodDays: Int, zone: java.time.ZoneId, now: Long): AnalyticsSummary {
        val sessions = workout.getCompleted(userId).map { row ->
            val endTime = row.endedAt ?: row.startedAt
            val items = workout.getSessionExercises(row.sessionId).mapNotNull { item ->
                val exercise = exercises.get(item.exerciseId)?.toModel() ?: return@mapNotNull null
                val sets = workout.getSets(item.sessionExerciseId)
                AnalyticsInputExercise(exercise, sets.map { it.reps to it.weightKg })
            }
            AnalyticsInputSession(endTime, items)
        }
        return calculateAnalytics(sessions, now, periodDays, zone)
    }

    suspend fun previewRecords(userId: String, draft: WorkoutDraft): List<PersonalRecord> {
        val records = mutableListOf<PersonalRecord>()
        draft.exercises.forEach { item ->
            personalRecord(item.exercise.id, item.sets.map { it.weightKg }, historicalMax(userId, item.exercise.id))
                ?.let(records::add)
        }
        return records
    }

    suspend fun getReferenceComparisons(userId: String, exerciseId: String, currentSets: List<WorkoutSet>): List<SetComparison> {
        val reference = latestReference(userId, exerciseId) ?: return emptyList()
        val previousByNumber = reference.sets.associateBy { it.number }
        return currentSets.map { current ->
            val previous = previousByNumber[current.number]
            SetComparison(
                previousSet = previous,
                currentSet = current,
                weightDeltaKg = previous?.let { current.weightKg.subtract(it.weightKg) },
                repsDelta = previous?.let { current.reps - it.reps },
            )
        }
    }

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

    suspend fun finalizeWorkout(userId: String, sessionId: String): CompletedWorkout = database.withTransaction {
        val session = workout.getSession(sessionId)?.toModel()?.takeIf { it.userId == userId }
            ?: error("Sesi tidak tersedia.")
        if (session.status == SessionStatus.COMPLETED) return@withTransaction loadCompleted(userId, sessionId)!!
        require(session.status == SessionStatus.REVIEW && session.endedAt != null) { "Sesi belum siap disimpan." }
        val draft = loadDraft(userId, sessionId) ?: error("Sesi tidak tersedia.")
        require(calculateTotals(draft).setCount > 0) { "Tambahkan minimal satu set yang valid." }
        val completedBefore = workout.getCompleted(userId).filter { it.sessionId != sessionId }
        val records = mutableListOf<PersonalRecord>()
        draft.exercises.filter { it.sets.isNotEmpty() }.forEach { current ->
            val previousWeights = mutableListOf<BigDecimal>()
            completedBefore.forEach { historicalSession ->
                workout.getSessionExercises(historicalSession.sessionId)
                    .filter { it.exerciseId == current.exercise.id }
                    .forEach { item -> workout.getSets(item.sessionExerciseId).forEach { previousWeights += it.weightKg } }
            }
            personalRecord(current.exercise.id, current.sets.map { it.weightKg }, previousWeights.maxOrNull())?.let(records::add)
        }
        workout.insertSnapshots(records.map { record ->
            PersonalRecordSnapshotEntity(
                sessionId = sessionId,
                exerciseId = record.exerciseId,
                previousMaxKg = record.previousMaxKg,
                currentMaxKg = record.currentMaxKg,
                status = record.status.name,
            )
        })
        workout.updateSession(session.copy(status = SessionStatus.COMPLETED).toEntity())
        workout.deleteActive(userId, sessionId)
        loadCompleted(userId, sessionId)!!
    }

    suspend fun historicalMax(userId: String, exerciseId: String, excludingSessionId: String? = null): BigDecimal? {
        val weights = mutableListOf<BigDecimal>()
        workout.getCompleted(userId).filter { it.sessionId != excludingSessionId }.forEach { session ->
            workout.getSessionExercises(session.sessionId).filter { it.exerciseId == exerciseId }.forEach { item ->
                workout.getSets(item.sessionExerciseId).forEach { weights += it.weightKg }
            }
        }
        return weights.maxOrNull()
    }

    suspend fun latestReference(userId: String, exerciseId: String): ExerciseReference? {
        for (session in workout.getCompleted(userId)) {
            val item = workout.getSessionExercise(session.sessionId, exerciseId) ?: continue
            val sets = workout.getSets(item.sessionExerciseId).map { it.toModel() }
            if (sets.isNotEmpty()) {
                val volume = sets.fold(BigDecimal.ZERO) { total, set ->
                    total.add(set.weightKg.multiply(BigDecimal.valueOf(set.reps.toLong())))
                }
                return ExerciseReference(session.toModel(), sets, volume)
            }
        }
        return null
    }

    suspend fun getCompletedWorkout(userId: String, sessionId: String): CompletedWorkout? =
        database.withTransaction { loadCompleted(userId, sessionId) }

    suspend fun getWorkoutDetail(userId: String, sessionId: String): CompletedWorkout? = getCompletedWorkout(userId, sessionId)

    private suspend fun loadCompleted(userId: String, sessionId: String): CompletedWorkout? {
        val session = workout.getSession(sessionId)?.toModel()?.takeIf {
            it.userId == userId && it.status == SessionStatus.COMPLETED
        } ?: return null
        val draft = loadContents(session) ?: return null
        val records = workout.getSnapshots(sessionId).map {
            PersonalRecord(it.exerciseId, it.previousMaxKg, it.currentMaxKg, PersonalRecordStatus.valueOf(it.status))
        }
        return CompletedWorkout(draft, records)
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
        return loadContents(session)
    }

    private suspend fun loadContents(session: WorkoutSession): WorkoutDraft? {
        val sessionId = session.id
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
