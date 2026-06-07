package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HotspotInfo(val ssid: String, val pass: String)

class HotspotManager(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    private val _hotspotInfo = MutableStateFlow<HotspotInfo?>(null)
    val hotspotInfo: StateFlow<HotspotInfo?> = _hotspotInfo

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    @SuppressLint("MissingPermission")
    fun startHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                        super.onStarted(res)
                        reservation = res
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val config = res.softApConfiguration
                            val ssid = config.ssid ?: ""
                            val pass = config.passphrase ?: ""
                            _hotspotInfo.value = HotspotInfo(ssid, pass)
                        } else {
                            @Suppress("DEPRECATION")
                            val config = res.wifiConfiguration
                            var ssid = config?.SSID ?: ""
                            var pass = config?.preSharedKey ?: ""
                            if (ssid.startsWith("\"") && ssid.endsWith("\"")) ssid = ssid.substring(1, ssid.length - 1)
                            if (pass.startsWith("\"") && pass.endsWith("\"")) pass = pass.substring(1, pass.length - 1)
                            _hotspotInfo.value = HotspotInfo(ssid, pass)
                        }
                    }

                    override fun onStopped() {
                        super.onStopped()
                        _hotspotInfo.value = null
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        val msg = when (reason) {
                            WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL -> "Channel not available"
                            WifiManager.LocalOnlyHotspotCallback.ERROR_GENERIC -> "Not supported by device. Try turning on Android Hotspot manually."
                            WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE -> "Incompatible mode (disconnect from current WiFi first)"
                            WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED -> "Tethering disallowed"
                            else -> "Reason: $reason"
                        }
                        _error.value = "Failed: $msg"
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                _error.value = e.message
            }
        } else {
            _error.value = "Not supported on this Android version"
        }
    }

    fun stopHotspot() {
        reservation?.close()
        reservation = null
        _hotspotInfo.value = null
    }
}
