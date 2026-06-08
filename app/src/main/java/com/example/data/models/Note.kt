package com.example.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val deltaMs: Long,
    val content: String,
    val isAutomated: Boolean = false
)
