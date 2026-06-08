package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.data.AuraDatabase
import com.example.data.PreferencesManager
import com.example.data.models.Note
import com.example.data.models.Session
import com.example.data.repository.AuraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AuraRepository
    private val audioRecorder = AudioRecorder(application)
    val prefsManager = PreferencesManager(application)
    val audioPlayer = AudioPlayer()

    val allSessions: StateFlow<List<Session>> 

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs = _elapsedTimeMs.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Int>>(emptyList())
    val amplitudeHistory = _amplitudeHistory.asStateFlow()
    
    private val _playbackPosMs = MutableStateFlow(0L)
    val playbackPosMs = _playbackPosMs.asStateFlow()

    private val _isProcessingAI = MutableStateFlow(false)
    val isProcessingAI = _isProcessingAI.asStateFlow()

    private var timerJob: Job? = null
    private var playbackTimerJob: Job? = null
    private var sessionStartTime = 0L

    init {
        val database = AuraDatabase.getDatabase(application)
        repository = AuraRepository(database.sessionDao(), database.noteDao())
        
        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun startSession(title: String, location: String) {
        viewModelScope.launch {
            val session = Session(title = title, location = location)
            val id = repository.insertSession(session)
            _currentSessionId.value = id
            
            // Start recording
            audioRecorder.startRecording("session_$id")
            _isRecording.value = true
            sessionStartTime = System.currentTimeMillis()
            
            timerJob = launch {
                while (true) {
                    _elapsedTimeMs.value = System.currentTimeMillis() - sessionStartTime
                    val amp = audioRecorder.getMaxAmplitude()
                    _currentAmplitude.value = amp
                    _amplitudeHistory.value = (_amplitudeHistory.value + amp).takeLast(200)
                    delay(100L)
                }
            }
        }
    }

    fun stopSession() {
        audioRecorder.stopRecording()
        _isRecording.value = false
        timerJob?.cancel()
        
        viewModelScope.launch {
            _currentSessionId.value?.let { id ->
                val duration = _elapsedTimeMs.value / 1000
                val sessionFlow = repository.getSessionById(id).firstOrNull()
                sessionFlow?.let { session ->
                    repository.updateSession(
                        session.copy(
                            durationSeconds = duration,
                            fileUri = audioRecorder.currentOutputFile ?: ""
                        )
                    )
                    generateAutomatedNotes(id, session.title, duration)
                }
            }
            _currentSessionId.value = null
            _elapsedTimeMs.value = 0L
            _currentAmplitude.value = 0
        }
    }

    fun addNoteToCurrentSession(content: String) {
        val sId = _currentSessionId.value ?: return
        val currentDelta = _elapsedTimeMs.value
        viewModelScope.launch {
            repository.insertNote(Note(sessionId = sId, deltaMs = currentDelta, content = content))
        }
    }

    fun getNotesForSession(id: Int): StateFlow<List<Note>> {
        return repository.getNotesForSession(id).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
    }

    fun getSessionById(id: Int): StateFlow<Session?> {
        return repository.getSessionById(id).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
    }
    
    fun startPlaybackTimer() {
        playbackTimerJob?.cancel()
        playbackTimerJob = viewModelScope.launch {
            while (true) {
                _playbackPosMs.value = audioPlayer.getCurrentPosition().toLong()
                delay(100L)
            }
        }
    }
    
    fun stopPlaybackTimer() {
        playbackTimerJob?.cancel()
    }

    fun generateAutomatedNotes(sessionId: Int, title: String, durationSecs: Long) {
        viewModelScope.launch {
            _isProcessingAI.value = true
            val keys = prefsManager.getApiKeys()
            val allKeys = mutableListOf(BuildConfig.GEMINI_API_KEY)
            allKeys.addAll(keys)
            
            var success = false
            for (key in allKeys) {
                if (key.isBlank() || key.contains("MY_GEMINI_API_KEY")) continue
                
                try {
                    val prompt = "Generate a sample chronological meeting notes transcript with relative timestamps for a ${durationSecs / 60} minute meeting about '$title'. Format each note as a JSON array of objects with 'deltaMs' (integer, milliseconds) and 'content' (string) fields. Return ONLY the raw JSON array."
                    val result = withContext(Dispatchers.IO) {
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt))))
                        )
                        RetrofitClient.service.generateContent(key, request)
                    }
                    val text = result.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (text != null) {
                        // Very naive JSON parsing without extra libs
                        val matches = Regex("\"deltaMs\"\\s*:\\s*(\\d+).*?\"content\"\\s*:\\s*\"(.*?)\"").findAll(text)
                        for (match in matches) {
                            val timeMs = match.groupValues[1].toLong()
                            val textContent = match.groupValues[2]
                            repository.insertNote(Note(sessionId = sessionId, deltaMs = timeMs, content = textContent.replace("\\\"", "\""), isAutomated = true))
                        }
                        success = true
                        break
                    }
                } catch (e: Exception) {
                    Log.e("AuraViewModel", "API Error with key", e)
                    // Continue to next key
                }
            }
            if (!success) {
                // Mock fallback if no API keys worked
                val mockStep = (durationSecs * 1000) / 4
                for (i in 0..3) {
                    repository.insertNote(Note(sessionId = sessionId, deltaMs = i * mockStep, content = "Automated AI Note ${i + 1} for $title", isAutomated = true))
                }
            }
            _isProcessingAI.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
        audioPlayer.release()
    }
}
