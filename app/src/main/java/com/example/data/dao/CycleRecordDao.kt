package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CycleRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleRecordDao {
    @Query("SELECT * FROM cycle_records")
    fun getAllCycleRecords(): Flow<List<CycleRecord>>

    @Query("SELECT * FROM cycle_records")
    suspend fun getAllCycleRecordsList(): List<CycleRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleRecords(records: List<CycleRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycleRecord(cycleRecord: CycleRecord)

    @Query("SELECT * FROM cycle_records WHERE id = :id LIMIT 1")
    suspend fun getCycleRecordById(id: String): CycleRecord?

    @Query("DELETE FROM cycle_records WHERE id = :id")
    suspend fun deleteCycleRecord(id: String)
}
