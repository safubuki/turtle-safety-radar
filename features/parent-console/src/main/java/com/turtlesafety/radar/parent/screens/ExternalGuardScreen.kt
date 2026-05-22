package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.parent.ParentConsoleState
import com.turtlesafety.radar.parent.ParentConsoleViewModel
import com.turtlesafety.radar.parent.guard.ChecklistCategory
import com.turtlesafety.radar.parent.guard.ChecklistDefinitions
import com.turtlesafety.radar.parent.guard.ChecklistItem

@Composable
fun ExternalGuardScreen(
    state: ParentConsoleState,
    vm: ParentConsoleViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
    ) {
        item { HeaderCard(state = state) }
        ChecklistDefinitions.categories.forEach { category ->
            item {
                CategoryCard(
                    category = category,
                    checked = state.checklistState,
                    onToggle = { id, value -> vm.setChecklistItem(id, value) },
                )
            }
        }
    }
}

@Composable
private fun HeaderCard(state: ParentConsoleState) {
    val progress = state.checklistProgress
    Card(colors = CardDefaults.elevatedCardColors(), elevation = CardDefaults.elevatedCardElevation()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "外部の見守りチェック",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    "このアプリだけでは SNS やブラウザの完全制限はできません。" +
                        "Family Link や端末標準のキッズモードなどと併用しているかを保護者ご自身で確認してください。",
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progress.ratio },
            )
            Text(
                text = "${progress.done} / ${progress.total} 項目を確認済み",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: ChecklistCategory,
    checked: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = iconFor(category.id),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Box(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider()
            category.items.forEach { item ->
                ChecklistRow(
                    item = item,
                    checked = checked[item.id] ?: false,
                    onToggle = { value -> onToggle(item.id, value) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
            )
            item.hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun iconFor(categoryId: String): ImageVector = when (categoryId) {
    "apps" -> Icons.Outlined.Apps
    "guard" -> Icons.Outlined.Shield
    "contact" -> Icons.Outlined.Phone
    "location" -> Icons.Outlined.Place
    else -> Icons.Outlined.Shield
}
