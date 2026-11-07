package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PhysicalSymptomLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PhysicalSymptomDao {
    @Query("SELECT * FROM physical_symptom_logs ORDER BY dateStr DESC, id DESC")
    fun getAllSymptoms(): Flow<List<PhysicalSymptomLog>>

    @Query("SELECT * FROM physical_symptom_logs ORDER BY dateStr DESC, id DESC")
    suspend fun getAllSymptomsList(): List<PhysicalSymptomLog>

    @Query("SELECT * FROM physical_symptom_logs WHERE dateStr = :dateStr ORDER BY id ASC")
    fun getSymptomsForDate(dateStr: String): Flow<List<PhysicalSymptomLog>>

    @Query("SELECT * FROM physical_symptom_logs WHERE dateStr = :dateStr ORDER BY id ASC")
    suspend fun getSymptomsListForDate(dateStr: String): List<PhysicalSymptomLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: PhysicalSymptomLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptoms: List<PhysicalSymptomLog>)

    @Query("DELETE FROM physical_symptom_logs WHERE dateStr = :dateStr")
    suspend fun deleteSymptomsForDate(dateStr: String)

    @Query("DELETE FROM physical_symptom_logs WHERE id = :id")
    suspend fun deleteSymptomById(id: Long)

    @Delete
    suspend fun deleteSymptom(symptom: PhysicalSymptomLog)

    @Query("SELECT COUNT(*) FROM physical_symptom_logs WHERE dateStr = :dateStr")
    suspend fun getSymptomCountForDate(dateStr: String): Int
}
