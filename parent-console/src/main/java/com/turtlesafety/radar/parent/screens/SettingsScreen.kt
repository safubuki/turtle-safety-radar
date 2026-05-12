package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.core.settings.SettingsStore
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel

@Composable
fun SettingsScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "設定",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        SensitivitySection(state = state, onSet = vm::setSensitivity)
        MonitoredAppsSection(state = state, onToggle = vm::toggleMonitored)
    }
}

@Composable
private fun SensitivitySection(state: ParentConsoleState, onSet: (Sensitivity) -> Unit) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "検知感度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "リスクスコアにこのオフセットを加算します。",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sensitivity.entries.forEach { s ->
                    FilterChip(
                        selected = state.sensitivity == s,
                        onClick = { onSet(s) },
                        label = { Text(text = s.label()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitoredAppsSection(
    state: ParentConsoleState,
    onToggle: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "監視対象アプリ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "通知が届いたときにリスク判定を行う対象パッケージ。",
                style = MaterialTheme.typography.bodySmall,
            )
            SettingsStore.DEFAULT_MONITORED_APPS.forEach { pkg ->
                MonitoredAppRow(
                    packageName = pkg,
                    enabled = pkg in state.monitoredApps,
                    onToggle = { onToggle(pkg) },
                )
            }
        }
    }
}

@Composable
private fun MonitoredAppRow(packageName: String, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friendlyName(packageName), fontWeight = FontWeight.Bold)
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

private fun friendlyName(packageName: String): String = when (packageName) {
    "jp.naver.line.android" -> "LINE"
    "com.google.android.gm" -> "Gmail"
    "com.google.android.apps.messaging" -> "Messages (SMS)"
    "com.discord" -> "Discord"
    "com.instagram.android" -> "Instagram"
    "com.twitter.android" -> "X (Twitter)"
    "com.zhiliaoapp.musically" -> "TikTok"
    else -> packageName
}

private fun Sensitivity.label(): String = when (this) {
    Sensitivity.LOW -> "低 (-1)"
    Sensitivity.NORMAL -> "標準"
    Sensitivity.HIGH -> "高 (+1)"
}
