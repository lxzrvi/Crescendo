package com.lxzrvi.crescendo.ui.onboarding

import android.Manifest
import android.os.Build

enum class PermissionKind { MUSIC, NOTIFICATIONS }

/**
 * Do hi pages: pehla me music + optional background playback,
 * doosra me notifications (skip-able).
 */
data class PermissionPage(
    val kind: PermissionKind,
    val title: String,
    val detail: String,
    val actionLabel: String,
)

object PermissionPages {

    val all = listOf(
        PermissionPage(
            kind = PermissionKind.MUSIC,
            title = "Music Library",
            detail = "Allow Crescendo to read the music stored on your device so we can build your library. You can also allow background playback so music keeps playing while the app is closed.",
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
                emptyList() // auto-granted below Android 13
            }
    }
}
