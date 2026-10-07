package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubtitleProjectEntity::class,
        CaptionSegmentEntity::class,
        StylePresetEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SubCutDatabase : RoomDatabase() {
    abstract fun subCutDao(): SubCutDao

    companion object {
        @Volatile
        private var INSTANCE: SubCutDatabase? = null

        fun getInstance(context: Context): SubCutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubCutDatabase::class.java,
                    "subcut_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
