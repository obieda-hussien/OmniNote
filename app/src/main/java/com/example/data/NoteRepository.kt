package com.example.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insert(note: NoteEntity) = noteDao.insertNote(note)

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)
    
    suspend fun togglePin(id: Int, isPinned: Boolean) = noteDao.updatePinnedStatus(id, isPinned)
}
