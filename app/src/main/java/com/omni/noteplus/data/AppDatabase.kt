package com.omni.noteplus.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NoteEntity::class, ProductivityNote::class, ProductivityActivity::class, ProductivityTracking::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun productivityDao(): ProductivityDao
}

