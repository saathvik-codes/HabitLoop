package com.habitloop.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habitloop.app.data.HabitCompletion
import com.habitloop.app.data.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {

    val habits = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(
        name: String,
        templateId: String,
        scheduleDaysCsv: String = "1,2,3,4,5,6,7",
        motivation: String = ""
    ) {
        viewModelScope.launch {
            repository.createHabit(name, templateId, scheduleDaysCsv, motivation)
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

    fun observeCompletions(habitId: Long): Flow<List<HabitCompletion>> =
        repository.observeCompletions(habitId)

    suspend fun completionsInRange(startEpochDay: Long, endEpochDay: Long): List<HabitCompletion> =
        repository.completionsInRange(startEpochDay, endEpochDay)

    suspend fun allCompletions(): List<HabitCompletion> = repository.allCompletions()
}
