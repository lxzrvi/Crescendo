package com.lxzrvi.crescendo.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class PermissionKind { MUSIC, NOTIFICATIONS }

data class PermissionPage(
    val kind: PermissionKind,
    val title: String,
    val detail: String,
    val actionLabel: String,
)

/** The indicators reflect system state, not whether a request was merely made. */
data class PermissionStatus(
    val musicGranted: Boolean,
    val backgroundGranted: Boolean,
    val notificationsGranted: Boolean,
)

object PermissionPages {
    val all = listOf(
        PermissionPage(
            kind = PermissionKind.MUSIC,
            title = "Music Library",
            detail = "Allow Crescendo to read the music stored on your device so we can build your library. For steadier background playback, you can also allow unrestricted battery use.",
            actionLabel = "Allow Music Access",
        ),
        PermissionPage(
            kind = PermissionKind.NOTIFICATIONS,
            title = "Notifications",
            detail = "Allow notifications so you can control playback and see album art on your lock screen.",
            actionLabel = "Allow Notifications",
        ),
    )

    fun runtimePermissions(kind: PermissionKind): List<String> = when (kind) {
        PermissionKind.MUSIC ->
            if (Build.VERSION.SDK_INT >= 33) {
                listOf(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        PermissionKind.NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= 33) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            }
    }
}

fun hasMusicAccess(context: Context): Boolean =
    PermissionPages.runtimePermissions(PermissionKind.MUSIC).all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

fun readPermissionStatus(context: Context): PermissionStatus {
    val notificationPermission = PermissionPages.runtimePermissions(PermissionKind.NOTIFICATIONS)
        .all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return PermissionStatus(
        musicGranted = hasMusicAccess(context),
        backgroundGranted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true,
        notificationsGranted = notificationPermission &&
            NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )
}
