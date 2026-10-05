package fr.geoking.arthur.pairing

import java.net.Inet4Address
import java.net.NetworkInterface

/** Local IPv4 addresses suitable for LAN pairing (excludes loopback / link-local). */
fun lanIpv4Addresses(): List<String> {
    val result = mutableListOf<String>()
    val interfaces = runCatching { NetworkInterface.getNetworkInterfaces() }.getOrNull() ?: return emptyList()
    for (iface in interfaces) {
        if (!iface.isUp || iface.isLoopback) continue
        for (address in iface.inetAddresses) {
            if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                result += address.hostAddress ?: continue
            }
        }
    }
    return result.distinct()
}

fun preferredLanIpv4(): String? = lanIpv4Addresses().firstOrNull()
