package com.turtlesafety.radar.parent.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import com.turtlesafety.radar.parent.format.RiskLabels
import com.turtlesafety.radar.parent.system.ParentNotificationPermission

@Composable
fun HomeScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val requestNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        vm.refreshPermissions(context)
    }
    val shouldRequestParentNotifications =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverallStatusCard(
            state = state,
            onRefresh = { vm.refreshPermissions(context) },
        )

        SetupProgressCard(state = state)

        SectionHeader("見守りモジュール")

        StatusCard(
            title = "入力監視",
            description = if (state.accessibilityEnabled)
                "どのキーボードでも入力テキストをリスク判定します"
            else
                "ユーザー補助サービスで「Turtle Safety Radar 入力監視」を有効にしてください",
            ok = state.accessibilityEnabled,
            icon = Icons.Outlined.AccessibilityNew,
            actionLabel = "ユーザー補助設定を開く",
            onAction = { vm.openAccessibilitySettings(context) },
        )

        StatusCard(
            title = "通知監視",
            description = if (state.notificationListenerEnabled)
                "対象アプリの通知本文をリスク判定します"
            else
                "通知へのアクセスを許可してください",
            ok = state.notificationListenerEnabled,
            icon = if (state.notificationListenerEnabled)
                Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
            actionLabel = "通知アクセス設定を開く",
            onAction = { vm.openNotificationListenerSettings(context) },
        )

        StatusCard(
            title = "保護者通知",
            description = when {
                state.parentNotificationsEnabled -> "高リスク検知時に端末通知を受信できます"
                shouldRequestParentNotifications -> "高リスク通知を受けるには通知権限を許可してください"
                else -> "アプリ通知を有効にしてください"
            },
            ok = state.parentNotificationsEnabled,
            icon = Icons.Outlined.NotificationsActive,
            actionLabel = if (shouldRequestParentNotifications) "通知を許可" else "通知設定を開く",
            onAction = {
                if (shouldRequestParentNotifications && ParentNotificationPermission.needsRuntimePermission(context)) {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.openParentNotificationSettings(context)
                }
            },
        )

        StatusCard(
            title = "画像チェッカー",
            description = if (state.mediaCheckerEnabled)
                "新規スクリーンショットを自動検査します"
            else
                "画像検査は無効です。設定から有効化できます",
            ok = state.mediaCheckerEnabled,
            icon = Icons.Outlined.Image,
            actionLabel = null,
            onAction = null,
        )

        StatusCard(
            title = "Safety IME (動作確認用)",
            description = when {
                state.safetyImeIsDefault -> "デフォルト入力方式: 子どもには非推奨。通常 IME に戻してください"
                state.safetyImeEnabled -> "有効化済み (送信前チェック用)"
                else -> "未有効。動作確認したい場合のみ有効化してください"
            },
            ok = state.safetyImeEnabled && !state.safetyImeIsDefault,
            icon = Icons.Outlined.Keyboard,
            actionLabel = if (state.safetyImeEnabled) "入力方式ピッカーを表示" else "入力方式設定を開く",
            onAction = {
                if (state.safetyImeEnabled) vm.showImePicker(context)
                else vm.openImeSettings(context)
            },
        )

        SectionHeader("最近の状況")
        SummaryCard(state = state)

        DeveloperToolsCard(
            state = state,
            onRunSelfTest = vm::runSelfTest,
            onClearSelfTest = vm::clearSelfTest,
            onRunQuickCheck = vm::previewQuickCheck,
            onClearQuickCheck = vm::clearQuickCheck,
        )

        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun OverallStatusCard(
    state: ParentConsoleState,
    onRefresh: () -> Unit,
) {
    val allCriticalReady =
        state.accessibilityEnabled && state.notificationListenerEnabled && state.parentNotificationsEnabled
    val tint = if (allCriticalReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val icon = if (allCriticalReady) Icons.Outlined.CheckCircle else Icons.Outlined.Warning
    val title = if (allCriticalReady) "見守りは正常に動作中です" else "セットアップを完了させてください"
    val description = if (allCriticalReady)
        "対象アプリの通知と入力をリスク判定し、高リスク時のみ最小限の証跡をログに残します。"
    else
        "未設定の権限があります。下のセットアップ進捗から順に設定してください。"

    Card(
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("権限状態を再確認")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun StatusCard(
    title: String,
    description: String,
    ok: Boolean,
    icon: ImageVector,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val accent = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(colors = CardDefaults.cardColors()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ok) "有効" else "未有効",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text(text = actionLabel)
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupProgressCard(state: ParentConsoleState) {
    val completedSteps = listOf(
        state.accessibilityEnabled,
        state.notificationListenerEnabled,
        state.parentNotificationsEnabled,
    ).count { it }
    val totalSteps = 3
    val nextAction = when {
        !state.accessibilityEnabled -> "1. 入力監視 (ユーザー補助) を有効にしてください"
        !state.notificationListenerEnabled -> "2. 通知アクセスを許可してください"
        !state.parentNotificationsEnabled -> "3. 保護者通知を許可してください"
        else -> "セットアップは完了しています"
    }
    val isComplete = completedSteps == totalSteps

    Card(colors = CardDefaults.cardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "初期セットアップ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$completedSteps / $totalSteps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isComplete) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { completedSteps.toFloat() / totalSteps.toFloat() },
            )
            Text(
                text = nextAction,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryCard(state: ParentConsoleState) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "検知ログ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "保存中: ${state.totalLogCount} 件 / 未確認: ${state.unacknowledgedLogCount} 件",
                style = MaterialTheme.typography.bodyMedium,
            )
            state.lastMediaScanResult?.let {
                Text(
                    text = "直近の画像検査: ${RiskLabels.levelLabel(it.score)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DeveloperToolsCard(
    state: ParentConsoleState,
    onRunSelfTest: () -> Unit,
    onClearSelfTest: () -> Unit,
    onRunQuickCheck: (String) -> Unit,
    onClearQuickCheck: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var quickCheckText by rememberSaveable { mutableStateOf("") }

    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Build, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "開発者向け診断",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "閉じる" else "開く",
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (expanded) "閉じる" else "開く")
                }
            }
            Text(
                text = "リスク判定エンジン・DB・PIN 保存の動作確認や、文章をその場で判定する開発用ツールです。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelfTestPanel(
                        state = state,
                        onRun = onRunSelfTest,
                        onClear = onClearSelfTest,
                    )
                    QuickCheckPanel(
                        text = quickCheckText,
                        onTextChange = { quickCheckText = it },
                        state = state,
                        onRun = { onRunQuickCheck(quickCheckText) },
                        onClear = {
                            quickCheckText = ""
                            onClearQuickCheck()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelfTestPanel(
    state: ParentConsoleState,
    onRun: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "セルフテスト",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "リスク判定 / ログDB / 設定 / PIN 保存が動作しているかを確認します。\nログ一覧にも 1 件挿入されます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row {
            Button(onClick = onRun) { Text("実行") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClear) { Text("結果をクリア") }
        }
        state.selfTestReport?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickCheckPanel(
    text: String,
    onTextChange: (String) -> Unit,
    state: ParentConsoleState,
    onRun: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "文章を直接判定する",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "ルールエンジンの挙動確認に使います。実際の検知は入力監視や通知監視が自動で行います。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("例: LINE 教えて、あとでこっそり話そう") },
        )
        Row {
            Button(onClick = onRun) { Text("判定") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClear) { Text("クリア") }
        }
        state.quickCheckError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.quickCheckAssessment?.let { assessment ->
            val color = riskAccent(assessment.score)
            Text(
                text = "判定: ${RiskLabels.levelLabel(assessment.score)} (スコア ${assessment.score})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            if (assessment.categories.isNotEmpty()) {
                Text(
                    text = "カテゴリ: ${assessment.categories.joinToString(separator = " / ") { RiskLabels.categoryLabel(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            assessment.excerpt?.let {
                Text(
                    text = "抜粋: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun riskAccent(score: Int): Color = when {
    score >= 4 -> Color(0xFFC62828)
    score >= 2 -> Color(0xFFEF6C00)
    score >= 1 -> Color(0xFFF9A825)
    else -> Color(0xFF2E7D32)
}
