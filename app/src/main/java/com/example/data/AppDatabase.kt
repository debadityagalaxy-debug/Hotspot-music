package com.example.data

import android.net.Uri
import androidx.room.*
import com.example.models.LocalAudio
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val songsJson: String 
) {
    fun getSongs(): List<LocalAudio> {
        val list = mutableListOf<LocalAudio>()
        try {
            val array = JSONArray(songsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LocalAudio(
                        uri = Uri.parse(obj.getString("uri")),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Unknown"),
                        durationMs = obj.optLong("durationMs", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        fun fromSongs(name: String, songs: List<LocalAudio>, id: Int = 0): Playlist {
            val array = JSONArray()
            for (song in songs) {
                val obj = JSONObject()
                obj.put("uri", song.uri.toString())
                obj.put("title", song.title)
                obj.put("artist", song.artist)
                obj.put("durationMs", song.durationMs)
                array.put(obj)
            }
            return Playlist(id = id, name = name, songsJson = array.toString())
        }
    }
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Int)
}

@Database(entities = [Playlist::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
