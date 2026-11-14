package id.ac.ub.athra.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "local_profiles")
data class LocalProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String,
    val updatedAt: Long,
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val exerciseId: String,
    val name: String,
    val primaryMuscleGroup: String,
    val equipment: String,
    val instructions: String,
    val catalogVersion: Int = 1,
)

@Entity(tableName = "workout_sessions", indices = [Index("userId"), Index("status", "endedAt")])
data class WorkoutSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "active_workouts",
    indices = [Index("sessionId")],
    foreignKeys = [ForeignKey(
        entity = WorkoutSessionEntity::class,
        parentColumns = ["sessionId"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ActiveWorkoutEntity(
    @PrimaryKey val userId: String,
    val sessionId: String,
)

@Entity(
    tableName = "session_exercises",
    indices = [Index("sessionId"), Index("exerciseId"), Index(value = ["sessionId", "exerciseId"], unique = true)],
    foreignKeys = [
        ForeignKey(WorkoutSessionEntity::class, ["sessionId"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["exerciseId"], ["exerciseId"]),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey val sessionExerciseId: String,
    val sessionId: String,
    val exerciseId: String,
    val position: Int,
)

@Entity(
    tableName = "workout_sets",
    indices = [Index(value = ["sessionExerciseId", "setNumber"], unique = true)],
    foreignKeys = [ForeignKey(
        SessionExerciseEntity::class,
        ["sessionExerciseId"],
        ["sessionExerciseId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class WorkoutSetEntity(
    @PrimaryKey val setId: String,
    val sessionExerciseId: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: BigDecimal,
)

@Entity(
    tableName = "personal_record_snapshots",
    primaryKeys = ["sessionId", "exerciseId"],
    foreignKeys = [ForeignKey(
        WorkoutSessionEntity::class,
        ["sessionId"],
        ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class PersonalRecordSnapshotEntity(
    val sessionId: String,
    val exerciseId: String,
    val previousMaxKg: BigDecimal?,
    val currentMaxKg: BigDecimal,
    val status: String,
)
