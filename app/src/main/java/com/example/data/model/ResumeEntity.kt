package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parsed_resumes")
data class ResumeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val rawJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
