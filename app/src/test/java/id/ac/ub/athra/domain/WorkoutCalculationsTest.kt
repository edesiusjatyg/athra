package id.ac.ub.athra.domain

import id.ac.ub.athra.domain.calculation.*
import id.ac.ub.athra.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.ZoneId

class WorkoutCalculationsTest {
    @Test
    fun parsesWeightsAndRejectsInvalidInput() {
        assertEquals(BigDecimal("20.5"), parseWeightKg("20,5", WeightUnit.KG))
        assertEquals(BigDecimal("4.53592370"), parseWeightKg("10", WeightUnit.LB))
        assertNull(parseWeightKg("1,000.2", WeightUnit.KG))
        assertNull(parseWeightKg("-1", WeightUnit.KG))
        assertNull(parseWeightKg("Infinity", WeightUnit.KG))
        assertNull(parseReps("0"))
        assertNull(parseReps("2.5"))
    }

    @Test
    fun totalsRecalculateFromSetsIncludingZeroLoad() {
        val exercise = Exercise("pull_up", "Pull Up", "Back", "Bodyweight", "")
        val draft = WorkoutDraft(
            WorkoutSession("s", "u", "Workout", 1L, null, SessionStatus.ACTIVE),
            listOf(WorkoutExercise(exercise, 0, listOf(
                WorkoutSet("1", 1, 12, BigDecimal.ZERO),
                WorkoutSet("2", 2, 10, BigDecimal("5")),
            ))),
        )
        val totals = calculateTotals(draft)
        assertEquals(1, totals.exerciseCount)
        assertEquals(2, totals.setCount)
        assertEquals(22, totals.repCount)
        assertEquals(BigDecimal("50"), totals.volumeKg)
    }

    @Test
    fun personalRecordUsesWeightMaximumAndFourStatuses() {
        assertEquals(PersonalRecordStatus.BASELINE, personalRecord("x", listOf(BigDecimal.ZERO), null)?.status)
        assertEquals(PersonalRecordStatus.NEW_RECORD, personalRecord("x", listOf(BigDecimal("32")), BigDecimal("30"))?.status)
        assertEquals(PersonalRecordStatus.MATCHED, personalRecord("x", listOf(BigDecimal("30.0")), BigDecimal("30"))?.status)
        assertEquals(PersonalRecordStatus.BELOW_RECORD, personalRecord("x", listOf(BigDecimal("25")), BigDecimal("30"))?.status)
    }

    @Test
    fun analyticsMatchesPrdFixtureAndUsesLatestDayBuckets() {
        val bench = Exercise("bench_press", "Bench Press", "Chest", "Barbell", "")
        val squat = Exercise("squat", "Squat", "Legs", "Barbell", "")
        val zone = ZoneId.of("Asia/Jakarta")
        fun time(day: Int, hour: Int) = java.time.LocalDate.of(2026, 11, day)
            .atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            AnalyticsInputSession(time(10, 18), listOf(AnalyticsInputExercise(bench, listOf(10 to BigDecimal("30"), 8 to BigDecimal("30"))))),
            AnalyticsInputSession(time(12, 18), listOf(AnalyticsInputExercise(bench, listOf(10 to BigDecimal("25"))), AnalyticsInputExercise(squat, listOf(10 to BigDecimal("40"))))),
            AnalyticsInputSession(time(14, 11), listOf(AnalyticsInputExercise(bench, listOf(10 to BigDecimal("32"), 10 to BigDecimal("30"))), AnalyticsInputExercise(squat, listOf(10 to BigDecimal("40"))))),
        )
        val result = calculateAnalytics(sessions, time(14, 12), 7, zone)
        assertEquals(3, result.sessionCount)
        assertEquals(7, result.setCount)
        assertEquals(68, result.repCount)
        assertEquals(BigDecimal("2210"), result.volumeKg)
        assertEquals("Bench Press", result.favoriteExercise?.name)
        assertEquals(3, result.favoriteSessionCount)
        assertEquals(7, result.dailyVolumeKg.size)
        assertTrue(result.dailyVolumeKg.all { it.second >= BigDecimal.ZERO })
    }
}
