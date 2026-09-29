package com.example.data.wifi

import android.content.Context
import android.util.Log
import com.example.model.DiscoveredDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress

class LocalDeviceScanner(private val context: Context) {

    companion object {
        private const val TAG = "LocalDeviceScanner"
    }

    suspend fun scanLocalSubnet(
        currentDeviceIp: String,
        gatewayIp: String
    ): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<DiscoveredDevice>()
        val seenIps = mutableSetOf<String>()

        // 1. Add Gateway / Router
        val effectiveGateway = if (gatewayIp.isNotBlank()) gatewayIp else "192.168.0.1"
        devices.add(
            DiscoveredDevice(
                ip = effectiveGateway,
                hostname = "Gateway / Access Point",
                isGateway = true,
                isCurrentDevice = false,
                deviceType = "Wi-Fi Router / Gateway",
                isReachable = true
            )
        )
        seenIps.add(effectiveGateway)

        // 2. Add Current Device
        val effectiveCurrentIp = if (currentDeviceIp.isNotBlank()) currentDeviceIp else "192.168.0.108"
        if (!seenIps.contains(effectiveCurrentIp)) {
            devices.add(
                DiscoveredDevice(
                    ip = effectiveCurrentIp,
                    hostname = "localhost (This Device)",
                    isGateway = false,
                    isCurrentDevice = true,
                    deviceType = "This Android Device",
                    isReachable = true
                )
            )
            seenIps.add(effectiveCurrentIp)
        }

        // 3. Check ARP table if readable on this Android OS version
        try {
            val arpReader = BufferedReader(FileReader("/proc/net/arp"))
            arpReader.use { reader ->
                var line: String?
                // Skip header line
                reader.readLine()
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.split("\\s+".toRegex()) ?: continue
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val mac = tokens[3]
                        if (ip.isNotBlank() && ip != "0.0.0.0" && mac != "00:00:00:00:00:00" && !seenIps.contains(ip)) {
                            seenIps.add(ip)
                            val hostName = try {
                                InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
                            } catch (e: Exception) {
                                null
                            }
                            devices.add(
                                DiscoveredDevice(
                                    ip = ip,
                                    hostname = hostName ?: "Network Device",
                                    isGateway = (ip == effectiveGateway),
                                    isCurrentDevice = (ip == effectiveCurrentIp),
                                    deviceType = classifyDevice(hostName),
                                    isReachable = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "ARP table read restricted on this device: ${e.message}")
        }

        // 4. Concurrently probe nearby active subnet addresses
        val subnetPrefix = if (effectiveGateway.contains(".")) {
            effectiveGateway.substringBeforeLast(".") + "."
        } else {
            "192.168.0."
        }

        val ipsToProbe = (1..25).map { "$subnetPrefix$it" }
            .filter { !seenIps.contains(it) }

        coroutineScope {
            val probeJobs = ipsToProbe.map { testIp ->
                async {
                    try {
                        val inet = InetAddress.getByName(testIp)
                        val reachable = inet.isReachable(150)
                        if (reachable) {
                            val host = inet.canonicalHostName.takeIf { it != testIp }
                            DiscoveredDevice(
                                ip = testIp,
                                hostname = host ?: "Local Host ($testIp)",
                                isGateway = false,
                                isCurrentDevice = false,
                                deviceType = classifyDevice(host),
                                isReachable = true
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val probedResults = probeJobs.awaitAll().filterNotNull()
            devices.addAll(probedResults)
        }

        // Sort: Gateway first, Current device second, then by IP
        devices.sortedWith(
            compareByDescending<DiscoveredDevice> { it.isGateway }
                .thenByDescending { it.isCurrentDevice }
                .thenBy { it.ip }
        )
    }

    private fun classifyDevice(hostname: String?): String {
        if (hostname == null) return "Connected Host"
        val lower = hostname.lowercase()
        return when {
            lower.contains("printer") -> "Network Printer"
            lower.contains("tv") || lower.contains("cast") -> "Smart TV / Media"
            lower.contains("phone") || lower.contains("android") || lower.contains("iphone") -> "Mobile Device"
            lower.contains("pc") || lower.contains("desktop") || lower.contains("laptop") -> "Computer / Workstation"
            lower.contains("router") || lower.contains("gateway") -> "Router / AP"
            else -> "Authorized Device"
        }
    }
}
