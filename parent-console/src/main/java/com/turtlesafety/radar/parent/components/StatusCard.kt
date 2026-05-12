package com.turtlesafety.radar.parent.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatusCard(
    title: String,
    state: String,
    ok: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val accent = if (ok) Color(0xFF2E7D32) else Color(0xFFC62828)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (ok) "● 有効" else "● 未有効",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(text = state, style = MaterialTheme.typography.bodyMedium)
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}
