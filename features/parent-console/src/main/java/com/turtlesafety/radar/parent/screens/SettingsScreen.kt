package com.turtlesafety.radar.parent.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.turtlesafety.radar.ai.LocalAiModeDescriptions
import com.turtlesafety.radar.core.settings.LocalAiMode
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.media.MediaScanResult
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import com.turtlesafety.radar.parent.format.RiskLabels

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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "検知感度", icon = Icons.Outlined.Tune) {
            Text(
                text = "リスクスコアに加算されるオフセットを変えます。誤検知が多ければ「低」、見逃しが気になるなら「高」を選んでください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sensitivity.entries.forEach { s ->
                    FilterChip(
                        selected = state.sensitivity == s,
                        onClick = { vm.setSensitivity(s) },
                        label = { Text(text = s.label()) },
                    )
                }
            }
        }

        SectionCard(title = "監視対象アプリ", icon = Icons.Outlined.Apps) {
            Text(
                text = "通知本文の検査対象とするアプリを管理します。初期値は LINE / Gmail / Messages / Discord / Instagram / X / TikTok です。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MonitoredAppsEditor(
                packages = state.monitoredApps,
                onAdd = vm::addMonitoredApp,
                onRemove = vm::removeMonitoredApp,
            )
        }

        SectionCard(title = "画像チェッカー", icon = Icons.Outlined.Image) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "新規スクリーンショットを自動検査",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "OFF にすると手動検査のみになります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.mediaCheckerEnabled,
                    onCheckedChange = vm::setMediaCheckerEnabled,
                )
            }

            Button(
                enabled = state.mediaCheckerEnabled && !state.mediaScanInProgress,
                onClick = { imagePicker.launch("image/*") },
            ) {
                Text(if (state.mediaScanInProgress) "検査中..." else "画像を選んで手動検査")
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

        SectionCard(title = "Local AI モード", icon = Icons.Outlined.AutoAwesome) {
            val description = LocalAiModeDescriptions.describe(state.localAiMode)
            Text(
                text = description.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalAiMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.localAiMode == mode,
                        onClick = { vm.setLocalAiMode(mode) },
                        label = { Text(LocalAiModeDescriptions.describe(mode).title) },
                    )
                }
            }
            Text(
                text = description.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "データ保持", icon = Icons.Outlined.Schedule) {
            Text(
                text = "ログの保持期間 (仕様 §11.3 に準拠)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            RetentionRow("高リスク", "90 日")
            RetentionRow("中リスク", "30 日")
            RetentionRow("低リスク", "7 日")
            Text(
                text = "起動時に保持期間を超えたログは自動削除されます。手動の全削除は「ログ」画面から行えます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "プライバシー方針", icon = Icons.Outlined.PrivacyTip) {
            BulletText("入力全文 / チャット全文は保存しません")
            BulletText("パスワード・認証コードは検査・保存しません")
            BulletText("画像そのものは保存しません (検査結果のみ)")
            BulletText("高リスクのみ最小限の抜粋を端末内ログに残します")
            BulletText("ログ・設定は端末外に送信しません")
        }

        AboutSection()

        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = remember(context) { resolveVersionName(context) }
    SectionCard(title = "このアプリについて", icon = Icons.Outlined.Info) {
        Row {
            Text(
                text = "アプリ名",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Turtle Safety Radar",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row {
            Text(
                text = "バージョン",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = versionName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row {
            Text(
                text = "対応仕様",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "v0.4",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "家庭内での見守り利用を想定したサイドロード版です。Google Play 配布版とは別の運用ポリシーで動作します。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resolveVersionName(context: Context): String {
    val pm = context.packageManager
    return runCatching {
        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(context.packageName, 0)
        val name = info.versionName ?: "—"
        val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION") info.versionCode.toLong()
        }
        "$name (${code})"
    }.getOrDefault("不明")
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            content()
        }
    }
}

@Composable
private fun MonitoredAppsEditor(
    packages: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var newPackage by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val sorted = remember(packages) { packages.sorted() }
        if (sorted.isEmpty()) {
            Text(
                text = "監視対象アプリは現在ありません。下のフィールドからパッケージ名を追加してください。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            sorted.forEach { pkg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayNameFor(pkg),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = pkg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(pkg) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "$pkg を監視対象から外す",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider()
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newPackage,
                onValueChange = { newPackage = it.trim() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("パッケージ名を追加 (例: com.example.app)") },
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = newPackage.isNotBlank(),
                onClick = {
                    onAdd(newPackage)
                    newPackage = ""
                },
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("追加")
            }
        }
    }
}

private fun displayNameFor(packageName: String): String = when (packageName) {
    "jp.naver.line.android" -> "LINE"
    "com.google.android.gm" -> "Gmail"
    "com.google.android.apps.messaging" -> "Google Messages"
    "com.discord" -> "Discord"
    "com.instagram.android" -> "Instagram"
    "com.twitter.android" -> "X (旧 Twitter)"
    "com.zhiliaoapp.musically" -> "TikTok"
    else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}

@Composable
private fun RetentionRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BulletText(text: String) {
    Row {
        Text(text = "・", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
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
            text = "${RiskLabels.levelLabel(result.score)} (スコア ${result.score}) · ${RiskLabels.sourceLabel(result.source)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = result.warningMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                text = "OCR 抜粋: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Sensitivity.label(): String = when (this) {
    Sensitivity.LOW -> "低 (-1)"
    Sensitivity.NORMAL -> "標準"
    Sensitivity.HIGH -> "高 (+1)"
}
