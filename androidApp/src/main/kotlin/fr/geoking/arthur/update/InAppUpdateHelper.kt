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

sealed class CheckFeedback {
    data object None : CheckFeedback()
    data object UpToDate : CheckFeedback()
    data class Error(val message: String) : CheckFeedback()
}

/**
 * Play In-App Updates helper (flexible). Emits [updateAvailable] for a dialog;
 * auto-[completeUpdate] when a flexible download finishes.
 * Manual checks (Settings) can re-prompt and emit [checkFeedback] when up to date or on error.
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

    private val _checkFeedback = MutableStateFlow<CheckFeedback>(CheckFeedback.None)
    val checkFeedback: StateFlow<CheckFeedback> = _checkFeedback.asStateFlow()

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

    /**
     * @param manual When true (Settings), ignores session dismiss, allows re-prompt,
     * and emits [checkFeedback] when already up to date or on error.
     */
    fun checkForUpdate(manual: Boolean = false) {
        if (!manual) {
            if (isUpdateDismissed) return
            if (_updateAvailable.value != null) return
        } else {
            _checkFeedback.value = CheckFeedback.None
            isUpdateDismissed = false
        }

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
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
                    return@addOnSuccessListener
                }

                if (manual) {
                    _checkFeedback.value = CheckFeedback.UpToDate
                }
            }
            .addOnFailureListener { error ->
                if (manual) {
                    _checkFeedback.value = CheckFeedback.Error(
                        error.message ?: "Unknown error",
                    )
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

    fun resetCheckFeedback() {
        _checkFeedback.value = CheckFeedback.None
    }
}
