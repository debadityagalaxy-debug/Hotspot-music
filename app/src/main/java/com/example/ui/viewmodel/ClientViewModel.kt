package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.client.AudioClient
import kotlinx.coroutines.flow.StateFlow

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val audioClient = AudioClient(application)
    
    val connectionStatus: StateFlow<String> = audioClient.connectionStatus
    val trackName: StateFlow<String> = audioClient.trackName
    val clockOffset: StateFlow<Long> = audioClient.clockOffset
    
    fun connect(ip: String) {
        if (ip.isNotBlank()) audioClient.connect(ip)
    }
    
    fun disconnect() {
        audioClient.disconnect()
    }
    
    override fun onCleared() {
        super.onCleared()
        audioClient.disconnect()
    }
}
