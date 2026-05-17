package com.turtlesafety.radar.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Media Checker(スクリーンショット自動検査)に必要な権限の確認。
 *
 * - Android 13+ : `READ_MEDIA_IMAGES`
 * - Android 12 以下: `READ_EXTERNAL_STORAGE`
 */
object MediaCheckerPermission {

    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, requiredPermission()) ==
            PackageManager.PERMISSION_GRANTED
}
