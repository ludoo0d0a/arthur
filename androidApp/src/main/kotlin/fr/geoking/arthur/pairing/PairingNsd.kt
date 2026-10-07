package fr.geoking.arthur.pairing

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/** NSD service type for Arthur LAN pairing (DNS-SD). */
const val PAIRING_NSD_SERVICE_TYPE = "_arthur-pairing._tcp."

data class DiscoveredPairingTv(
    val serviceName: String,
    val host: String,
    val port: Int,
)

/** Phone-side discovery of Arthur TVs (NSD or test doubles). */
interface PairingTvDiscovery {
    val tvs: StateFlow<List<DiscoveredPairingTv>>
    fun start()
    fun stop()
}

/** TV-side NSD advertisement for [LanPairingServer]. */
class PairingNsdAdvertiser(
    context: Context,
    private val port: Int = LanPairingServer.DEFAULT_PORT,
    private val deviceName: String = "Arthur TV",
) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registered = false
    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            registered = true
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            registered = false
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            registered = false
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
    }

    fun start() {
        val info = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = PAIRING_NSD_SERVICE_TYPE
            setPort(port)
        }
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener) }
    }

    fun stop() {
        if (!registered) {
            runCatching { nsd.unregisterService(registrationListener) }
            return
        }
        runCatching { nsd.unregisterService(registrationListener) }
        registered = false
    }
}

/** Phone-side NSD browser for Arthur TVs on the LAN. */
class PairingNsdBrowser(context: Context) : PairingTvDiscovery {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val found = ConcurrentHashMap<String, DiscoveredPairingTv>()
    private val _tvs = MutableStateFlow<List<DiscoveredPairingTv>>(emptyList())
    override val tvs: StateFlow<List<DiscoveredPairingTv>> = _tvs.asStateFlow()

    private var discovering = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            discovering = true
        }

        override fun onDiscoveryStopped(serviceType: String) {
            discovering = false
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            discovering = false
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            discovering = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (!serviceInfo.serviceType.contains("arthur-pairing")) return
            resolve(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            found.remove(serviceInfo.serviceName)
            publish()
        }
    }

    override fun start() {
        if (discovering) return
        found.clear()
        publish()
        runCatching {
            nsd.discoverServices(PAIRING_NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }
    }

    override fun stop() {
        runCatching { nsd.stopServiceDiscovery(discoveryListener) }
        discovering = false
        found.clear()
        publish()
    }

    @Suppress("DEPRECATION")
    private fun resolve(serviceInfo: NsdServiceInfo) {
        runCatching {
            nsd.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        val port = resolved.port.takeIf { it > 0 } ?: LanPairingServer.DEFAULT_PORT
                        found[resolved.serviceName] = DiscoveredPairingTv(
                            serviceName = resolved.serviceName,
                            host = host,
                            port = port,
                        )
                        publish()
                    }
                },
            )
        }
    }

    private fun publish() {
        _tvs.value = found.values.sortedBy { it.serviceName }
    }
}
