package com.omninote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val colorHex: String? = null, // For background color customization
    val tags: String = "", // Comma-separated tags
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val isLocked: Boolean = false,
    val lockPin: String? = null
)
