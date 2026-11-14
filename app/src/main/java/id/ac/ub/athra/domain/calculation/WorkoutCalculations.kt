package id.ac.ub.athra.domain.calculation

import id.ac.ub.athra.domain.model.AnalyticsSummary
import id.ac.ub.athra.domain.model.Exercise
import id.ac.ub.athra.domain.model.PersonalRecord
import id.ac.ub.athra.domain.model.PersonalRecordStatus
import id.ac.ub.athra.domain.model.WorkoutDraft
import id.ac.ub.athra.domain.model.WorkoutTotals
import id.ac.ub.athra.domain.model.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val LB_TO_KG = BigDecimal("0.45359237")
private val DECIMAL_INPUT = Regex("[0-9]+(?:[.,][0-9]+)?")

fun parseWeightKg(input: String, unit: WeightUnit): BigDecimal? {
    val value = input.trim()
    if (!DECIMAL_INPUT.matches(value)) return null
    val decimal = value.replace(',', '.').toBigDecimalOrNull() ?: return null
    return if (unit == WeightUnit.LB) decimal.multiply(LB_TO_KG) else decimal
}

fun parseReps(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it > 0 }

fun displayWeight(weightKg: BigDecimal, unit: WeightUnit): BigDecimal =
    if (unit == WeightUnit.KG) weightKg else weightKg.divide(LB_TO_KG, 8, RoundingMode.HALF_UP)

fun displayNumber(value: BigDecimal): String =
    value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

fun calculateTotals(draft: WorkoutDraft): WorkoutTotals {
    val validExercises = draft.exercises.filter { it.sets.isNotEmpty() }
    val sets = validExercises.flatMap { it.sets }
    return WorkoutTotals(
        exerciseCount = validExercises.size,
        setCount = sets.size,
        repCount = sets.sumOf { it.reps },
        volumeKg = sets.fold(BigDecimal.ZERO) { total, set ->
            total.add(set.weightKg.multiply(BigDecimal.valueOf(set.reps.toLong())))
        },
    )
}

fun personalRecord(exerciseId: String, currentWeightsKg: List<BigDecimal>, previousMaxKg: BigDecimal?): PersonalRecord? {
    val currentMax = currentWeightsKg.maxOrNull() ?: return null
    val status = when {
        previousMaxKg == null -> PersonalRecordStatus.BASELINE
        currentMax > previousMaxKg -> PersonalRecordStatus.NEW_RECORD
        currentMax.compareTo(previousMaxKg) == 0 -> PersonalRecordStatus.MATCHED
        else -> PersonalRecordStatus.BELOW_RECORD
    }
    return PersonalRecord(exerciseId, previousMaxKg, currentMax, status)
}

data class AnalyticsInputSession(
    val endedAt: Long,
    val exercises: List<AnalyticsInputExercise>,
)

data class AnalyticsInputExercise(
    val exercise: Exercise,
    val sets: List<Pair<Int, BigDecimal>>,
)

fun calculateAnalytics(
    sessions: List<AnalyticsInputSession>,
    nowEpochMs: Long,
    periodDays: Int,
    zone: ZoneId,
    calendarWeek: Boolean = false,
): AnalyticsSummary {
    require(periodDays in setOf(7, 30, 90))
    val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
    val firstDay = if (calendarWeek) {
        now.toLocalDate().minusDays((now.dayOfWeek.value - 1).toLong())
    } else now.toLocalDate().minusDays((periodDays - 1).toLong())
    val start = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
    val included = sessions.filter { it.endedAt in start..nowEpochMs }
    val dayCount = if (calendarWeek) ChronoUnit.DAYS.between(firstDay, now.toLocalDate()).toInt() + 1 else periodDays
    val daily = (0 until dayCount).associate { firstDay.plusDays(it.toLong()) to BigDecimal.ZERO }.toMutableMap()
    var setCount = 0
    var repCount = 0
    var volume = BigDecimal.ZERO
    val frequency = mutableMapOf<Exercise, Int>()
    included.forEach { session ->
        val day = Instant.ofEpochMilli(session.endedAt).atZone(zone).toLocalDate()
        session.exercises.forEach { exercise ->
            if (exercise.sets.isNotEmpty()) frequency[exercise.exercise] =
                frequency.getOrDefault(exercise.exercise, 0) + 1
            exercise.sets.forEach { (reps, weightKg) ->
                setCount += 1
                repCount += reps
                val setVolume = weightKg.multiply(BigDecimal.valueOf(reps.toLong()))
                volume = volume.add(setVolume)
                daily[day] = daily.getValue(day).add(setVolume)
            }
        }
    }
    val favorite = frequency.entries.sortedWith(
        compareByDescending<Map.Entry<Exercise, Int>> { it.value }
            .thenBy { it.key.name.lowercase() }
            .thenBy { it.key.id },
    ).firstOrNull()
    return AnalyticsSummary(
        startEpochMs = start,
        endEpochMs = nowEpochMs,
        sessionCount = included.size,
        setCount = setCount,
        repCount = repCount,
        volumeKg = volume,
        favoriteExercise = favorite?.key,
        favoriteSessionCount = favorite?.value ?: 0,
        dailyVolumeKg = daily.entries.map { it.key.atStartOfDay(zone).toInstant().toEpochMilli() to it.value },
    )
}

fun currentWeekStartEpochMs(nowEpochMs: Long, zone: ZoneId): Long {
    val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return monday.atStartOfDay(zone).toInstant().toEpochMilli()
}
