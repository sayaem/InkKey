package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.FontAssetDao
import com.example.data.dao.GlyphDao
import com.example.data.dao.ProfileDao
import com.example.data.entity.FontAssetEntity
import com.example.data.entity.GlyphEntity
import com.example.data.entity.HandwritingProfileEntity

@Database(
    entities = [
        HandwritingProfileEntity::class,
        GlyphEntity::class,
        FontAssetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun glyphDao(): GlyphDao
    abstract fun fontAssetDao(): FontAssetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inkkey_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
