package com.turtlesafety.radar

import android.app.Application
import com.turtlesafety.radar.core.Radar
import com.turtlesafety.radar.parent.system.PermissionStateMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RadarApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Radar.init(this)

        // 起動時にログのリテンションを一度適用 (仕様書 §11.3)。
        applicationScope.launch {
            runCatching { Radar.services().retentionEnforcer.enforce() }
        }

        // 起動時に権限状態を診断 — 前回からの取り消しがあれば SYSTEM ログに残す (§9)。
        applicationScope.launch {
            runCatching {
                PermissionStateMonitor(
                    context = this@RadarApplication,
                    repository = Radar.services().detectionLogRepository,
                ).snapshotAndReport()
            }
        }

        // 高リスクログと権限無効化ログを保護者向けの端末通知へ変換する。
        applicationScope.launch {
            runCatching {
                ParentAlertNotifier(
                    context = this@RadarApplication,
                    repository = Radar.services().detectionLogRepository,
                ).run()
            }
        }
    }
}
