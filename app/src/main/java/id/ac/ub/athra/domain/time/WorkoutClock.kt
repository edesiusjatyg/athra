package id.ac.ub.athra.domain.time

fun interface WorkoutClock {
    fun nowMillis(): Long
}

object SystemWorkoutClock : WorkoutClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

fun elapsedSeconds(startedAt: Long, now: Long): Long = (now - startedAt).coerceAtLeast(0L) / 1_000L
