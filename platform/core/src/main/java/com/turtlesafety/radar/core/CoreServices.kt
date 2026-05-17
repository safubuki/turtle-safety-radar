package com.turtlesafety.radar.core

import android.content.Context
import com.turtlesafety.radar.core.db.RadarDatabase
import com.turtlesafety.radar.core.log.DetectionLogDao
import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.log.LogRetentionEnforcer
import com.turtlesafety.radar.core.risk.LocalAiAwareRiskEngine
import com.turtlesafety.radar.core.risk.RiskEngine
import com.turtlesafety.radar.core.risk.RuleBasedRiskEngine
import com.turtlesafety.radar.core.risk.SensitivityAwareRiskEngine
import com.turtlesafety.radar.core.security.PinManager
import com.turtlesafety.radar.core.settings.SettingsStore

/**
 * Core 層のサービス一式を 1 つに束ねる軽量 ServiceLocator。
 *
 * MVP では Hilt 等の DI を導入せず、`Application` 起動時に [Radar.init] を呼んで
 * 全モジュールから [Radar.services] 経由でアクセスする方針。
 */
class CoreServices internal constructor(applicationContext: Context) {
    val database: RadarDatabase = RadarDatabase.get(applicationContext)
    val detectionLogDao: DetectionLogDao = database.detectionLogDao()
    val detectionLogRepository: DetectionLogRepository =
        DetectionLogRepository(detectionLogDao)
    val retentionEnforcer: LogRetentionEnforcer =
        LogRetentionEnforcer(detectionLogDao)
    val pinManager: PinManager = PinManager(applicationContext)
    val settingsStore: SettingsStore = SettingsStore(applicationContext)
    val riskEngine: RiskEngine = SensitivityAwareRiskEngine(
        delegate = LocalAiAwareRiskEngine(
            delegate = RuleBasedRiskEngine(),
            currentMode = { settingsStore.localAiMode },
        ),
        currentSensitivity = { settingsStore.sensitivity },
    )
}

/**
 * アプリケーションスコープの ServiceLocator。
 * `Application.onCreate` で [init] を 1 回呼ぶ。
 */
object Radar {
    @Volatile private var instance: CoreServices? = null

    fun init(context: Context) {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    instance = CoreServices(context.applicationContext)
                }
            }
        }
    }

    fun services(): CoreServices =
        instance ?: error("Radar.init(context) must be called from Application.onCreate")
}
