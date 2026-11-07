package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HealthEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthEntryDao {
    @Query("SELECT * FROM health_entries ORDER BY dateStr DESC, timestamp DESC")
    fun getAll(): Flow<List<HealthEntry>>

    @Query("SELECT * FROM health_entries ORDER BY dateStr DESC, timestamp DESC")
    suspend fun getAllList(): List<HealthEntry>

    @Query("SELECT * FROM health_entries WHERE type = :type ORDER BY dateStr DESC, timestamp DESC")
    suspend fun getByType(type: String): List<HealthEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HealthEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HealthEntry>)

    @Query("DELETE FROM health_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM health_entries WHERE dateStr = :dateStr AND type = :type")
    suspend fun deleteForDay(dateStr: String, type: String)
}
