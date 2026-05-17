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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel

@Composable
fun ChecklistScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    // checklistRevision を依存に含めることで再評価を促す。
    val snapshot = remember(state.checklistRevision) { vm.checklistSnapshot() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "External Guard チェックリスト",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "TONE / Family Link / MDM / 端末標準機能のいずれを使っているかに関わらず、" +
                "保護者として運用すべき項目です。",
            style = MaterialTheme.typography.bodySmall,
        )
        LinearProgressIndicator(
            progress = { if (state.checklistTotal == 0) 0f else state.checklistCheckedCount.toFloat() / state.checklistTotal },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${state.checklistCheckedCount} / ${state.checklistTotal} 完了",
            style = MaterialTheme.typography.bodyMedium,
        )

        snapshot.forEach { (category, entries) ->
            Card(colors = CardDefaults.cardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = category.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    entries.forEach { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = entry.isChecked,
                                onCheckedChange = { vm.setChecklistItem(entry.item.id, it) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.item.title)
                                entry.item.hint?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
