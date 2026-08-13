package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FocusSessionEntity::class], version = 1, exportSchema = false)
abstract class ZenWedgeDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: ZenWedgeDatabase? = null

        fun getDatabase(context: Context): ZenWedgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenWedgeDatabase::class.java,
                    "zenwedge_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
