package id.ac.ub.athra.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import java.math.BigDecimal

class DecimalConverters {
    @TypeConverter
    fun decimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)
}

@Database(
    entities = [
        LocalProfileEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        ActiveWorkoutEntity::class,
        SessionExerciseEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DecimalConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        /** Installs SQLite-level guards, including when opening an existing v1 database. */
        val validationCallback: Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = installWorkoutSetTriggers(db)
            override fun onOpen(db: SupportSQLiteDatabase) = installWorkoutSetTriggers(db)
        }

        private fun installWorkoutSetTriggers(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TRIGGER IF NOT EXISTS workout_sets_validate_insert
                    BEFORE INSERT ON workout_sets
                    FOR EACH ROW WHEN NEW.setNumber <= 0 OR NEW.reps <= 0 OR NEW.weightKg IS NULL OR TRIM(NEW.weightKg) GLOB '-*'
                    BEGIN SELECT RAISE(ABORT, 'invalid workout set'); END""".trimIndent(),
            )
            db.execSQL(
                """CREATE TRIGGER IF NOT EXISTS workout_sets_validate_update
                    BEFORE UPDATE ON workout_sets
                    FOR EACH ROW WHEN NEW.setNumber <= 0 OR NEW.reps <= 0 OR NEW.weightKg IS NULL OR TRIM(NEW.weightKg) GLOB '-*'
                    BEGIN SELECT RAISE(ABORT, 'invalid workout set'); END""".trimIndent(),
            )
        }
    }
}
