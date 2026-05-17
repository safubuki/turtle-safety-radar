package com.turtlesafety.radar.parent.screens

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.turtlesafety.radar.ai.LocalAiModeDescriptions
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import com.turtlesafety.radar.parent.components.StatusCard
import com.turtlesafety.radar.parent.system.ParentNotificationPermission

@Composable
fun HomeScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    var quickCheckText by rememberSaveable { mutableStateOf("") }
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "見守り状態",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Button(onClick = { vm.refreshPermissions(context) }) {
            Text("権限状態を再診断")
        }

        SetupProgressCard(state = state)
        QuickStartCard()
        QuickCheckCard(
            text = quickCheckText,
            onTextChange = { quickCheckText = it },
            state = state,
            onRun = { vm.previewQuickCheck(quickCheckText) },
            onClear = {
                quickCheckText = ""
                vm.clearQuickCheck()
            },
        )

        StatusCard(
            title = "通知監視",
            state = if (state.notificationListenerEnabled)
                "通知本文をリスク判定中"
            else
                "通知へのアクセスを許可してください",
            ok = state.notificationListenerEnabled,
            actionLabel = "通知アクセス設定を開く",
            onAction = { vm.openNotificationListenerSettings(context) },
        )

        StatusCard(
            title = "Safety IME",
            state = when {
                state.safetyImeIsDefault -> "デフォルト入力方式に設定済み"
                state.safetyImeEnabled -> "有効化済み (デフォルト未設定)"
                else -> "入力方式として有効にしてください"
            },
            ok = state.safetyImeEnabled,
            actionLabel = if (state.safetyImeEnabled) "入力方式ピッカーを表示" else "入力方式設定を開く",
            onAction = {
                if (state.safetyImeEnabled) vm.showImePicker(context)
                else vm.openImeSettings(context)
            },
        )

        StatusCard(
            title = "保護者通知",
            state = if (state.parentNotificationsEnabled)
                "高リスク時の端末通知を受信できます"
            else
                if (shouldRequestParentNotifications)
                    "高リスク通知を受けるには通知権限を許可してください"
                else
                    "アプリ通知を有効にしてください",
            ok = state.parentNotificationsEnabled,
            actionLabel = if (shouldRequestParentNotifications) "通知を許可" else "通知設定を開く",
            onAction = {
                if (shouldRequestParentNotifications && ParentNotificationPermission.needsRuntimePermission(context)) {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.openParentNotificationSettings(context)
                }
            },
        )

        Card(colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Media Checker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (state.mediaCheckerEnabled) "画像検査を利用できます" else "画像検査は無効です",
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.lastMediaScanResult?.let {
                    Text(
                        text = "直近 score ${it.score} / ${it.warningMessage}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Local AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = LocalAiModeDescriptions.describe(state.localAiMode).title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = LocalAiModeDescriptions.describe(state.localAiMode).note,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(
            modifier = Modifier,
            colors = CardDefaults.cardColors(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "検知ログ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "保存中: ${state.totalLogCount} 件",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "External Guard チェックリスト",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.checklistCheckedCount} / ${state.checklistTotal} 項目を確認済",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SetupProgressCard(state: ParentConsoleState) {
    val completedSteps = listOf(
        state.notificationListenerEnabled,
        state.safetyImeEnabled,
        state.parentNotificationsEnabled,
    ).count { it }
    val totalSteps = 3
    val nextAction = when {
        !state.notificationListenerEnabled -> "1. 通知アクセスを許可してください"
        !state.safetyImeEnabled -> "2. Safety IME を有効化してください"
        !state.parentNotificationsEnabled -> "3. 保護者通知を許可してください"
        !state.safetyImeIsDefault -> "補足: Safety IME を標準入力方式に設定すると使いやすくなります"
        else -> "初期設定は完了しています"
    }

    Card(colors = CardDefaults.cardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "初期セットアップ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { completedSteps.toFloat() / totalSteps.toFloat() },
            )
            Text(
                text = "$completedSteps / $totalSteps 完了",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = nextAction,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun QuickStartCard() {
    Card(colors = CardDefaults.cardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "このアプリで今できること",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "1. 通知を監視して危険な兆候を記録します",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "2. 下のテスト欄で、その場で文章の危険度を確認できます",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "3. Safety IME は通常キーボードではなく、送信前チェック専用です",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "4. Media Checker は設定画面から画像を選んで試せます",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun QuickCheckCard(
    text: String,
    onTextChange: (String) -> Unit,
    state: ParentConsoleState,
    onRun: () -> Unit,
    onClear: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "その場で試す",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "まず通常キーボードで文章を入力してください。Safety IME は文字入力ではなく、入力後のチェックに使います。",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("例: LINE教えて、あとでこっそり話そう") },
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onRun) {
                    Text("この文章を判定")
                }
                TextButton(onClick = onClear) {
                    Text("クリア")
                }
            }
            state.quickCheckError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.quickCheckAssessment?.let { assessment ->
                val level = when {
                    assessment.score >= 4 -> "STOP"
                    assessment.score >= 2 -> "WARN"
                    assessment.score >= 1 -> "NOTICE"
                    else -> "NONE"
                }
                Text(
                    text = "判定: $level / score ${assessment.score}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = assessment.reason,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (assessment.categories.isNotEmpty()) {
                    Text(
                        text = "カテゴリ: ${assessment.categories.joinToString(separator = ", ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                assessment.excerpt?.let {
                    Text(
                        text = "抜粋: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
