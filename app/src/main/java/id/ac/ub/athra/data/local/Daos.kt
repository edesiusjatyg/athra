package id.ac.ub.athra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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
    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY endedAt DESC, createdAt DESC, sessionId")
    fun observeHistory(userId: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED' ORDER BY endedAt DESC, createdAt DESC, sessionId")
    suspend fun getCompleted(userId: String): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)

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

    @Query("DELETE FROM session_exercises WHERE sessionExerciseId = :id")
    suspend fun deleteSessionExercise(id: String)

    @Query("SELECT * FROM workout_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber")
    suspend fun getSets(sessionExerciseId: String): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE setId = :setId")
    suspend fun deleteSet(setId: String)

    @Query("SELECT * FROM personal_record_snapshots WHERE sessionId = :sessionId ORDER BY exerciseId")
    suspend fun getSnapshots(sessionId: String): List<PersonalRecordSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshots(snapshots: List<PersonalRecordSnapshotEntity>)
}
