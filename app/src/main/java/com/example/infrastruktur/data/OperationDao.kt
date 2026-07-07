package com.example.infrastruktur.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {
    @Query("SELECT * FROM operations ORDER BY id DESC")
    fun getAllOperations(): Flow<List<Operation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: Operation)

    @Query("DELETE FROM operations")
    suspend fun deleteAll()
}
