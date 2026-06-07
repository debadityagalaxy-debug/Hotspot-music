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

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import android.content.Context
import androidx.compose.ui.window.Dialog
import com.example.utils.HotspotManager
import com.example.utils.QrCodeUtils
import androidx.compose.ui.text.style.TextAlign
import android.graphics.Bitmap
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostScreen(
    viewModel: HostViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hostIp by remember { mutableStateOf(NetworkUtils.getLocalIpAddress(context) ?: "Unknown IP") }
    
    LaunchedEffect(Unit) {
        while (true) {
            hostIp = NetworkUtils.getLocalIpAddress(context) ?: "Unknown IP"
            delay(2000)
        }
    }
    
    val connectedClients by viewModel.connectedClients.collectAsStateWithLifecycle()
    val statusMessage by viewModel.hostStatus.collectAsStateWithLifecycle()
    
    var trackName by remember { mutableStateOf("No song selected") }
    var isPlaying by remember { mutableStateOf(false) }
    
    var showMusicSheet by remember { mutableStateOf(false) }
    var showHotspotDialog by remember { mutableStateOf(false) }
    
    val hotspotManager = remember { HotspotManager(context) }
    val hotspotInfo by hotspotManager.hotspotInfo.collectAsStateWithLifecycle()
    val hotspotError by hotspotManager.error.collectAsStateWithLifecycle()
    
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(android.Manifest.permission.READ_MEDIA_AUDIO, android.Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    LaunchedEffect(Unit) {
        viewModel.startServer()
        com.example.utils.DiscoveryManager.startHosting(context)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            com.example.utils.DiscoveryManager.stopHosting()
        }
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
    
    val hotspotPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
             showHotspotDialog = true
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
                            val hotspotPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.NEARBY_WIFI_DEVICES)
                            } else {
                                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            val hasPermissions = hotspotPermissions.all { 
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
                            }
                            
                            if (hasPermissions) {
                                showHotspotDialog = true
                            } else {
                                hotspotPermissionLauncher.launch(hotspotPermissions)
                            }
                        },
                        modifier = Modifier.background(ThemeSurfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = "Share Hotspot", tint = ThemeOnBackground)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                                        viewModel.pause()
                                    } else {
                                        viewModel.play() 
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
        MusicSelectorDialog(
            viewModel = viewModel,
            onDismiss = { showMusicSheet = false },
            onPlay = { song ->
                viewModel.setAudio(song.uri, song.title)
                trackName = song.title
                isPlaying = false
            }
        )
    }
    
    if (showHotspotDialog) {
        Dialog(onDismissRequest = { showHotspotDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Device Hotspot",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (hotspotInfo == null) {
                        if (hotspotError != null) {
                            Text(hotspotError!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Text("If soft hotspot fails, turn on your device's Hotspot manually. Clients on the same network will discover the host automatically.", 
                                 textAlign = TextAlign.Center, color = ThemeOnSurfaceVariant, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(
                            onClick = { hotspotManager.startHotspot() },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                        ) {
                            Text("Start Local Hotspot")
                        }
                    } else {
                        val info = hotspotInfo!!
                        // Construct the wifi string
                        // format: WIFI:T:WPA;S:Mynetwork;P:mypass;;
                        val wifiString = "WIFI:T:WPA;S:${info.ssid};P:${info.pass};;"
                        val qrBitmap = remember(wifiString) { QrCodeUtils.generate(wifiString, 600) }
                        
                        Text("1. Scan this QR using camera to connect to Wi-Fi", textAlign = TextAlign.Center, color = ThemeOnSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("SSID: ${info.ssid}\nPass: ${info.pass}", color = ThemeOnSurfaceVariant, textAlign = TextAlign.Center)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("2. Host IP (enter in app): $hostIp", fontWeight = FontWeight.Bold, color = ThemeOnBackground)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { hotspotManager.stopHotspot() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemePrimary)
                        ) {
                            Text("Stop Hotspot")
                        }
                    }
                    
                    TextButton(onClick = { showHotspotDialog = false }) {
                        Text("Close", color = ThemePrimary)
                    }
                }
            }
        }
    }
}

