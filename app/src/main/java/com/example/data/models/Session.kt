package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val location: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val fileUri: String = ""
)
