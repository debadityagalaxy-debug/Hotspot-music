package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.host.AudioHost
import com.example.models.LocalAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HostViewModel(application: Application) : AndroidViewModel(application) {
    private val audioHost = AudioHost(application)

    val connectedClients: StateFlow<Int> = audioHost.connectedClients
    val hostStatus: StateFlow<String> = audioHost.hostStatusMessage
    
    private val _localSongs = MutableStateFlow<List<LocalAudio>>(emptyList())
    val localSongs: StateFlow<List<LocalAudio>> = _localSongs.asStateFlow()

    fun fetchLocalSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<LocalAudio>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            
            try {
                getApplication<Application>().contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val duration = cursor.getLong(durationColumn)
                        val uri = ContentUris.withAppendedId(collection, id)
                        list.add(LocalAudio(uri, title, artist, duration))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _localSongs.value = list
        }
    }
    
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
