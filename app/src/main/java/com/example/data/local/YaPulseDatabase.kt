package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackHistoryEntity::class, TrackBookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class YaPulseDatabase : RoomDatabase() {
    abstract fun trackHistoryDao(): TrackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: YaPulseDatabase? = null

        fun getInstance(context: Context): YaPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YaPulseDatabase::class.java,
                    "yapulse_room.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
