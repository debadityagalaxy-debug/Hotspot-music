package com.example.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.util.Locale

object NetworkUtils {
    fun getLocalIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            // If Wi-Fi is disconnected (ipAddress == 0), maybe we are the host. Iterate NetworkInterfaces.
            if (ipAddress == 0) {
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces().toList()
                val validAddresses = interfaces.filter { 
                    val name = it.name?.lowercase() ?: ""
                    name.contains("wlan") || name.contains("ap") || name.contains("rndis") || name.contains("swlan")
                }.flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
                
                return validAddresses.firstOrNull()?.hostAddress 
                    ?: interfaces.flatMap { it.inetAddresses.toList() }
                        .filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
                        .firstOrNull { 
                            val addr = it.hostAddress ?: ""
                            !addr.startsWith("10.") && !addr.startsWith("100.")
                        }?.hostAddress 
                    ?: interfaces.flatMap { it.inetAddresses.toList() }
                        .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }?.hostAddress
            }
            return String.format(
                Locale.US,
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) { return null }
    }
    
    fun getDefaultGateway(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo
            if (dhcp != null && dhcp.gateway != 0) {
                val ipAddress = dhcp.gateway
                return String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    ipAddress shr 8 and 0xff,
                    ipAddress shr 16 and 0xff,
                    ipAddress shr 24 and 0xff
                )
            }
        } catch (e: Exception) {}
        return "192.168.43.1" // fallback to most common hotspot default gateway
    }
}
