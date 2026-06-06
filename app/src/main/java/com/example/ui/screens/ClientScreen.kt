package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientScreen(
    viewModel: ClientViewModel,
    onNavigateBack: () -> Unit
) {
    var ipInput by remember { mutableStateOf("") }
    
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val trackName by viewModel.trackName.collectAsStateWithLifecycle()
    val clockOffset by viewModel.clockOffset.collectAsStateWithLifecycle()
    
    val isConnected = connectionStatus == "Connected"

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
                            .background(if (isConnected) ThemePrimary else ThemeSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isConnected) Icons.Rounded.Sensors else Icons.Rounded.SensorsOff, 
                            contentDescription = null, 
                            tint = if (isConnected) ThemeOnPrimary else ThemeOnBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("SyncBeat", color = ThemeOnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isConnected) Color.Green else Color.Gray))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(connectionStatus, color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (!isConnected) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CellTower, 
                        contentDescription = null,
                        tint = ThemePrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Join a Room", 
                        color = ThemeOnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enter the Host's IP Address to sync playback. Make sure you are on the same Wi-Fi.", 
                        color = ThemeOnSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("Host IP Address", color = ThemePrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimary,
                            unfocusedBorderColor = ThemeSurfaceVariant,
                            focusedTextColor = ThemeOnBackground,
                            unfocusedTextColor = ThemeOnBackground
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.connect(ipInput.trim()) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                    ) {
                        Text("CONNECT TO HOST", color = ThemeOnPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            } else {
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
                        text = trackName.ifEmpty { "Waiting for host..." },
                        color = ThemeOnBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .background(ThemeSecondaryContainer, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${clockOffset}ms LATENCY", 
                            color = ThemePrimary, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeSurface, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .shadow(32.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Synced with Host. Controls are disabled on client.",
                            color = ThemeOnSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedButton(
                            onClick = { viewModel.disconnect() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemePrimary)
                        ) {
                            Text("DISCONNECT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
