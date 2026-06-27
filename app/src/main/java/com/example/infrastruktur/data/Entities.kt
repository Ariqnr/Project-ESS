package com.example.infrastruktur.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Operator")
data class Operator(
    @PrimaryKey val idOperator: String,
    val nama: String,
    val jabatan: String,
    val shift: String
)

@Entity(tableName = "Supervisor")
data class Supervisor(
    @PrimaryKey val idSupervisor: String,
    val nama: String,
    val departemen: String
)

@Entity(tableName = "StaffWarehouse")
data class StaffWarehouse(
    @PrimaryKey val idStaff: String,
    val nama: String,
    val shift: String
)
