package com.example.data

import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val playlistDao: PlaylistDao) {
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun insert(playlist: Playlist) = playlistDao.insertPlaylist(playlist)

    suspend fun deleteById(id: Int) = playlistDao.deletePlaylistById(id)
}
