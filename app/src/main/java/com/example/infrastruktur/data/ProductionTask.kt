package com.example.infrastruktur.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "production_tasks")
data class ProductionTask(
    @PrimaryKey val serialNumber: String,
    val productName: String,
    var status: String, // "ASSEMBLY", "QC_PENDING", "QC_PASSED", "QC_REJECTED", "REPAIRING", "READY_FOR_RETEST", "PACKED", "SHIPPED"
    val timestamp: Long = System.currentTimeMillis()
)
