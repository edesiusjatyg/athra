package id.ac.ub.athra.domain

import id.ac.ub.athra.domain.calculation.*
import id.ac.ub.athra.domain.model.*
import id.ac.ub.athra.domain.time.elapsedSeconds
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

    @Test
    fun analyticsIncludesFirstDayAndExcludesImmediatelyBeforeIt() {
        val exercise = Exercise("row", "Row", "Back", "Cable", "")
        val zone = ZoneId.of("Asia/Jakarta")
        val now = java.time.LocalDate.of(2026, 11, 14).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val firstDay = java.time.LocalDate.of(2026, 11, 8).atStartOfDay(zone).toInstant().toEpochMilli()
        val inputs = listOf(
            AnalyticsInputSession(firstDay - 1, listOf(AnalyticsInputExercise(exercise, listOf(10 to BigDecimal("10"))))),
            AnalyticsInputSession(firstDay, listOf(AnalyticsInputExercise(exercise, listOf(10 to BigDecimal("10"))))),
        )
        val result = calculateAnalytics(inputs, now, 7, zone)
        assertEquals(1, result.sessionCount)
        assertEquals(BigDecimal("100"), result.volumeKg)
    }

    @Test
    fun favoriteFrequencyCountsSessionsAndBreaksTiesAlphabetically() {
        val alpha = Exercise("a", "Alpha", "Back", "Cable", "")
        val beta = Exercise("b", "beta", "Chest", "Barbell", "")
        val zone = ZoneId.of("UTC")
        val now = java.time.LocalDate.of(2026, 11, 14).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            AnalyticsInputSession(now - 2_000, listOf(AnalyticsInputExercise(beta, listOf(10 to BigDecimal.ONE, 10 to BigDecimal.ONE)), AnalyticsInputExercise(alpha, listOf(1 to BigDecimal.ONE)))),
            AnalyticsInputSession(now - 1_000, listOf(AnalyticsInputExercise(beta, listOf(10 to BigDecimal.ONE)), AnalyticsInputExercise(alpha, listOf(1 to BigDecimal.ONE)))),
        )
        val result = calculateAnalytics(sessions, now, 7, zone)
        assertEquals("Alpha", result.favoriteExercise?.name)
        assertEquals(2, result.favoriteSessionCount)
    }

    @Test
    fun zeroLoadSessionIsNotAnEmptyPeriod() {
        val exercise = Exercise("plank", "Plank", "Core", "Bodyweight", "")
        val zone = ZoneId.of("UTC")
        val now = java.time.LocalDate.of(2026, 11, 14).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val result = calculateAnalytics(
            listOf(AnalyticsInputSession(now, listOf(AnalyticsInputExercise(exercise, listOf(30 to BigDecimal.ZERO))))),
            now,
            30,
            zone,
        )
        assertEquals(1, result.sessionCount)
        assertEquals(1, result.setCount)
        assertEquals(30, result.repCount)
        assertEquals(BigDecimal.ZERO, result.volumeKg)
        assertEquals(BigDecimal.ZERO, result.dailyVolumeKg.last().second)
    }

    @Test
    fun displayAndParsePoundsPreserveCanonicalValueForExactConversion() {
        val canonicalKg = BigDecimal("4.53592370")
        val displayedLb = displayWeight(canonicalKg, WeightUnit.LB)
        assertEquals("10", displayedLb.stripTrailingZeros().toPlainString())
        assertEquals(0, canonicalKg.compareTo(parseWeightKg(displayedLb.toPlainString(), WeightUnit.LB)))
    }

    @Test
    fun homeAnalyticsUsesMondayToTodayCalendarWeek() {
        val exercise = Exercise("press", "Press", "Chest", "Dumbbell", "")
        val zone = ZoneId.of("Asia/Jakarta")
        val now = java.time.LocalDate.of(2026, 11, 14).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val monday = java.time.LocalDate.of(2026, 11, 9).atStartOfDay(zone).toInstant().toEpochMilli()
        val sunday = java.time.LocalDate.of(2026, 11, 8).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            AnalyticsInputSession(monday, listOf(AnalyticsInputExercise(exercise, listOf(10 to BigDecimal("20"))))),
            AnalyticsInputSession(sunday, listOf(AnalyticsInputExercise(exercise, listOf(10 to BigDecimal("20"))))),
        )
        val result = calculateAnalytics(sessions, now, 7, zone, calendarWeek = true)
        assertEquals(monday, result.startEpochMs)
        assertEquals(1, result.sessionCount)
        assertEquals(6, result.dailyVolumeKg.size)
    }

    @Test
    fun analyticsPeriodBoundariesWorkForThirtyAndNinetyDays() {
        val exercise = Exercise("row", "Row", "Back", "Cable", "")
        val zone = ZoneId.of("Asia/Jakarta")
        val now = java.time.LocalDate.of(2026, 11, 14).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        listOf(30, 90).forEach { days ->
            val firstDay = java.time.LocalDate.of(2026, 11, 14).minusDays((days - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
            val sessions = listOf(
                AnalyticsInputSession(firstDay - 1, listOf(AnalyticsInputExercise(exercise, listOf(1 to BigDecimal.ONE)))),
                AnalyticsInputSession(firstDay, listOf(AnalyticsInputExercise(exercise, listOf(1 to BigDecimal.ONE)))),
            )
            val result = calculateAnalytics(sessions, now, days, zone)
            assertEquals(1, result.sessionCount)
            assertEquals(days, result.dailyVolumeKg.size)
        }
    }

    @Test
    fun setComparisonMatchesByNumberAndLeavesMissingPairsWithoutZeroHistory() {
        val previous = listOf(
            WorkoutSet("p1", 1, 8, BigDecimal("25")),
            WorkoutSet("p2", 2, 8, BigDecimal("30")),
        )
        val current = listOf(
            WorkoutSet("c1", 1, 10, BigDecimal("27.5")),
            WorkoutSet("c3", 3, 6, BigDecimal.ZERO),
        )
        val result = compareSets(current, previous)
        assertEquals(BigDecimal("2.5"), result[0].weightDeltaKg)
        assertEquals(2, result[0].repsDelta)
        assertNull(result[1].previousSet)
        assertNull(result[1].weightDeltaKg)
        assertNull(result[1].repsDelta)
    }

    @Test
    fun displayedNumbersUseHalfUpRoundingAndStripTrailingZeroes() {
        assertEquals("1.01", displayNumber(BigDecimal("1.005")))
        assertEquals("2", displayNumber(BigDecimal("2.000")))
    }

    @Test
    fun elapsedTimerUsesInjectedTimestampAndNeverGoesNegative() {
        assertEquals(3_661L, elapsedSeconds(10_000L, 3_671_999L))
        assertEquals(0L, elapsedSeconds(5_000L, 4_999L))
    }
}
