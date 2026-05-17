package com.turtlesafety.radar.parent.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.ai.LocalAiModeDescriptions
import com.turtlesafety.radar.core.settings.LocalAiMode
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.core.settings.SettingsStore
import com.turtlesafety.radar.media.MediaScanResult
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel

@Composable
fun SettingsScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let(vm::runMediaCheck)
    }

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
        MonitoredAppsSection(
            state = state,
            onToggle = vm::toggleMonitored,
            onAddCustom = vm::addMonitoredApp,
            onRemoveCustom = vm::removeMonitoredApp,
        )
        MediaCheckerSection(
            state = state,
            onToggle = vm::setMediaCheckerEnabled,
            onAnalyze = { imagePicker.launch("image/*") },
        )
        LocalAiSection(
            currentMode = state.localAiMode,
            onSet = vm::setLocalAiMode,
        )
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
    onAddCustom: (String) -> Unit,
    onRemoveCustom: (String) -> Unit,
) {
    var customPackage by remember { mutableStateOf("") }
    val customPackages = state.monitoredApps
        .filterNot { it in SettingsStore.DEFAULT_MONITORED_APPS }
        .sorted()

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

            Text(
                text = "カスタム追加",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "既定一覧にないアプリはパッケージ名で追加できます。",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = customPackage,
                    onValueChange = { customPackage = it },
                    label = { Text("例: com.example.chat") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        onAddCustom(customPackage)
                        customPackage = ""
                    },
                ) {
                    Text("追加")
                }
            }

            if (customPackages.isNotEmpty()) {
                customPackages.forEach { pkg ->
                    CustomMonitoredAppRow(
                        packageName = pkg,
                        onRemove = { onRemoveCustom(pkg) },
                    )
                }
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

@Composable
private fun CustomMonitoredAppRow(packageName: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = packageName, fontWeight = FontWeight.Bold)
            Text(
                text = "カスタム監視対象",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(onClick = onRemove) {
            Text("削除")
        }
    }
}

@Composable
private fun MediaCheckerSection(
    state: ParentConsoleState,
    onToggle: (Boolean) -> Unit,
    onAnalyze: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Media Checker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "画像・スクリーンショットから QR や連絡先誘導を検査します。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = state.mediaCheckerEnabled, onCheckedChange = onToggle)
            }

            Button(
                enabled = state.mediaCheckerEnabled && !state.mediaScanInProgress,
                onClick = onAnalyze,
            ) {
                Text(if (state.mediaScanInProgress) "検査中..." else "画像を選んで検査")
            }

            if (state.mediaScanInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.mediaScanError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            state.lastMediaScanResult?.let { result ->
                MediaScanSummary(result)
            }
        }
    }
}

@Composable
private fun MediaScanSummary(result: MediaScanResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "直近の検査結果",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "score ${result.score} / source ${result.source.name}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = result.warningMessage,
            style = MaterialTheme.typography.bodySmall,
        )
        if (result.signals.isNotEmpty()) {
            Text(
                text = "検出: ${result.signals.joinToString(separator = " / ") { it.label }}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (result.qrPayloads.isNotEmpty()) {
            Text(
                text = "QR: ${result.qrPayloads.size} 件",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        result.extractedTextPreview?.let {
            Text(
                text = "OCR抜粋: $it",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LocalAiSection(
    currentMode: LocalAiMode,
    onSet: (LocalAiMode) -> Unit,
) {
    val description = LocalAiModeDescriptions.describe(currentMode)

    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Local AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = description.summary,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalAiMode.entries.forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { onSet(mode) },
                        label = { Text(LocalAiModeDescriptions.describe(mode).title) },
                    )
                }
            }
            Text(
                text = description.note,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
