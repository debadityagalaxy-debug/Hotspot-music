package com.example.models
import android.net.Uri

data class LocalAudio(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)
