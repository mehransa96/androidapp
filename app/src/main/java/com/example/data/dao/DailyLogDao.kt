package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY dateStr DESC")
    fun getAllLogs(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE dateStr = :date LIMIT 1")
    suspend fun getLogByDate(date: String): DailyLog?
    
    @Query("SELECT * FROM daily_logs WHERE dateStr = :date LIMIT 1")
    fun getLogByDateFlow(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE periodFlow != 'NONE' ORDER BY dateStr DESC")
    fun getPeriodDays(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE periodFlow != 'NONE' ORDER BY dateStr DESC")
    suspend fun getPeriodDaysList(): List<DailyLog>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllLogsList(): List<DailyLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<DailyLog>)

    @Query("DELETE FROM daily_logs WHERE dateStr = :date")
    suspend fun deleteLogByDate(date: String)
}
