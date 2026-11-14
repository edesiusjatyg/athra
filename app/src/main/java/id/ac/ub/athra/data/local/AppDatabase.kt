package id.ac.ub.athra.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
}
