package com.turtlesafety.radar

import android.app.Application
import com.turtlesafety.radar.core.Radar
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
    }
}
