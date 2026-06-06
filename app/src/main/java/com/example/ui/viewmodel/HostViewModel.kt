package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.host.AudioHost
import kotlinx.coroutines.flow.StateFlow

class HostViewModel(application: Application) : AndroidViewModel(application) {
    private val audioHost = AudioHost(application)

    val connectedClients: StateFlow<Int> = audioHost.connectedClients
    val hostStatus: StateFlow<String> = audioHost.hostStatusMessage
    
    fun startServer(port: Int = 8080) {
        audioHost.startServer(port)
    }

    fun stopServer() {
        audioHost.stopServer()
    }

    fun setAudio(uri: Uri, name: String) {
        audioHost.setAudio(uri, name)
    }

    fun play(positionMs: Long = 0) {
        audioHost.play(positionMs)
    }

    fun pause(positionMs: Long = 0) {
        audioHost.pause(positionMs)
    }

    fun seek(positionMs: Long) {
        audioHost.seek(positionMs)
    }
    
    override fun onCleared() {
        super.onCleared()
        audioHost.stopServer()
    }
}
