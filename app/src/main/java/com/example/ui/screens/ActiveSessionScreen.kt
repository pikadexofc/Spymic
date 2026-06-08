package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraViewModel
import androidx.compose.ui.platform.LocalConfiguration
import com.example.ui.components.ShareDialog
import com.example.ui.components.VisualizerCanvas

@Composable
fun ActiveSessionScreen(
    viewModel: AuraViewModel,
    onStopSession: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val elapsedTimeMs by viewModel.elapsedTimeMs.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val amplitudeHistory by viewModel.amplitudeHistory.collectAsState()
    
    val notes = currentSessionId?.let { viewModel.getNotesForSession(it).collectAsState(emptyList()).value } ?: emptyList()
    var noteInput by remember { mutableStateOf("") }
    var showShare by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    
    // Fake local filter states for UI purposes
    var lowCut by remember { mutableStateOf(80f) }
    var compressor by remember { mutableStateOf(4f) }
    
    val config = LocalConfiguration.current
    val isPortrait = config.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    if (showShare && currentSessionId != null) {
        ShareDialog(sessionId = currentSessionId!!, onDismiss = { showShare = false })
    }

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Manual Filter Controls") },
            text = {
                Column {
                    Text("Low-Cut Filter (${lowCut.toInt()} Hz)")
                    Slider(value = lowCut, onValueChange = { lowCut = it }, valueRange = 20f..200f)
                    
                    Text("Compression Ratio (${compressor.toInt()}:1)")
                    Slider(value = compressor, onValueChange = { compressor = it }, valueRange = 1f..10f)
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilters = false }) { Text("Apply") }
            }
        )
    }

    if (isPortrait) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            RecordingBanner(alpha, elapsedTimeMs, onShare = { showShare = true }, onFilters = { showFilters = true })
            
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(16.dp)) {
                VisualizerCanvas(amplitudes = amplitudeHistory)
            }
            
            Box(Modifier.weight(1f)) {
                NotesList(notes = notes, modifier = Modifier.fillMaxSize())
            }
            
            NoteInputArea(
                noteValue = noteInput,
                onNoteChange = { noteInput = it },
                onAddNote = { 
                    if (noteInput.isNotBlank()) {
                        viewModel.addNoteToCurrentSession(noteInput)
                        noteInput = ""
                    }
                },
                onStop = {
                    viewModel.stopSession()
                    onStopSession()
                }
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Column(modifier = Modifier.weight(1f)) {
                RecordingBanner(alpha, elapsedTimeMs, onShare = { showShare = true }, onFilters = { showFilters = true })
                Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                    VisualizerCanvas(amplitudes = amplitudeHistory)
                }
            }
            Column(modifier = Modifier.weight(1.5f)) {
                Box(Modifier.weight(1f)) {
                    NotesList(notes = notes, modifier = Modifier.fillMaxSize())
                }
                NoteInputArea(
                    noteValue = noteInput,
                    onNoteChange = { noteInput = it },
                    onAddNote = {
                        if(noteInput.isNotBlank()) {
                            viewModel.addNoteToCurrentSession(noteInput)
                            noteInput = ""
                        }
                    },
                    onStop = {
                        viewModel.stopSession()
                        onStopSession()
                    }
                )
            }
        }
    }
}

@Composable
fun RecordingBanner(alpha: Float, elapsedTimeMs: Long, onShare: () -> Unit, onFilters: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECORDING ACTIVE",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                val seconds = (elapsedTimeMs / 1000) % 60
                val minutes = (elapsedTimeMs / 1000) / 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row {
                IconButton(onClick = onFilters) {
                    Icon(Icons.Default.Tune, contentDescription = "Filters", tint = MaterialTheme.colorScheme.onError)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

@Composable
fun NotesList(notes: List<com.example.data.models.Note>, modifier: Modifier) {
    val manualNotes = notes.filter { !it.isAutomated }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(manualNotes) { note ->
            val seconds = (note.deltaMs / 1000) % 60
            val minutes = (note.deltaMs / 1000) / 60
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = String.format("[%02d:%02d]", minutes, seconds),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = note.content,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NoteInputArea(
    noteValue: String,
    onNoteChange: (String) -> Unit,
    onAddNote: () -> Unit,
    onStop: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = noteValue,
                onValueChange = onNoteChange,
                placeholder = { Text("Add Timestamped Note...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAddNote,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Session", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}
