package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseType: String, // Running, Cycling, Strength, Cardio, Yoga, Walking, etc.
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "step_logs")
data class StepLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val steps: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "strength_exercise_sets")
data class StrengthExerciseSetLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workoutId: String, // Groups sets belonging to same workout session
    val workoutName: String, // "Leg Day", "Push A", etc.
    val exerciseName: String, // "Squat", "Bench Press", etc.
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double,
    val durationMinutes: Int, // Duration of the whole workout
    val caloriesBurned: Int,  // Calories of the whole workout
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val timestamp: Long = System.currentTimeMillis()
)

// 2. Data Access Object (DAO)
@Dao
interface FitnessDao {
    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutLog)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutLog)

    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllWaterLogs(): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog)

    @Delete
    suspend fun deleteWaterLog(waterLog: WaterLog)

    @Query("SELECT * FROM step_logs ORDER BY timestamp DESC")
    fun getAllStepLogs(): Flow<List<StepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepLog(stepLog: StepLog)

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(weightLog: WeightLog)

    @Query("SELECT * FROM strength_exercise_sets ORDER BY timestamp DESC")
    fun getAllStrengthSets(): Flow<List<StrengthExerciseSetLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrengthSet(set: StrengthExerciseSetLog)

    @Delete
    suspend fun deleteStrengthSet(set: StrengthExerciseSetLog)

    @Query("SELECT * FROM nutrition_logs ORDER BY timestamp DESC")
    fun getAllNutritionLogs(): Flow<List<NutritionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionLog(log: NutritionLog)

    @Delete
    suspend fun deleteNutritionLog(log: NutritionLog)
}

// 3. Database Abstract Class
@Database(
    entities = [
        WorkoutLog::class, 
        WaterLog::class, 
        StepLog::class, 
        WeightLog::class, 
        StrengthExerciseSetLog::class, 
        NutritionLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var INSTANCE: FitnessDatabase? = null

        fun getDatabase(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// 4. Repository Pattern
class FitnessRepository(private val fitnessDao: FitnessDao) {
    val allWorkouts: Flow<List<WorkoutLog>> = fitnessDao.getAllWorkouts()
    val allWaterLogs: Flow<List<WaterLog>> = fitnessDao.getAllWaterLogs()
    val allStepLogs: Flow<List<StepLog>> = fitnessDao.getAllStepLogs()
    val allWeightLogs: Flow<List<WeightLog>> = fitnessDao.getAllWeightLogs()
    val allStrengthSets: Flow<List<StrengthExerciseSetLog>> = fitnessDao.getAllStrengthSets()
    val allNutritionLogs: Flow<List<NutritionLog>> = fitnessDao.getAllNutritionLogs()

    suspend fun insertWorkout(workout: WorkoutLog) {
        fitnessDao.insertWorkout(workout)
    }

    suspend fun deleteWorkout(workout: WorkoutLog) {
        fitnessDao.deleteWorkout(workout)
    }

    suspend fun insertWaterLog(waterLog: WaterLog) {
        fitnessDao.insertWaterLog(waterLog)
    }

    suspend fun deleteWaterLog(waterLog: WaterLog) {
        fitnessDao.deleteWaterLog(waterLog)
    }

    suspend fun insertStepLog(stepLog: StepLog) {
        fitnessDao.insertStepLog(stepLog)
    }

    suspend fun insertWeightLog(weightLog: WeightLog) {
        fitnessDao.insertWeightLog(weightLog)
    }

    suspend fun insertStrengthSet(set: StrengthExerciseSetLog) {
        fitnessDao.insertStrengthSet(set)
    }

    suspend fun deleteStrengthSet(set: StrengthExerciseSetLog) {
        fitnessDao.deleteStrengthSet(set)
    }

    suspend fun insertNutritionLog(log: NutritionLog) {
        fitnessDao.insertNutritionLog(log)
    }

    suspend fun deleteNutritionLog(log: NutritionLog) {
        fitnessDao.deleteNutritionLog(log)
    }
}
