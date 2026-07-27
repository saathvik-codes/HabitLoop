package com.habitloop.app.ui

import androidx.lifecycle.ViewModel
import android.content.Context
import com.habitloop.app.worker.ReminderScheduler
import androidx.lifecycle.viewModelScope
import com.habitloop.app.data.HabitCompletion
import com.habitloop.app.data.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(private val repository: HabitRepository, private val appContext: Context) : ViewModel() {

    val habits = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plannerTasks = repository.observePlannerTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(
        name: String,
        templateId: String,
        scheduleDaysCsv: String = "1,2,3,4,5,6,7",
        motivation: String = "",
        reminderHour: Int = 19,
        reminderMinute: Int = 0
    ) {
        viewModelScope.launch {
            val id = repository.createHabit(name, templateId, scheduleDaysCsv, motivation, reminderHour, reminderMinute)
            ReminderScheduler.scheduleHabitReminder(appContext, id, reminderHour, reminderMinute)
        }
    }

    fun completeToday(habitId: Long) {
        viewModelScope.launch {
            repository.completeToday(habitId)
        }
    }

    fun grantFreezeToken(habitId: Long) {
        viewModelScope.launch {
            repository.grantFreezeToken(habitId)
        }
    }

    fun updateSchedule(habitId: Long, scheduleDaysCsv: String) {
        viewModelScope.launch { repository.updateSchedule(habitId, scheduleDaysCsv) }
    }

    fun pauseHabit(habitId: Long, untilEpochDay: Long?) {
        viewModelScope.launch { repository.pauseHabit(habitId, untilEpochDay) }
    }

    fun setArchived(habitId: Long, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(habitId, archived) }
    }

    fun updateHabitReminder(habitId: Long, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateHabitReminder(habitId, hour, minute)
            ReminderScheduler.scheduleHabitReminder(appContext, habitId, hour, minute)
        }
    }

    fun addPlannerTask(title: String, note: String, dueAtEpochMillis: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createPlannerTask(title, note, dueAtEpochMillis))
        }
    }

    fun setPlannerTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch { repository.setPlannerTaskCompleted(taskId, completed) }
    }

    fun deletePlannerTask(taskId: Long) {
        viewModelScope.launch { repository.deletePlannerTask(taskId) }
    }

    fun updatePlannerTask(task: com.habitloop.app.data.PlannerTask) {
        viewModelScope.launch { repository.updatePlannerTask(task) }
    }

    fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>> =
        repository.observeCompletions(habitId)

    suspend fun completionsInRange(startEpochDay: Long, endEpochDay: Long): List<HabitCompletion> =
        repository.completionsInRange(startEpochDay, endEpochDay)

    suspend fun allCompletions(): List<HabitCompletion> = repository.allCompletions()
}
