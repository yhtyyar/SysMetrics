package com.sysmetrics.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sysmetrics.app.data.local.dao.MetricsHistoryDao
import com.sysmetrics.app.data.local.entity.MetricsHistoryEntity

/**
 * Room database for storing metrics history.
 * Stores up to 24 hours of metrics data.
 */
@Database(
    entities = [MetricsHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MetricsDatabase : RoomDatabase() {

    abstract fun metricsHistoryDao(): MetricsHistoryDao

    companion object {
        private const val DATABASE_NAME = "sysmetrics_db"

        @Volatile
        private var INSTANCE: MetricsDatabase? = null

        fun getInstance(context: Context): MetricsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MetricsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MetricsDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
