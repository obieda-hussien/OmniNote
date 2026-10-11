package com.omni.noteplus.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import android.content.Context

@Database(entities = [NoteEntity::class, ProductivityNote::class, ProductivityActivity::class, ProductivityTracking::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun productivityDao(): ProductivityDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "omninote_database"
            ).addMigrations(*DatabaseMigrations.all).build().also { instance = it }
        }
    }
}

