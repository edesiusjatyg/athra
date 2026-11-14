package id.ac.ub.athra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

data class AnalyticsSetRow(
    val sessionId: String,
    val endedAt: Long,
    val exerciseId: String,
    val name: String,
    val primaryMuscleGroup: String,
    val equipment: String,
    val instructions: String,
    val reps: Int,
    val weightKg: BigDecimal,
)

data class ReferenceSetRow(
    val sessionId: String,
    val userId: String,
    val sessionName: String,
    val startedAt: Long,
    val endedAt: Long,
    val exerciseId: String,
    val exerciseName: String,
    val primaryMuscleGroup: String,
    val equipment: String,
    val instructions: String,
    val setId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: BigDecimal,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM local_profiles WHERE userId = :userId")
    suspend fun get(userId: String): LocalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: LocalProfileEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE exerciseId = :id")
    suspend fun get(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)
}

@Dao
interface WorkoutDao {
    @Query("""
        SELECT s.sessionId AS sessionId, COALESCE(s.endedAt, s.startedAt) AS endedAt, e.exerciseId AS exerciseId,
            e.name AS name, e.primaryMuscleGroup AS primaryMuscleGroup, e.equipment AS equipment,
            e.instructions AS instructions, ws.reps AS reps, ws.weightKg AS weightKg
        FROM workout_sessions s
        JOIN session_exercises se ON se.sessionId = s.sessionId
        JOIN exercises e ON e.exerciseId = se.exerciseId
        JOIN workout_sets ws ON ws.sessionExerciseId = se.sessionExerciseId
        WHERE s.userId = :userId AND s.status = 'COMPLETED' AND s.endedAt BETWEEN :from AND :to
        ORDER BY s.endedAt DESC, s.sessionId, se.position, ws.setNumber
    """)
    suspend fun getAnalyticsRows(userId: String, from: Long, to: Long): List<AnalyticsSetRow>

    @Query("""
        SELECT s.sessionId AS sessionId, s.userId AS userId, s.name AS sessionName,
            s.startedAt AS startedAt, COALESCE(s.endedAt, s.startedAt) AS endedAt, e.exerciseId AS exerciseId,
            e.name AS exerciseName, e.primaryMuscleGroup AS primaryMuscleGroup,
            e.equipment AS equipment, e.instructions AS instructions, ws.setId AS setId,
            ws.setNumber AS setNumber, ws.reps AS reps, ws.weightKg AS weightKg
        FROM workout_sessions s
        JOIN session_exercises se ON se.sessionId = s.sessionId
        JOIN exercises e ON e.exerciseId = se.exerciseId
        JOIN workout_sets ws ON ws.sessionExerciseId = se.sessionExerciseId
        WHERE s.userId = :userId AND s.status = 'COMPLETED'
            AND NOT EXISTS (
                SELECT 1 FROM workout_sessions newer
                JOIN session_exercises newerSe ON newerSe.sessionId = newer.sessionId
                JOIN workout_sets newerSet ON newerSet.sessionExerciseId = newerSe.sessionExerciseId
                WHERE newer.userId = s.userId AND newer.status = 'COMPLETED'
                    AND newerSe.exerciseId = se.exerciseId
                    AND (newer.endedAt > s.endedAt OR
                        (newer.endedAt = s.endedAt AND
                            (newer.createdAt > s.createdAt OR
                                (newer.createdAt = s.createdAt AND newer.sessionId > s.sessionId))))
            )
        ORDER BY e.exerciseId, ws.setNumber
    """)
    suspend fun getLatestReferenceRows(userId: String): List<ReferenceSetRow>

    @Query("""
        SELECT ws.weightKg
        FROM workout_sessions s
        JOIN session_exercises se ON se.sessionId = s.sessionId
        JOIN workout_sets ws ON ws.sessionExerciseId = se.sessionExerciseId
        WHERE s.userId = :userId AND s.status = 'COMPLETED' AND se.exerciseId = :exerciseId
            AND (:excludingSessionId IS NULL OR s.sessionId != :excludingSessionId)
    """)
    suspend fun getCompletedWeights(userId: String, exerciseId: String, excludingSessionId: String?): List<BigDecimal>

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED' ORDER BY endedAt DESC, createdAt DESC, sessionId")
    fun observeHistory(userId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED' ORDER BY endedAt DESC, createdAt DESC, sessionId")
    suspend fun getCompleted(userId: String): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE sessionId = :sessionId AND userId = :userId AND status IN ('ACTIVE', 'REVIEW')")
    suspend fun deleteDraft(userId: String, sessionId: String)

    @androidx.room.Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM active_workouts WHERE userId = :userId")
    suspend fun getActive(userId: String): ActiveWorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActive(active: ActiveWorkoutEntity)

    @Query("DELETE FROM active_workouts WHERE userId = :userId AND sessionId = :sessionId")
    suspend fun deleteActive(userId: String, sessionId: String)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getSessionExercises(sessionId: String): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun getSessionExercise(sessionId: String, exerciseId: String): SessionExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSessionExercise(exercise: SessionExerciseEntity)

    @Query("UPDATE session_exercises SET position = :position WHERE sessionExerciseId = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Query("DELETE FROM session_exercises WHERE sessionExerciseId = :id")
    suspend fun deleteSessionExercise(id: String)

    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteSessionExercise(sessionId: String, exerciseId: String)

    @Query("SELECT * FROM workout_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber")
    suspend fun getSets(sessionExerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE setId = :setId")
    suspend fun getSet(setId: String): WorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_sets WHERE setId = :setId")
    suspend fun deleteSet(setId: String)

    @Query("UPDATE workout_sets SET setNumber = :number WHERE setId = :setId")
    suspend fun updateSetNumber(setId: String, number: Int)

    @Query("SELECT * FROM personal_record_snapshots WHERE sessionId = :sessionId ORDER BY exerciseId")
    suspend fun getSnapshots(sessionId: String): List<PersonalRecordSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshots(snapshots: List<PersonalRecordSnapshotEntity>)
}
