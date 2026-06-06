package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED
}

data class SettingsState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isAutoConnectEnabled: Boolean = false,
    val autoConnectTimeout: Float = 15f,
    val useLowLatency: Boolean = true
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun updateConnectionStatus(status: ConnectionStatus) {
        _state.update { it.copy(connectionStatus = status) }
    }

    fun toggleAutoConnect(enabled: Boolean) {
        _state.update { it.copy(isAutoConnectEnabled = enabled) }
    }

    fun setAutoConnectTimeout(timeout: Float) {
        _state.update { it.copy(autoConnectTimeout = timeout) }
    }

    fun toggleLowLatency(enabled: Boolean) {
        _state.update { it.copy(useLowLatency = enabled) }
    }
}
