package com.example.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.HostViewModel
import com.example.utils.NetworkUtils
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreen(
    viewModel: HostViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val hostIp = remember { NetworkUtils.getLocalIpAddress(context) ?: "Unknown IP" }
    
    val connectedClients by viewModel.connectedClients.collectAsStateWithLifecycle()
    val statusMessage by viewModel.hostStatus.collectAsStateWithLifecycle()
    
    var trackName by remember { mutableStateOf("No song selected") }
    var isPlaying by remember { mutableStateOf(false) }
    
    var showMusicSheet by remember { mutableStateOf(false) }
    
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(android.Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    LaunchedEffect(Unit) {
        viewModel.startServer()
    }
    
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            var name = "Unknown Track"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                }
            }
            trackName = name
            viewModel.setAudio(it, name)
            isPlaying = false
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            viewModel.fetchLocalSongs()
            showMusicSheet = true
        } else {
            audioPicker.launch(arrayOf("audio/*"))
        }
    }

    Scaffold(
        containerColor = ThemeBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ThemePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Sensors, contentDescription = null, tint = ThemeOnPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("SyncBeat", color = ThemeOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Host Mode • IP: $hostIp", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val hasPermission = permissions.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (hasPermission) {
                                viewModel.fetchLocalSongs()
                                showMusicSheet = true
                            } else {
                                permissionLauncher.launch(permissions.toTypedArray())
                            }
                        },
                        modifier = Modifier.background(ThemeSurfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = "Add Music", tint = ThemeOnBackground)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(32.dp)
                            .background(ThemePrimary.copy(alpha = 0.1f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(40.dp))
                            .background(Brush.linearGradient(listOf(ThemeSurfaceVariant, ThemeBackground)))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(40.dp))
                            .shadow(24.dp, RoundedCornerShape(40.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .border(4.dp, ThemePrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(ThemePrimary, CircleShape)
                                    .shadow(20.dp, CircleShape, spotColor = ThemePrimary)
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = ThemeOnPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = trackName,
                    color = ThemeOnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .background(ThemeSecondaryContainer, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ULTRA-SYNC ACTIVE", 
                        color = ThemePrimary, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ThemeSurface)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONNECTED DEVICES ($connectedClients)", 
                            color = ThemeOnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ThemePrimary, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Smartphone, contentDescription = null, tint = ThemeOnPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Host", color = ThemeOnBackground, fontSize = 10.sp)
                        }
                        
                        for (i in 0 until connectedClients) {
                             Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ThemeSurfaceVariant, RoundedCornerShape(16.dp))
                                        .border(1.dp, ThemePrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = ThemePrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Client ${i+1}", color = ThemeOnBackground, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeSurface, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .shadow(32.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Previous */ }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = ThemeOnBackground, modifier = Modifier.size(32.dp))
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(8.dp, CircleShape)
                                .background(ThemePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {
                                if (trackName != "No song selected") {
                                    if (isPlaying) {
                                        viewModel.pause(0)
                                    } else {
                                        viewModel.play(0) 
                                    }
                                    isPlaying = !isPlaying
                                }
                            }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                    contentDescription = "Play/Pause", 
                                    tint = ThemeOnPrimary, 
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        
                        IconButton(onClick = { /* Next */ }) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = ThemeOnBackground, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
    
    if (showMusicSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMusicSheet = false },
            sheetState = sheetState,
            containerColor = ThemeSurface,
            contentColor = ThemeOnSurface
        ) {
            val songs by viewModel.localSongs.collectAsStateWithLifecycle()
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    "Local Music", 
                    style = MaterialTheme.typography.titleLarge, 
                    color = ThemeOnBackground,
                    modifier = Modifier.padding(16.dp)
                )
                if (songs.isEmpty()) {
                    Text(
                        "No music found. Use file picker instead?",
                        color = ThemePrimary,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                showMusicSheet = false
                                audioPicker.launch(arrayOf("audio/*"))
                            }
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(songs) { song ->
                            ListItem(
                                headlineContent = { Text(song.title, color = ThemeOnBackground, maxLines = 1) },
                                supportingContent = { Text(song.artist, color = ThemeOnSurfaceVariant, maxLines = 1) },
                                leadingContent = { 
                                    Box(
                                        modifier = Modifier.size(40.dp).background(ThemePrimary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, null, tint = ThemePrimary)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    viewModel.setAudio(song.uri, song.title)
                                    trackName = song.title
                                    isPlaying = false
                                    showMusicSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
