package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import com.turtlesafety.radar.parent.components.StatusCard

@Composable
fun HomeScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    val context = LocalContext.current

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
