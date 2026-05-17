package com.turtlesafety.radar

import android.app.Application
import android.util.Log
import com.turtlesafety.radar.core.Radar
import com.turtlesafety.radar.media.MediaCheckerPermission
import com.turtlesafety.radar.media.MediaWatcherService
import com.turtlesafety.radar.parent.system.PermissionStateMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RadarApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // 起動時クラッシュをユーザーに見える形で残す。
        CrashRecorder.install(this)

        val initSucceeded = runCatching { Radar.init(this) }
            .onFailure {
                Log.e(TAG, "Radar.init failed", it)
                CrashRecorder.record(this, it)
            }
            .isSuccess

        if (!initSucceeded) {
            // Core が初期化できない場合は後段の起動処理を一切走らせない。
            // UI 側 (Parent Console) はサービス未初期化を検出して案内する。
            return
        }

        // 起動時にログのリテンションを一度適用 (仕様書 §11.3)。
        applicationScope.launch {
            runCatching { Radar.services().retentionEnforcer.enforce() }
                .onFailure { Log.w(TAG, "retention enforce failed", it) }
        }

        // 起動時に権限状態を診断 — 前回からの取り消しがあれば SYSTEM ログに残す (§9)。
        applicationScope.launch {
            runCatching {
                PermissionStateMonitor(
                    context = this@RadarApplication,
                    repository = Radar.services().detectionLogRepository,
                ).snapshotAndReport()
            }.onFailure { Log.w(TAG, "permission snapshot failed", it) }
        }

        // 高リスクログと権限無効化ログを保護者向けの端末通知へ変換する。
        applicationScope.launch {
            runCatching {
                ParentAlertNotifier(
                    context = this@RadarApplication,
                    repository = Radar.services().detectionLogRepository,
                ).run()
            }.onFailure { Log.w(TAG, "parent alert notifier failed", it) }
        }

        // Media Checker 設定 ON かつ画像読取権限ありの時、スクリーンショット自動監視を起動。
        runCatching {
            if (Radar.services().settingsStore.mediaCheckerEnabled &&
                MediaCheckerPermission.isGranted(this)
            ) {
                MediaWatcherService.start(this)
            }
        }.onFailure { Log.w(TAG, "start MediaWatcherService failed", it) }
    }

    companion object {
        private const val TAG = "RadarApplication"
    }
}
