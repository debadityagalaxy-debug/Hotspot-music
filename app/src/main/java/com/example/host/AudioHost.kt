package com.example.host

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.models.SyncMessage
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.io.File
import java.time.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Collections

class AudioHost(private val context: Context) {
    private var server: ApplicationEngine? = null
    val localPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketServerSession>())
    
    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients
    
    private val _hostStatusMessage = MutableStateFlow("Stopped")
    val hostStatusMessage: StateFlow<String> = _hostStatusMessage
    
    private var currentTrackName: String = ""
    private var currentAudioUri: Uri? = null
    
    fun startServer(port: Int) {
        if (server != null) return
        
        scope.launch {
            try {
                server = embeddedServer(CIO, port = port) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    routing {
                        get("/audio") {
                            val tempFile = File(context.cacheDir, "current_audio.tmp")
                            if (tempFile.exists()) {
                                call.respondFile(tempFile)
                            } else {
                                call.respondText("Not found", status = io.ktor.http.HttpStatusCode.NotFound)
                            }
                        }
                        webSocket("/sync") {
                            sessions.add(this)
                            _connectedClients.value = sessions.size
                            
                            val initialMsg = SyncMessage(
                                type = "STATE",
                                timestamp = System.currentTimeMillis(),
                                position = localPlayer.currentPosition,
                                isPlaying = localPlayer.isPlaying,
                                trackName = currentTrackName
                            )
                            send(Frame.Text(initialMsg.toJson()))
                            
                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        val msg = SyncMessage.fromJson(text)
                                        if (msg.type == "OFFSET_PING") {
                                            val reply = SyncMessage("OFFSET_PONG", msg.timestamp, localPlayer.currentPosition, localPlayer.isPlaying)
                                            send(Frame.Text(reply.toJson()))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                            } finally {
                                sessions.remove(this)
                                _connectedClients.value = sessions.size
                            }
                        }
                    }
                }.start(wait = false)
                _hostStatusMessage.value = "Server running on port $port"
                
                launch {
                    while (isActive) {
                        delay(2000)
                        if (sessions.isNotEmpty()) {
                            val msg = SyncMessage("SYNC", System.currentTimeMillis(), localPlayer.currentPosition, localPlayer.isPlaying, currentTrackName)
                            val text = msg.toJson()
                            sessions.forEach { it.send(Frame.Text(text)) }
                        }
                    }
                }
            } catch (e: Exception) {
                _hostStatusMessage.value = "Failed: ${e.message}"
            }
        }
    }
    
    fun setAudio(uri: Uri, title: String) {
        currentTrackName = title
        currentAudioUri = uri
        
        scope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "current_audio.tmp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch(e: Exception) {}
        }
        
        scope.launch(Dispatchers.Main) {
            localPlayer.stop()
            localPlayer.clearMediaItems()
            try {
                val mediaItem = MediaItem.fromUri(uri)
                localPlayer.setMediaItem(mediaItem)
                localPlayer.prepare()
                broadcastState()
            } catch (e: Exception) {}
        }
    }
    
    fun play(position: Long) {
        scope.launch(Dispatchers.Main) {
            localPlayer.seekTo(position)
            localPlayer.play()
            broadcastState()
        }
    }
    
    fun pause(position: Long) {
        scope.launch(Dispatchers.Main) {
            localPlayer.pause()
            localPlayer.seekTo(position)
            broadcastState()
        }
    }
    
    private fun broadcastState() {
        scope.launch {
            val msg = SyncMessage("STATE", System.currentTimeMillis(), localPlayer.currentPosition, localPlayer.isPlaying, currentTrackName)
            val text = msg.toJson()
            sessions.forEach { try { it.send(Frame.Text(text)) } catch(e: Exception) { } }
        }
    }
    
    fun stop() {
        server?.stop(0, 0)
        server = null
        localPlayer.release()
        scope.cancel()
    }
}
