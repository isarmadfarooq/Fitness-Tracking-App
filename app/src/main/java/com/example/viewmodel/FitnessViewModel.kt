package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FitnessViewModel(application: Application, private val repository: FitnessRepository) : AndroidViewModel(application) {

    // Target goals
    val stepGoal = 10000
    val waterGoalMl = 2500
    val calorieGoalKcal = 500

    // Base flows
    val workoutsFlow: Flow<List<WorkoutLog>> = repository.allWorkouts
    val waterLogsFlow: Flow<List<WaterLog>> = repository.allWaterLogs
    val stepLogsFlow: Flow<List<StepLog>> = repository.allStepLogs
    val weightLogsFlow: Flow<List<WeightLog>> = repository.allWeightLogs
    val strengthSetsFlow: Flow<List<StrengthExerciseSetLog>> = repository.allStrengthSets
    val nutritionLogsFlow: Flow<List<NutritionLog>> = repository.allNutritionLogs

    // Helper to evaluate if timestamp belongs to today
    private fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        val logCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return todayYear == logCalendar.get(Calendar.YEAR) && todayDay == logCalendar.get(Calendar.DAY_OF_YEAR)
    }

    // StateFlows that transform the raw room data reactively for today
    val todayWorkouts: Flow<List<WorkoutLog>> = workoutsFlow.map { list ->
        list.filter { isToday(it.timestamp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySteps: Flow<Int> = stepLogsFlow.map { list ->
        list.filter { isToday(it.timestamp) }.sumOf { it.steps }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayWaterMl: Flow<Int> = waterLogsFlow.map { list ->
        list.filter { isToday(it.timestamp) }.sumOf { it.amountMl }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayStrengthSets: Flow<List<StrengthExerciseSetLog>> = strengthSetsFlow.map { list ->
        list.filter { isToday(it.timestamp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCaloriesBurned: Flow<Int> = combine(todayWorkouts, todayStrengthSets) { standardWorkouts, strengthSets ->
        // Standard workout calories burned + aggregated strength sets calories burned (strength sets grouped by workoutId)
        val standardSum = standardWorkouts.sumOf { it.caloriesBurned }
        // For strength sets, group by workoutId and sum their caloriesBurned limit at 1 per workoutId
        val strengthDistinctCal = strengthSets.groupBy { it.workoutId }
            .map { entry -> entry.value.firstOrNull()?.caloriesBurned ?: 0 }
            .sum()
        standardSum + strengthDistinctCal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentWeight: Flow<Double?> = weightLogsFlow.map { list ->
        list.firstOrNull()?.weightKg
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Today's nutrition flows
    val todayNutritionLogs: Flow<List<NutritionLog>> = nutritionLogsFlow.map { list ->
        list.filter { isToday(it.timestamp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCaloriesConsumed: Flow<Int> = todayNutritionLogs.map { list ->
        list.sumOf { it.calories }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayProteinGrams: Flow<Double> = todayNutritionLogs.map { list ->
        list.sumOf { it.proteinGrams }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayCarbsGrams: Flow<Double> = todayNutritionLogs.map { list ->
        list.sumOf { it.carbsGrams }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayFatGrams: Flow<Double> = todayNutritionLogs.map { list ->
        list.sumOf { it.fatGrams }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // User Operations
    fun logWorkout(type: String, duration: Int, calories: Int) {
        viewModelScope.launch {
            repository.insertWorkout(
                WorkoutLog(
                    exerciseType = type,
                    durationMinutes = duration,
                    caloriesBurned = calories
                )
            )
        }
    }

    fun deleteWorkout(workout: WorkoutLog) {
        viewModelScope.launch {
            repository.deleteWorkout(workout)
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.insertWaterLog(
                WaterLog(amountMl = amountMl)
            )
        }
    }

    fun logSteps(steps: Int) {
        viewModelScope.launch {
            repository.insertStepLog(
                StepLog(steps = steps)
            )
        }
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            repository.insertWeightLog(
                WeightLog(weightKg = weightKg)
            )
        }
    }

    fun logStrengthSet(workoutId: String, workoutName: String, exerciseName: String, setNumber: Int, reps: Int, weightKg: Double, durationMinutes: Int, caloriesBurned: Int) {
        viewModelScope.launch {
            repository.insertStrengthSet(
                StrengthExerciseSetLog(
                    workoutId = workoutId,
                    workoutName = workoutName,
                    exerciseName = exerciseName,
                    setNumber = setNumber,
                    reps = reps,
                    weightKg = weightKg,
                    durationMinutes = durationMinutes,
                    caloriesBurned = caloriesBurned
                )
            )
        }
    }

    fun deleteStrengthSet(set: StrengthExerciseSetLog) {
        viewModelScope.launch {
            repository.deleteStrengthSet(set)
        }
    }

    fun logNutrition(foodName: String, calories: Int, protein: Double, carbs: Double, fat: Double, mealType: String) {
        viewModelScope.launch {
            repository.insertNutritionLog(
                NutritionLog(
                    foodName = foodName,
                    calories = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    mealType = mealType
                )
            )
        }
    }

    fun deleteNutritionLog(log: NutritionLog) {
        viewModelScope.launch {
            repository.deleteNutritionLog(log)
        }
    }

    // Factory pattern to instantiate in the compose layer
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FitnessViewModel::class.java)) {
                val db = FitnessDatabase.getDatabase(application)
                val repository = FitnessRepository(db.fitnessDao())
                @Suppress("UNCHECKED_CAST")
                return FitnessViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
