package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    sessionId: Int,
    viewModel: AuraViewModel,
    onBack: () -> Unit
) {
    val session by viewModel.getSessionById(sessionId).collectAsState()
    val notes by viewModel.getNotesForSession(sessionId).collectAsState(emptyList())
    
    val playbackPosMs by viewModel.playbackPosMs.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.audioPlayer.pause()
            viewModel.stopPlaybackTimer()
        }
    }
    
    val manualNotes = notes.filter { !it.isAutomated }
    val automatedNotes = notes.filter { it.isAutomated }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Playback") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Player
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authorized Listener Console",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    viewModel.audioPlayer.pause()
                                    viewModel.stopPlaybackTimer()
                                } else {
                                    session?.fileUri?.let { uri ->
                                        if (uri.isNotEmpty()) {
                                            viewModel.audioPlayer.playFile(uri) {
                                                isPlaying = false
                                                viewModel.stopPlaybackTimer()
                                            }
                                            viewModel.startPlaybackTimer()
                                        }
                                    }
                                }
                                isPlaying = !isPlaying
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        val dur = viewModel.audioPlayer.getDuration()
                        val pos = playbackPosMs.toInt()
                        
                        Slider(
                            value = if (dur > 0) pos.toFloat() / dur.toFloat() else 0f,
                            onValueChange = { frac ->
                                val newPos = (frac * dur).toInt()
                                viewModel.audioPlayer.seekTo(newPos)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    val pMins = (playbackPosMs / 1000) / 60
                    val pSecs = (playbackPosMs / 1000) % 60
                    Text(String.format("%02d:%02d", pMins, pSecs), color = MaterialTheme.colorScheme.onSurface)
                }
            }
            
            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Manual Vault") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Automated Vault") })
            }
            
            val displayedNotes = if (selectedTab == 0) manualNotes else automatedNotes
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = {
                    val transcript = displayedNotes.joinToString("\n") { 
                        val m = (it.deltaMs / 1000) / 60
                        val s = (it.deltaMs / 1000) % 60
                        "[%02d:%02d] %s".format(m,s, it.content)
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
                }) {
                    Text("Copy All")
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedNotes) { note ->
                    val isNear = Math.abs(note.deltaMs - playbackPosMs) < 2000
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNear) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                viewModel.audioPlayer.seekTo(note.deltaMs.toInt())
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val m = (note.deltaMs / 1000) / 60
                        val s = (note.deltaMs / 1000) % 60
                        Text(
                            "[%02d:%02d]".format(m,s), 
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(note.content, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Note", "[%02d:%02d] %s".format(m,s, note.content)))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Note", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
