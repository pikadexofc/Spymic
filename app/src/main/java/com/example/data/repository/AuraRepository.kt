package com.example.data.repository

import com.example.data.dao.NoteDao
import com.example.data.dao.SessionDao
import com.example.data.models.Note
import com.example.data.models.Session
import kotlinx.coroutines.flow.Flow

class AuraRepository(
    private val sessionDao: SessionDao,
    private val noteDao: NoteDao
) {
    val allSessions: Flow<List<Session>> = sessionDao.getAllSessions()

    fun getSessionById(id: Int): Flow<Session?> = sessionDao.getSessionById(id)
    
    fun getNotesForSession(sessionId: Int): Flow<List<Note>> = noteDao.getNotesForSession(sessionId)

    suspend fun insertSession(session: Session): Int {
        return sessionDao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun deleteSession(id: Int) {
        sessionDao.deleteSessionById(id)
    }
}
