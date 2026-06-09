package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.core.log.DetectionLog
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import com.turtlesafety.radar.parent.format.RiskLabels
import com.turtlesafety.radar.parent.system.LogExportFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LogFilter(val label: String, val minScore: Int) {
    ALL("すべて", 0),
    NOTICE("注意以上", 1),
    HIGH("高め以上", 3),
    CRITICAL("高リスクのみ", 4),
}

@Composable
fun LogListScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    val context = LocalContext.current
    val filtered = remember(state.recentLogs, filter) {
        state.recentLogs.filter { it.score >= filter.minScore }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        SummaryRow(
            total = state.totalLogCount,
            unread = state.unacknowledgedLogCount,
            onShare = {
                LogExportFormatter.share(context, state.recentLogs)
            },
            shareEnabled = state.recentLogs.isNotEmpty(),
            onClearAll = { confirmClear = true },
        )

        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState(filterApplied = filter != LogFilter.ALL && state.recentLogs.isNotEmpty())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { log ->
                    LogCard(log = log, onAcknowledge = { vm.acknowledge(log.id) })
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("ログを全削除しますか？") },
            text = { Text("この操作は取り消せません。すべての検知ログが消えます。") },
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
private fun SummaryRow(
    total: Int,
    unread: Int,
    onShare: () -> Unit,
    shareEnabled: Boolean,
    onClearAll: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$total 件のログ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (unread > 0) "未確認 $unread 件" else "未確認はありません",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onShare, enabled = shareEnabled) {
            Text("共有")
        }
        TextButton(onClick = onClearAll) {
            Text("全削除")
        }
    }
}

@Composable
private fun EmptyState(filterApplied: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = if (filterApplied) "このフィルタに該当するログはありません" else "まだ検知されたログはありません",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (filterApplied)
                "「すべて」フィルタに戻すと過去のログを確認できます。"
            else
                "通知監視・入力監視が有効な間に高リスクが検知されると、ここに表示されます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogCard(log: DetectionLog, onAcknowledge: () -> Unit) {
    val accent = riskAccent(log.score)
    val categoryLabel = RiskLabels.categoryLabels(log.categories).ifBlank { "カテゴリなし" }
    val sourceLabel = RiskLabels.sourceLabel(log.source)
    val sourceIcon = sourceIconFor(log.source)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = RiskLabels.levelLabel(log.score),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (!log.acknowledged) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(50)),
                    )
                }
            }

            Spacer(Modifier.size(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = sourceIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = buildString {
                        append(sourceLabel)
                        if (!log.appName.isNullOrBlank()) {
                            append(" · ")
                            append(log.appName)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.size(6.dp))
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            log.excerpt?.let { excerpt ->
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("スコア ${log.score}") },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
                Spacer(Modifier.weight(1f))
                if (!log.acknowledged) {
                    TextButton(onClick = onAcknowledge) {
                        Text("確認済にする")
                    }
                } else {
                    Text(
                        text = "確認済",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
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

private fun sourceIconFor(rawSource: String): ImageVector = when (rawSource) {
    "NOTIFICATION" -> Icons.Outlined.NotificationsActive
    "IME" -> Icons.Outlined.Keyboard
    "IMAGE" -> Icons.Outlined.Image
    "QR" -> Icons.Outlined.QrCode
    "SYSTEM" -> Icons.Outlined.Settings
    else -> Icons.Outlined.Inbox
}

private fun formatTimestamp(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    return fmt.format(Date(millis))
}
