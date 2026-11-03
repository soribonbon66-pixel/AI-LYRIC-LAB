package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongProjectEntity::class,
        SongVersionEntity::class,
        PersonalRhymeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LyricDatabase : RoomDatabase() {
    abstract fun lyricDao(): LyricDao

    companion object {
        @Volatile
        private var INSTANCE: LyricDatabase? = null

        fun getInstance(context: Context): LyricDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LyricDatabase::class.java,
                    "lyric_lab_ai.db"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
