package com.example.models

import org.json.JSONObject

data class SyncMessage(
    val type: String,
    val timestamp: Long,
    val position: Long,
    val isPlaying: Boolean,
    val trackName: String? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type)
        json.put("timestamp", timestamp)
        json.put("position", position)
        json.put("isPlaying", isPlaying)
        if (trackName != null) {
            json.put("trackName", trackName)
        }
        return json.toString()
    }
    
    companion object {
        fun fromJson(jsonString: String): SyncMessage {
            val json = JSONObject(jsonString)
            return SyncMessage(
                type = json.getString("type"),
                timestamp = json.getLong("timestamp"),
                position = json.getLong("position"),
                isPlaying = json.getBoolean("isPlaying"),
                trackName = if (json.has("trackName")) json.getString("trackName") else null
            )
        }
    }
}
