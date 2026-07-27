package com.habitloop.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAtEpochDay ASC")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabit(habitId: Long): Habit?

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY epochDay DESC")
    fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun hasCompletionOn(habitId: Long, epochDay: Long): Int

    @Query("SELECT * FROM habit_completions WHERE epochDay >= :startDay AND epochDay <= :endDay")
    suspend fun getCompletionsInRange(startDay: Long, endDay: Long): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletions(): List<HabitCompletion>

    @Query("SELECT * FROM planner_tasks ORDER BY isCompleted ASC, dueAtEpochMillis ASC")
    fun observePlannerTasks(): Flow<List<PlannerTask>>

    @Query("SELECT * FROM planner_tasks WHERE id = :taskId")
    suspend fun getPlannerTask(taskId: Long): PlannerTask?

    @Insert
    suspend fun insertPlannerTask(task: PlannerTask): Long

    @Update
    suspend fun updatePlannerTask(task: PlannerTask)

    @Query("DELETE FROM planner_tasks WHERE id = :taskId")
    suspend fun deletePlannerTask(taskId: Long)
}
