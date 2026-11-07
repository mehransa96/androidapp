package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DailyLogDao
import com.example.data.model.DailyLog
import com.example.data.dao.CycleRecordDao
import com.example.data.model.CycleRecord
import com.example.data.dao.PhysicalSymptomDao
import com.example.data.model.PhysicalSymptomLog
import com.example.data.dao.HealthEntryDao
import com.example.data.model.HealthEntry

@Database(entities = [DailyLog::class, CycleRecord::class, PhysicalSymptomLog::class, HealthEntry::class], version = 9, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao
    abstract fun cycleRecordDao(): CycleRecordDao
    abstract fun physicalSymptomDao(): PhysicalSymptomDao
    abstract fun healthEntryDao(): HealthEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN waterIntake INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN takenMeds TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `physical_symptom_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateStr` TEXT NOT NULL,
                        `symptomName` TEXT NOT NULL,
                        `severity` TEXT NOT NULL,
                        `severityScore` INTEGER NOT NULL DEFAULT 1,
                        `category` TEXT NOT NULL DEFAULT 'عمومی',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN bbt REAL")
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN weight REAL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Health tools (intimacy, tests, blood pressure, pill, breast exam) live in their own table. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `health_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateStr` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `num1` INTEGER,
                        `num2` INTEGER,
                        `notes` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Closes the open database so its file can be deleted safely (full reset). */
        fun closeAndForget() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hanita_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                // Never wipe user health data on upgrade. Only the pre-release schema 1 may be rebuilt.
                .fallbackToDestructiveMigrationFrom(true, 1)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
