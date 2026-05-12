package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogListScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "検知ログ (${state.recentLogs.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { confirmClear = true }) {
                Text("全削除")
            }
        }

        if (state.recentLogs.isEmpty()) {
            Text(
                text = "まだ検知されたログはありません",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.recentLogs, key = { it.id }) { log ->
                    LogCard(log = log, onAcknowledge = { vm.acknowledge(log.id) })
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("ログを全削除しますか？") },
            text = { Text("この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAllLogs()
                    confirmClear = false
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun LogCard(log: DetectionLog, onAcknowledge: () -> Unit) {
    val accent = when {
        log.score >= 4 -> Color(0xFFC62828)
        log.score >= 2 -> Color(0xFFEF6C00)
        log.score >= 1 -> Color(0xFFF9A825)
        else -> Color(0xFF2E7D32)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "score ${log.score}",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(text = formatTimestamp(log.timestamp), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "${log.source}${log.appName?.let { " - $it" }.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = log.categories.ifEmpty { "(no category)" },
                style = MaterialTheme.typography.bodyMedium,
            )
            log.excerpt?.let { excerpt ->
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!log.acknowledged) {
                TextButton(onClick = onAcknowledge) { Text("確認済にする") }
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPAN)
    return fmt.format(Date(millis))
}
