package com.example.infrastruktur.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionTaskDao {
    @Query("SELECT * FROM production_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<ProductionTask>>

    @Query("SELECT * FROM production_tasks WHERE serialNumber = :sn LIMIT 1")
    suspend fun getTaskBySn(sn: String): ProductionTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProductionTask)

    @Update
    suspend fun updateTask(task: ProductionTask)
    
    @Query("SELECT * FROM production_tasks WHERE status = 'QC_PASSED'")
    fun getReadyToPack(): Flow<List<ProductionTask>>

    @Query("SELECT * FROM production_tasks WHERE status = 'FINISHED'")
    fun getReadyToShip(): Flow<List<ProductionTask>>
}
