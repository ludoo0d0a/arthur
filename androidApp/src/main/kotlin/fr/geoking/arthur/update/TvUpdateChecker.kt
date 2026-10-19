package fr.geoking.arthur.update

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Soft availability probe for Android TV. Does not start a Play Core update flow
 * (unsupported on TV). When [available] is true, the host should open the Play
 * Store listing via [PlayStoreUpdates].
 */
class TvUpdateChecker(
    context: Context,
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)

    private var dismissed = false

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()

    fun check() {
        if (dismissed) return
        runCatching {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (dismissed) return@addOnSuccessListener
                    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        _available.value = true
                    }
                }
        }
    }

    fun dismiss() {
        dismissed = true
        _available.value = false
    }
}
