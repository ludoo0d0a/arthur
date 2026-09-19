package fr.geoking.arthur.source

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.StateFlow

/**
 * Gates remote still downloads for Ambient / phone UI.
 * When [RotationSettings.wifiOnlyRemoteStills] is on and the active network is not
 * unmetered, callers must stay cache-only (no download).
 *
 * Auto still rendering does not use this gate.
 */
class RemoteStillNetworkGate(
    context: Context,
    private val rotationSettings: RotationSettings,
) {
    private val appContext = context.applicationContext
    private val connectivity =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val wifiOnlyRemoteStills: StateFlow<Boolean> = rotationSettings.wifiOnlyRemoteStills

    /** True when a remote still download is allowed (setting off, or unmetered network). */
    fun canDownloadRemoteStill(): Boolean {
        if (!rotationSettings.wifiOnlyRemoteStills.value) return true
        return isUnmetered()
    }

    /** Cache-only: Wi‑Fi-only enabled and no unmetered network. */
    fun isCacheOnlyMode(): Boolean = !canDownloadRemoteStill()

    fun isUnmetered(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
