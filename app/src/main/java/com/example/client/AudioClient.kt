package com.example.client

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.models.SyncMessage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioClient(private val context: Context) {
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 15_000
        }
    }

    private var wsSession: DefaultClientWebSocketSession? = null
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val syncMessageAdapter = moshi.adapter(SyncMessage::class.java)

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _clockOffset = MutableStateFlow(0L) // Server time - Client time
    val clockOffset: StateFlow<Long> = _clockOffset.asStateFlow()
    
    private val _trackName = MutableStateFlow("")
    val trackName: StateFlow<String> = _trackName.asStateFlow()
    
    private var pingJob: Job? = null
    private var syncJob: Job? = null
    
    private var hostIp: String = ""

    @OptIn(DelicateCoroutinesApi::class)
    fun connect(ip: String, port: Int = 8080) {
        hostIp = ip
        GlobalScope.launch(Dispatchers.IO) {
            _connectionStatus.value = "Connecting..."
            try {
                client.webSocket(method = HttpMethod.Get, host = ip, port = port, path = "/ws") {
                    wsSession = this
                    _connectionStatus.value = "Connected"
                    
                    // Setup Exoplayer stream exactly once we know ip
                    withContext(Dispatchers.Main) {
                        player.setMediaItem(MediaItem.fromUri("http://$ip:$port/stream"))
                        player.prepare()
                        player.playWhenReady = false
                    }

                    // Start NTP ping-pong
                    startPingPongLoop()

                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val text = frame.readText()
                        try {
                            val msg = syncMessageAdapter.fromJson(text) ?: continue
                            handleServerMessage(msg)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Error: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                wsSession = null
                stopPingPongLoop()
                if (_connectionStatus.value == "Connected") {
                    _connectionStatus.value = "Disconnected"
                }
            }
        }
    }

    private fun startPingPongLoop() {
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                if (wsSession != null) {
                    val pingMsg = SyncMessage(type = "ping", clientSendTime = System.currentTimeMillis())
                    wsSession?.send(Frame.Text(syncMessageAdapter.toJson(pingMsg)))
                }
                delay(2000)
            }
        }
    }
    
    private fun stopPingPongLoop() {
        pingJob?.cancel()
        pingJob = null
        syncJob?.cancel()
        syncJob = null
    }

    private suspend fun handleServerMessage(msg: SyncMessage) {
        // NTP-like logic
        // offset = ((t1 - t0) + (t2 - t3)) / 2
        // where t0 = clientSend, t1 = serverRecv, t2 = serverSend, t3 = clientRecv
        if (msg.type == "pong") {
            val t0 = msg.clientSendTime
            val t1 = msg.serverReceiveTime
            val t2 = msg.serverSendTime
            val t3 = System.currentTimeMillis()
            
            val offset = ((t1 - t0) + (t2 - t3)) / 2
            _clockOffset.value = offset
            return
        }

        withContext(Dispatchers.Main) {
            when (msg.type) {
                "room_info" -> {
                    _trackName.value = msg.trackName
                    // Initial setup if we join a room already playing
                }
                "play" -> {
                    _trackName.value = msg.trackName
                    // playAtTime is in server time.
                    // when should client play? 
                    // localTargetTime = serverTargetTime - clockOffset
                    val localTargetTime = msg.playAtTime - _clockOffset.value
                    schedulePlay(localTargetTime, msg.positionMs)
                }
                "pause" -> {
                    player.playWhenReady = false
                    player.seekTo(msg.positionMs)
                }
                "seek" -> {
                    player.seekTo(msg.positionMs)
                }
            }
        }
    }
    
    private fun schedulePlay(localTargetTime: Long, positionMs: Long) {
        syncJob?.cancel()
        syncJob = CoroutineScope(Dispatchers.Main).launch {
            val now = System.currentTimeMillis()
            val delayMs = localTargetTime - now
            
            if (delayMs > 0) {
                player.seekTo(positionMs)
                player.playWhenReady = false
                delay(delayMs)
                player.playWhenReady = true
            } else {
                // We missed the target start time, need to catch up
                val missedBy = -delayMs
                player.seekTo(positionMs + missedBy)
                player.playWhenReady = true
            }
        }
    }

    fun disconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            wsSession?.close()
            withContext(Dispatchers.Main) {
                player.stop()
                player.clearMediaItems()
            }
        }
    }
}
