package id.ac.ub.athra.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.ub.athra.data.local.AppDatabase
import id.ac.ub.athra.data.local.ExerciseEntity
import id.ac.ub.athra.data.local.SessionExerciseEntity
import id.ac.ub.athra.data.local.WorkoutSessionEntity
import id.ac.ub.athra.data.local.WorkoutSetEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class WorkoutSetConstraintsTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() = runBlocking {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.validationCallback)
            .allowMainThreadQueries()
            .build()
        database.openHelper.writableDatabase
        database.exerciseDao().insertAll(listOf(ExerciseEntity("bench", "Bench", "Chest", "Barbell", "")))
        database.workoutDao().insertSession(WorkoutSessionEntity("session", "uid", "Workout", 1, null, "ACTIVE", 1, 1))
        database.workoutDao().insertSessionExercise(SessionExerciseEntity("session-exercise", "session", "bench", 0))
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun databaseRejectsNonPositiveRepsAndSetNumberAndNegativeWeight() = runBlocking {
        assertRejected(WorkoutSetEntity("bad-reps", "session-exercise", 1, 0, BigDecimal.ZERO))
        assertRejected(WorkoutSetEntity("bad-number", "session-exercise", 0, 1, BigDecimal.ZERO))
        assertRejected(WorkoutSetEntity("bad-weight", "session-exercise", 1, 1, BigDecimal("-0.01")))

        database.workoutDao().upsertSet(WorkoutSetEntity("valid-zero", "session-exercise", 1, 12, BigDecimal.ZERO))
    }

    private suspend fun assertRejected(set: WorkoutSetEntity) {
        try {
            database.workoutDao().upsertSet(set)
            fail("SQLite accepted invalid workout set $set")
        } catch (_: SQLiteConstraintException) {
            // The trigger rejects invalid rows before Room persists them.
        }
    }
}
