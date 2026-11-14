package id.ac.ub.athra.domain.model

import java.math.BigDecimal

enum class SessionStatus { ACTIVE, REVIEW, COMPLETED }
enum class WeightUnit { KG, LB }
enum class PersonalRecordStatus { BASELINE, NEW_RECORD, MATCHED, BELOW_RECORD }

data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscleGroup: String,
    val equipment: String,
    val instructions: String,
)

data class WorkoutSession(
    val id: String,
    val userId: String,
    val name: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: SessionStatus,
)

data class WorkoutSet(
    val id: String,
    val number: Int,
    val reps: Int,
    val weightKg: BigDecimal,
)

data class WorkoutExercise(
    val exercise: Exercise,
    val position: Int,
    val sets: List<WorkoutSet>,
)

data class WorkoutDraft(
    val session: WorkoutSession,
    val exercises: List<WorkoutExercise>,
)

data class WorkoutTotals(
    val exerciseCount: Int,
    val setCount: Int,
    val repCount: Int,
    val volumeKg: BigDecimal,
)

data class PersonalRecord(
    val exerciseId: String,
    val previousMaxKg: BigDecimal?,
    val currentMaxKg: BigDecimal,
    val status: PersonalRecordStatus,
)

data class ExerciseReference(
    val session: WorkoutSession,
    val sets: List<WorkoutSet>,
    val volumeKg: BigDecimal,
)

data class SetComparison(
    val previousSet: WorkoutSet?,
    val currentSet: WorkoutSet?,
    val weightDeltaKg: BigDecimal?,
    val repsDelta: Int?,
)

data class AnalyticsSummary(
    val startEpochMs: Long,
    val endEpochMs: Long,
    val sessionCount: Int,
    val setCount: Int,
    val repCount: Int,
    val volumeKg: BigDecimal,
    val favoriteExercise: Exercise?,
    val favoriteSessionCount: Int,
    val dailyVolumeKg: List<Pair<Long, BigDecimal>>,
)
