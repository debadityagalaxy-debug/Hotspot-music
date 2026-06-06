package com.example.host

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.models.SyncMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.Duration
import java.util.Collections

class AudioHost(private val context: Context) {
    private var server: NettyApplicationEngine? = null
    private val connections = Collections.synchronizedSet<DefaultWebSocketServerSession>(LinkedHashSet())
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val syncMessageAdapter = moshi.adapter(SyncMessage::class.java)

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    private val _hostStatusMessage = MutableStateFlow("Stopped")
    val hostStatusMessage: StateFlow<String> = _hostStatusMessage.asStateFlow()

    val localPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    
    var currentAudioUri: Uri? = null
    var currentAudioFile: java.io.File? = null
    var currentTrackName: String = ""
    var isPlaying = false
    var currentPositionMs: Long = 0
    private var syncJob: Job? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun startServer(port: Int = 8080) {
        if (server != null) return
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                server = embeddedServer(Netty, port = port) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    install(CORS) {
                        anyHost()
                    }
                    install(io.ktor.server.plugins.partialcontent.PartialContent) // Enables Range requests
                    routing {
                        get("/stream") {
                            val file = currentAudioFile
                            if (file != null && file.exists()) {
                                call.respondFile(file)
                            } else {
                                call.respond(io.ktor.http.HttpStatusCode.NotFound, "No audio selected")
                            }
                        }

                        webSocket("/ws") {
                            connections.add(this)
                            updateClientCount()
                            
                            // Send initial room state
                            send(Frame.Text(syncMessageAdapter.toJson(
                                SyncMessage(
                                    type = "room_info",
                                    trackName = currentTrackName,
                                    isPlaying = isPlaying,
                                    positionMs = currentPositionMs
                                )
                            )))

                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        try {
                                            val message = syncMessageAdapter.fromJson(text)
                                            if (message?.type == "ping") {
                                                // Respond with server time for NTP-like sync
                                                val serverRecvTime = System.currentTimeMillis()
                                                val pongMsg = message.copy(
                                                    type = "pong",
                                                    serverReceiveTime = serverRecvTime,
                                                    serverSendTime = System.currentTimeMillis()
                                                )
                                                send(Frame.Text(syncMessageAdapter.toJson(pongMsg)))
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            } finally {
                                connections.remove(this)
                                updateClientCount()
                            }
                        }
                    }
                }.start(wait = false)
                _hostStatusMessage.value = "Server running on port $port"
            } catch (e: Exception) {
                _hostStatusMessage.value = "Failed to start server: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun updateClientCount() {
        _connectedClients.value = connections.size
    }

    private fun broadcast(message: SyncMessage) {
        val textFrame = Frame.Text(syncMessageAdapter.toJson(message))
        synchronized(connections) {
            connections.forEach { session ->
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        session.send(textFrame)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun setAudio(uri: Uri, name: String) {
        currentAudioUri = uri
        currentTrackName = name
        
        // Setup local player
        localPlayer.setMediaItem(MediaItem.fromUri(uri))
        localPlayer.prepare()
        localPlayer.playWhenReady = false
        
        // Copy to temp file so Ktor can serve it using respondFile and support Range header
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val tempFile = java.io.File(context.cacheDir, "current_audio_stream.tmp")
                this@AudioHost.context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                currentAudioFile = tempFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        broadcast(SyncMessage(type = "room_info", trackName = currentTrackName, isPlaying = isPlaying))
    }

    fun play(positionMs: Long) {
        isPlaying = true
        this.currentPositionMs = positionMs
        
        // Add a 500ms delay to give clients time to buffer before starting playback
        val playAtExpectedTime = System.currentTimeMillis() + 500 
        
        broadcast(SyncMessage(
            type = "play",
            playAtTime = playAtExpectedTime,
            positionMs = positionMs,
            trackName = currentTrackName,
            isPlaying = true
        ))
        
        // Schedule local playback
        syncJob?.cancel()
        syncJob = CoroutineScope(Dispatchers.Main).launch {
            val now = System.currentTimeMillis()
            val delayMs = playAtExpectedTime - now
            if (delayMs > 0) {
                localPlayer.seekTo(positionMs)
                localPlayer.playWhenReady = false
                delay(delayMs)
                localPlayer.playWhenReady = true
            } else {
                localPlayer.seekTo(positionMs - delayMs)
                localPlayer.playWhenReady = true
            }
        }
    }

    fun pause(positionMs: Long) {
        isPlaying = false
        this.currentPositionMs = positionMs
        syncJob?.cancel()
        CoroutineScope(Dispatchers.Main).launch {
            localPlayer.playWhenReady = false
            localPlayer.seekTo(positionMs)
        }
        broadcast(SyncMessage(
            type = "pause",
            positionMs = positionMs,
            trackName = currentTrackName,
            isPlaying = false
        ))
    }
    
    fun seek(positionMs: Long) {
        this.currentPositionMs = positionMs
        CoroutineScope(Dispatchers.Main).launch {
            localPlayer.seekTo(positionMs)
        }
        broadcast(SyncMessage(
            type = "seek",
            positionMs = positionMs,
            trackName = currentTrackName,
            isPlaying = isPlaying
        ))
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        _hostStatusMessage.value = "Stopped"
        CoroutineScope(Dispatchers.Main).launch {
            localPlayer.release()
        }
    }
}
