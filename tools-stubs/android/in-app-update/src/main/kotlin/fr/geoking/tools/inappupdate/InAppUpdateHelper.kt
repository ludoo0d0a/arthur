package fr.geoking.tools.inappupdate

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CheckFeedback {
    object None : CheckFeedback()
    object UpToDate : CheckFeedback()
    data class Error(val message: String, val title: String = "") : CheckFeedback()
}

data class UpdateNotificationSpec(
    val channelId: String,
    val channelName: String,
    val smallIcon: Int = 0,
    val title: String,
    val message: String,
    val launchActivityClass: Class<*>? = null,
)

class InAppUpdateHelper(
    private val context: Context,
    notificationSpec: UpdateNotificationSpec? = null,
) {
    private val _updateAvailable = MutableStateFlow<AppUpdateInfo?>(null)
    val updateAvailable: StateFlow<AppUpdateInfo?> = _updateAvailable.asStateFlow()

    private val _autoStartUpdate = MutableStateFlow(false)
    val autoStartUpdate: StateFlow<Boolean> = _autoStartUpdate.asStateFlow()

    private val _installStatus = MutableStateFlow(0)
    val installStatus: StateFlow<Int> = _installStatus.asStateFlow()

    private val _checkFeedback = MutableStateFlow<CheckFeedback>(CheckFeedback.None)
    val checkFeedback: StateFlow<CheckFeedback> = _checkFeedback.asStateFlow()

    fun consumeLaunchIntent(intent: Intent?) {}

    fun checkForUpdate(manual: Boolean = false, forceFeedback: Boolean = false) {
        if (manual || forceFeedback) {
            _checkFeedback.value = CheckFeedback.UpToDate
        }
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    fun resetCheckFeedback() {
        _checkFeedback.value = CheckFeedback.None
    }

    fun startUpdate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>) {}

    fun maybeAutoStartUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {}

    fun completeUpdate() {}

    fun unregister() {}
}
