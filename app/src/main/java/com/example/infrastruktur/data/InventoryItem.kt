package com.example.infrastruktur.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey val sku: String,
    val name: String,
    var quantity: Int,
    val type: String // "RAW", "FINISHED"
)
