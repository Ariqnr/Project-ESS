package com.example.infrastruktur.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operations")
data class Operation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val timestamp: String,
    val type: String // e.g., "SUCCESS", "ERROR", "PENDING"
)
