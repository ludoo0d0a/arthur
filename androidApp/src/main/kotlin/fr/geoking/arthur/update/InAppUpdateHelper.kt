package fr.geoking.arthur.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play In-App Updates helper (flexible). Emits [updateAvailable] for a dialog;
 * auto-[completeUpdate] when a flexible download finishes.
 */
class InAppUpdateHelper(
    context: Context,
) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    private var isUpdateDismissed = false

    private val _updateAvailable = MutableStateFlow<AppUpdateInfo?>(null)
    val updateAvailable: StateFlow<AppUpdateInfo?> = _updateAvailable.asStateFlow()

    private val _installStatus = MutableStateFlow(InstallStatus.UNKNOWN)
    val installStatus: StateFlow<Int> = _installStatus.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { state ->
        _installStatus.value = state.installStatus()
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            completeUpdate()
        }
    }

    init {
        appUpdateManager.registerListener(installStateListener)
    }

    fun unregister() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    fun checkForUpdate() {
        if (isUpdateDismissed) return
        if (_updateAvailable.value != null) return

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            _installStatus.value = appUpdateInfo.installStatus()

            val inProgress = appUpdateInfo.installStatus() == InstallStatus.PENDING ||
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADING ||
                appUpdateInfo.installStatus() == InstallStatus.INSTALLING
            if (inProgress) return@addOnSuccessListener

            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                completeUpdate()
                return@addOnSuccessListener
            }

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                _updateAvailable.value = appUpdateInfo
            }
        }
    }

    fun startUpdate(
        appUpdateInfo: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        isUpdateDismissed = true
        val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, launcher, options)
        _updateAvailable.value = null
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun dismissUpdate() {
        isUpdateDismissed = true
        _updateAvailable.value = null
    }
}
