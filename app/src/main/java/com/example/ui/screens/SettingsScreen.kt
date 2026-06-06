package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ConnectionStatus
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isConnected = state.connectionStatus == ConnectionStatus.CONNECTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Simulated connection status toggle for demonstration purposes
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Connection:",
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        val nextStatus = if (isConnected) ConnectionStatus.DISCONNECTED else ConnectionStatus.CONNECTED
                        viewModel.updateConnectionStatus(nextStatus)
                    }
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto-connect toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-Connect Mode",
                    color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Switch(
                    checked = state.isAutoConnectEnabled,
                    onCheckedChange = { viewModel.toggleAutoConnect(it) },
                    enabled = isConnected
                )
            }

            // Low-latency toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use Low Latency",
                    color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Switch(
                    checked = state.useLowLatency,
                    onCheckedChange = { viewModel.toggleLowLatency(it) },
                    enabled = isConnected
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeout Slider
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    text = "Connection Timeout: ${state.autoConnectTimeout.toInt()}s",
                    color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Slider(
                    value = state.autoConnectTimeout,
                    onValueChange = { viewModel.setAutoConnectTimeout(it) },
                    valueRange = 5f..60f,
                    steps = 10,
                    enabled = isConnected
                )
            }
        }
    }
}
