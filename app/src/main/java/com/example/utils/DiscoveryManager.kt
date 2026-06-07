package com.example.utils

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

object DiscoveryManager {
    private const val DISCOVERY_PORT = 8888
    private const val DISCOVERY_MSG = "SYNCBEAT_HOST_BROADCAST"
    
    private var hostJob: Job? = null
    private var clientJob: Job? = null
    
    private val _discoveredHostIp = MutableStateFlow<String?>(null)
    val discoveredHostIp: StateFlow<String?> = _discoveredHostIp

    private fun getBroadcastAddresses(context: Context): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (interfaceAddress in intf.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        addresses.add(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
                val quads = ByteArray(4)
                for (k in 0..3) {
                    quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
                }
                val inetAddr = InetAddress.getByAddress(quads)
                if (!addresses.contains(inetAddr)) {
                    addresses.add(inetAddr)
                }
            }
        } catch (e: Exception) {}
        
        if (addresses.isEmpty()) {
            addresses.add(InetAddress.getByName("255.255.255.255"))
        }
        return addresses
    }

    fun startHosting(context: Context) {
        stopHosting()
        hostJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val sendData = DISCOVERY_MSG.toByteArray()
                
                while (isActive) {
                    try {
                        val addresses = getBroadcastAddresses(context)
                        for (address in addresses) {
                            try {
                                val packet = DatagramPacket(sendData, sendData.size, address, DISCOVERY_PORT)
                                socket.send(packet)
                            } catch (e: Exception) {}
                        }
                    } catch (e: Exception) {}
                    delay(2000) // broadcast every 2 seconds
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun stopHosting() {
        hostJob?.cancel()
        hostJob = null
    }

    fun startDiscovering() {
        stopDiscovering()
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(DISCOVERY_PORT)
                socket.soTimeout = 3000
                socket.broadcast = true
                val recvBuf = ByteArray(1024)
                
                while (isActive) {
                    try {
                        val packet = DatagramPacket(recvBuf, recvBuf.size)
                        socket.receive(packet)
                        val message = String(packet.data, 0, packet.length)
                        if (message == DISCOVERY_MSG) {
                            val hostIp = packet.address.hostAddress
                            _discoveredHostIp.value = hostIp
                        }
                    } catch (e: SocketTimeoutException) {
                        // Expected timeout if no broadcast received
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                socket?.close()
            }
        }
    }

    fun stopDiscovering() {
        clientJob?.cancel()
        clientJob = null
        _discoveredHostIp.value = null
    }
}
