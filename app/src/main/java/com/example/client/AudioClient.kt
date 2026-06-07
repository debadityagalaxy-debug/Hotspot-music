package com.example.client

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
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
        install(WebSockets)
    }
    
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var connectionScope: CoroutineScope? = null
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    private val _trackName = MutableStateFlow("")
    val trackName: StateFlow<String> = _trackName
    
    private val _clockOffset = MutableStateFlow(0L)
    val clockOffset: StateFlow<Long> = _clockOffset
    
    private val syncMeasurements = mutableListOf<Pair<Long, Long>>()
    
    fun connect(hostIp: String, port: Int = 8080) {
        connectionScope?.cancel()
        connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        syncMeasurements.clear()
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
                        var pingCount = 0
                        while(isActive) {
                            val pingMsg = SyncMessage("OFFSET_PING", System.currentTimeMillis(), 0, false)
                            send(Frame.Text(pingMsg.toJson()))
                            pingCount++
                            val delayMs = if (pingCount <= 10) 500L else 5000L
                            delay(delayMs)
                        }
                    }
                    
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val msg = SyncMessage.fromJson(frame.readText())
                            when(msg.type) {
                                "OFFSET_PONG" -> {
                                    val t_c1 = System.currentTimeMillis()
                                    val t_c0 = msg.position
                                    val t_h = msg.timestamp
                                    val rtt = t_c1 - t_c0
                                    val expectedHostTimeAtTc1 = t_h + rtt / 2
                                    val newOffset = expectedHostTimeAtTc1 - t_c1
                                    
                                    syncMeasurements.add(Pair(rtt, newOffset))
                                    if (syncMeasurements.size > 10) {
                                        syncMeasurements.removeAt(0)
                                    }
                                    val bestMeasurement = syncMeasurements.minByOrNull { it.first }
                                    if (bestMeasurement != null) {
                                        _clockOffset.value = bestMeasurement.second
                                    }
                                }
                                "STATE", "SYNC" -> {
                                    withContext(Dispatchers.Main) {
                                        if (msg.trackName != null) _trackName.value = msg.trackName
                                        
                                        val estimatedHostTimeNow = System.currentTimeMillis() + _clockOffset.value
                                        val messageTransitTime = estimatedHostTimeNow - msg.timestamp
                                        val targetPosition = msg.position + if (msg.isPlaying) messageTransitTime else 0L
                                        
                                        if (msg.isPlaying) {
                                            if (!player.isPlaying) {
                                                player.seekTo(targetPosition)
                                                player.play()
                                                player.playbackParameters = PlaybackParameters(1.0f)
                                            } else {
                                                val diff = targetPosition - player.currentPosition
                                                if (abs(diff) > 2000) {
                                                    // Hard seek if difference is large
                                                    player.seekTo(targetPosition)
                                                    player.playbackParameters = PlaybackParameters(1.0f)
                                                } else if (abs(diff) > 50) {
                                                    // Soft catch-up/slow-down
                                                    val speed = if (diff > 0) 1.05f else 0.95f
                                                    player.playbackParameters = PlaybackParameters(speed)
                                                } else {
                                                    // Sync is good
                                                    player.playbackParameters = PlaybackParameters(1.0f)
                                                }
                                            }
                                        } else {
                                            if (player.isPlaying) {
                                                player.pause()
                                                player.seekTo(targetPosition)
                                            }
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
                withContext(NonCancellable + Dispatchers.Main) { player.stop() }
            }
        }
    }
    
    fun disconnect() {
        connectionScope?.cancel()
        _connectionStatus.value = "Disconnected"
    }
}
