package com.example.client

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.models.SyncMessage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class AudioClient(private val context: Context) {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 15_000
        }
    }
    
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var connectionScope: CoroutineScope? = null
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    private val _trackName = MutableStateFlow("")
    val trackName: StateFlow<String> = _trackName
    
    private val _clockOffset = MutableStateFlow(0L)
    val clockOffset: StateFlow<Long> = _clockOffset
    
    fun connect(hostIp: String, port: Int = 8080) {
        connectionScope?.cancel()
        connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        _connectionStatus.value = "Connecting..."
        
        connectionScope?.launch {
            try {
                withContext(Dispatchers.Main) {
                    player.stop()
                    player.clearMediaItems()
                    val mediaItem = MediaItem.fromUri("http://$hostIp:$port/audio")
                    player.setMediaItem(mediaItem)
                    player.prepare()
                }
                
                client.webSocket(method = io.ktor.http.HttpMethod.Get, host = hostIp, port = port, path = "/sync") {
                    _connectionStatus.value = "Connected"
                    
                    launch {
                        while(isActive) {
                            val pingMsg = SyncMessage("OFFSET_PING", System.currentTimeMillis(), 0, false)
                            send(Frame.Text(pingMsg.toJson()))
                            delay(5000)
                        }
                    }
                    
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val msg = SyncMessage.fromJson(frame.readText())
                            when(msg.type) {
                                "OFFSET_PONG" -> {
                                    val rtt = System.currentTimeMillis() - msg.timestamp
                                    _clockOffset.value = (System.currentTimeMillis() - rtt / 2) - msg.timestamp
                                }
                                "STATE", "SYNC" -> {
                                    withContext(Dispatchers.Main) {
                                        if (msg.trackName != null) _trackName.value = msg.trackName
                                        val targetPosition = msg.position + _clockOffset.value
                                        
                                        if (msg.isPlaying && !player.isPlaying) {
                                            player.seekTo(targetPosition)
                                            player.play()
                                        } else if (!msg.isPlaying && player.isPlaying) {
                                            player.pause()
                                            player.seekTo(targetPosition)
                                        } else if (msg.isPlaying && abs(player.currentPosition - targetPosition) > 150) {
                                            player.seekTo(targetPosition)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Failed: ${e.message}"
            } finally {
                withContext(Dispatchers.Main) { player.stop() }
            }
        }
    }
    
    fun disconnect() {
        connectionScope?.cancel()
        _connectionStatus.value = "Disconnected"
    }
}
