package com.turtlesafety.radar.parent.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.ime.SafetyImePermission

private enum class SetupStep { Welcome, Pin }

@Composable
fun PinSetupScreen(
    error: String?,
    onSubmit: (String) -> Unit,
) {
    var step by remember { mutableStateOf(SetupStep.Welcome) }

    when (step) {
        SetupStep.Welcome -> WelcomeStep(onContinue = { step = SetupStep.Pin })
        SetupStep.Pin -> PinStep(
            error = error,
            onBack = { step = SetupStep.Welcome },
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Turtle Safety Radar へようこそ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "子どもの端末利用で起きやすい危険な兆候を、端末内で検知するアプリです。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FeatureCard(
            icon = Icons.Outlined.NotificationsActive,
            title = "通知監視",
            description = "LINE / SMS / SNS の通知本文を端末内で判定し、リスクの高いやり取りを保護者に通知します。",
        )
        FeatureCard(
            icon = Icons.Outlined.Keyboard,
            title = "入力監視",
            description = "どのキーボードを使っていても、入力テキストを端末内でチェックし、危険なメッセージ送信前に警告できます。",
        )
        FeatureCard(
            icon = Icons.Outlined.Image,
            title = "画像チェッカー",
            description = "スクリーンショットや受信画像から QR / 連絡先 / 個人情報の兆候を抽出します。画像本体は保存しません。",
        )

        PrivacyCard()

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(text = "はじめる", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Card(colors = CardDefaults.cardColors()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "プライバシーの考え方",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = "・キーロガーではありません。すべての入力・会話を保存することはしません。\n" +
                    "・パスワードや認証コードは検査・保存しません。\n" +
                    "・高リスク時のみ、ごく短い抜粋を端末内ログに残します。\n" +
                    "・ログや設定はクラウドに送信しません。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PinStep(
    error: String?,
    onBack: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatched = confirm.isNotEmpty() && pin != confirm
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
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            text = "管理者 PIN を設定",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Parent Console を守る PIN を 4 桁以上で設定してください。子どもには伝えないでください。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (safetyImeIsDefault) {
            SafetyImeWarningCard(context = context)
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("新しい PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("PIN を再入力") },
            singleLine = true,
            isError = mismatched,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )

        if (mismatched) {
            Text(
                text = "PIN が一致しません",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Button(
            enabled = pin.length >= 4 && pin == confirm,
            onClick = { onSubmit(pin) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("この PIN で設定する", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onBack) {
            Text("← 戻る")
        }
    }
}

@Composable
private fun SafetyImeWarningCard(context: android.content.Context) {
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
                text = "Safety IME はチェック専用パネルで文字入力できません。Gboard などの通常キーボードを一時的にデフォルトに戻してください。",
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
