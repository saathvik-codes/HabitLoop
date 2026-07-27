package com.habitloop.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_tasks")
data class PlannerTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val note: String = "",
    val dueAtEpochMillis: Long = 0,
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
