package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncMessage(
    val type: String, // "ping", "pong", "play", "pause", "seek", "status", "room_info"
    val clientSendTime: Long = 0,
    val serverReceiveTime: Long = 0,
    val serverSendTime: Long = 0,
    val clientReceiveTime: Long = 0,
    val playAtTime: Long = 0, // Server clock time when playback should precisely start
    val positionMs: Long = 0, // Where in the track to start
    val trackName: String = "",
    val isPlaying: Boolean = false
)
