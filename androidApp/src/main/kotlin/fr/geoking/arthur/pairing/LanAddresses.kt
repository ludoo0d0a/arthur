package fr.geoking.arthur.pairing

import java.net.Inet4Address
import java.net.NetworkInterface

/** Local IPv4 addresses suitable for LAN pairing (excludes loopback / link-local unless fallback required). */
fun lanIpv4Addresses(): List<String> {
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull()
        ?.let { runCatching { it.toList() }.getOrNull() }
        ?: return listOf("127.0.0.1")

    val allAddresses = mutableListOf<Inet4Address>()
    for (iface in interfaces) {
        val addrs = runCatching { iface.inetAddresses.toList() }.getOrNull() ?: continue
        for (address in addrs) {
            if (address is Inet4Address) {
                allAddresses += address
            }
        }
    }

    // Pass 1: Strict LAN IPv4 — non-loopback, non-link-local
    val strict = allAddresses.filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
        .mapNotNull { it.hostAddress }
        .distinct()
    if (strict.isNotEmpty()) return strict

    // Pass 2: Fallback to non-loopback (e.g. link-local / virtual / emulator interfaces)
    val nonLoopback = allAddresses.filter { !it.isLoopbackAddress }
        .mapNotNull { it.hostAddress }
        .distinct()
    if (nonLoopback.isNotEmpty()) return nonLoopback

    // Pass 3: Fallback to loopback if no other interface exists
    val loopback = allAddresses.mapNotNull { it.hostAddress }.distinct()
    if (loopback.isNotEmpty()) return loopback

    return listOf("127.0.0.1")
}

fun preferredLanIpv4(): String = lanIpv4Addresses().first()
