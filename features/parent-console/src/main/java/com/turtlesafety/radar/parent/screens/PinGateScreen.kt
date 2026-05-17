package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.ime.SafetyImePermission

@Composable
fun PinGateScreen(
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    val context = LocalContext.current
    val safetyImeIsDefault = remember { SafetyImePermission.isDefault(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Parent Console ロック中",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "管理者 PIN を入力してください。",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (safetyImeIsDefault) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Safety IME がデフォルトのキーボードになっています",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                    )
                    Text(
                        text = "Safety IME は文字入力できないパネルです。Gboard などの通常キーボードをデフォルトに戻してから PIN を入力してください。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { SafetyImePermission.openImeSettings(context) }) {
                        Text("入力方式設定を開く")
                    }
                    OutlinedButton(onClick = { SafetyImePermission.showPicker(context) }) {
                        Text("キーボード切替ピッカーを表示")
                    }
                }
            }
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error != null,
            modifier = Modifier.fillMaxWidth(0.8f),
        )

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            enabled = pin.length >= 4,
            onClick = { onSubmit(pin); pin = "" },
        ) {
            Text("解除する")
        }
    }
}
