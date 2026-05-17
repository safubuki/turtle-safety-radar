package com.turtlesafety.radar.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.turtlesafety.radar.core.Radar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 新規に MediaStore に追加された画像(主にスクリーンショット)を自動検査するサービス。
 *
 * 仕様書 §5.4 を「親操作前提」から「裏側で自動」へ作り直すコア。
 * - MediaStore の Images コレクションを ContentObserver で監視
 * - 変更検知のたびに、直前に観測した最大 id 以降の新規行を列挙
 * - 新規画像ごとに [MediaChecker] でリスク判定
 * - 高リスクのみ MediaChecker 側で DetectionLog に保存
 */
class MediaWatcherService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: SharedPreferences
    private var observer: ContentObserver? = null
    private var mediaChecker: MediaChecker? = null

    override fun onCreate() {
        super.onCreate()
        prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        runCatching {
            val services = Radar.services()
            mediaChecker = MediaChecker(
                context = applicationContext,
                riskEngine = services.riskEngine,
                repository = services.detectionLogRepository,
            )
        }.onFailure { Log.e(TAG, "Radar.services() failed", it) }

        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startObserving()
    }

    private fun startObserving() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val handler = Handler(Looper.getMainLooper())
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, changeUri: Uri?) {
                scope.launch { processNewImages() }
            }
        }
        runCatching {
            contentResolver.registerContentObserver(uri, true, obs)
            observer = obs
        }.onFailure { Log.e(TAG, "registerContentObserver failed", it) }
    }

    private suspend fun processNewImages() {
        val checker = mediaChecker ?: return
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val lastSeenId = prefs.getLong(KEY_LAST_ID, 0L)

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Images.Media._ID} > ?"
        val args = arrayOf(lastSeenId.toString())
        val sort = "${MediaStore.Images.Media._ID} ASC"

        val candidates = mutableListOf<Uri>()
        var newestId = lastSeenId
        runCatching {
            contentResolver.query(collection, projection, selection, args, sort)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val pathIdx = c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                val nameIdx = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    val path = if (pathIdx >= 0) c.getString(pathIdx).orEmpty() else ""
                    val name = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                    newestId = maxOf(newestId, id)
                    if (looksLikeScreenshot(path, name)) {
                        candidates += ContentUris.withAppendedId(collection, id)
                    }
                }
            }
        }.onFailure { Log.e(TAG, "MediaStore query failed", it) }

        if (newestId != lastSeenId) {
            prefs.edit().putLong(KEY_LAST_ID, newestId).apply()
        }

        for (uri in candidates) {
            runCatching { checker.analyze(uri) }
                .onFailure { Log.w(TAG, "analyze failed for $uri", it) }
        }
    }

    private fun looksLikeScreenshot(relativePath: String, displayName: String): Boolean {
        val p = relativePath.lowercase()
        val n = displayName.lowercase()
        return p.contains("screenshot") || n.contains("screenshot") ||
            n.startsWith("screenshot_") || p.contains("pictures/screenshots")
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("見守り中: スクリーンショット監視")
            .setContentText("新しいスクリーンショットを自動検査しています")
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Media Watcher",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = "スクリーンショット自動検査の常駐通知"
        }
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observer?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        observer = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MediaWatcherService"
        private const val CHANNEL_ID = "media_watcher"
        private const val NOTIFICATION_ID = 2001
        private const val PREFS = "radar_media_watcher"
        private const val KEY_LAST_ID = "last_seen_id"

        fun start(context: Context) {
            val intent = Intent(context, MediaWatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
