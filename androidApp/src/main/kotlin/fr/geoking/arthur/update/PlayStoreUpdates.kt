package fr.geoking.arthur.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Play In-App Updates (AppUpdateManager) are **not** supported on Android TV.
 * Always-on ambient / Dream also blocks silent Play auto-install while the app
 * stays in the foreground — open the store listing so the user can update.
 */
object PlayStoreUpdates {
    fun openAppListing(context: Context): Boolean {
        val packageName = context.packageName
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(market)
            true
        } catch (_: ActivityNotFoundException) {
            runCatching {
                context.startActivity(web)
                true
            }.getOrDefault(false)
        }
    }
}
