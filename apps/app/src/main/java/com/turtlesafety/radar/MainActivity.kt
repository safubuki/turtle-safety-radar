package com.turtlesafety.radar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turtlesafety.radar.parent.ParentConsole
import com.turtlesafety.radar.ui.theme.TurtleSafetyRadarTheme
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TurtleSafetyRadarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Root()
                }
            }
        }
    }
}

@Composable
private fun Root() {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashRecorder.lastCrash(context)) }
    if (crash != null) {
        CrashScreen(
            crashText = crash!!.first,
            crashAt = crash!!.second,
            onDismiss = {
                CrashRecorder.clear(context)
                crash = null
            },
        )
    } else {
        ParentConsole()
    }
}

@Composable
private fun CrashScreen(
    crashText: String,
    crashAt: Long,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val timestamp = remember(crashAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(Date(crashAt))
    }
    val report = remember(crashText, crashAt) {
        buildReport(context, crashText, crashAt)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "前回起動時にクラッシュしました",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "発生時刻: $timestamp", style = MaterialTheme.typography.bodySmall)
        Text(
            text = "下のスタックトレースをコピー/共有/保存できます。",
            style = MaterialTheme.typography.bodyMedium,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    copyToClipboard(context, report)
                    Toast.makeText(context, "クリップボードにコピーしました", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("クリップボードにコピー")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { shareReport(context, report, crashAt) },
            ) {
                Text("共有 (メール / メモ等へ送る)")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val saved = saveToDownloads(context, report, crashAt)
                    Toast.makeText(
                        context,
                        if (saved != null) "保存しました: $saved" else "保存に失敗しました",
                        Toast.LENGTH_LONG,
                    ).show()
                },
            ) {
                Text("Downloads に保存 (テキスト+JSON)")
            }
        }

        Card(colors = CardDefaults.cardColors()) {
            // SelectionContainer により長押し選択 → コピーも可能。
            SelectionContainer {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = report,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Button(onClick = onDismiss) {
            Text("閉じてアプリを続行")
        }
    }
}

private fun buildReport(context: Context, crashText: String, crashAt: Long): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).format(Date(crashAt))
    return buildString {
        appendLine("Turtle Safety Radar - Crash Report")
        appendLine("timestamp: $timestamp ($crashAt)")
        appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("android: ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT})")
        appendLine("app: ${context.packageName} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("---")
        append(crashText)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Turtle Safety Radar crash", text))
}

private fun shareReport(context: Context, text: String, crashAt: Long) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Turtle Safety Radar crash $crashAt")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(intent, "クラッシュ情報を共有")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun saveToDownloads(context: Context, text: String, crashAt: Long): String? {
    val base = "radar-crash-$crashAt"
    return runCatching {
        // テキストとJSONの両方を保存。
        val txtName = "$base.txt"
        val jsonName = "$base.json"
        val json = buildString {
            append('{')
            append("\"timestampMillis\":").append(crashAt).append(',')
            append("\"device\":\"").append(escapeJson("${Build.MANUFACTURER} ${Build.MODEL}")).append("\",")
            append("\"androidSdk\":").append(Build.VERSION.SDK_INT).append(',')
            append("\"appPackage\":\"").append(context.packageName).append("\",")
            append("\"versionName\":\"").append(BuildConfig.VERSION_NAME).append("\",")
            append("\"stackTrace\":\"").append(escapeJson(text)).append('"')
            append('}')
        }
        writeDownload(context, txtName, "text/plain", text)
        writeDownload(context, jsonName, "application/json", json)
        "$txtName + $jsonName"
    }.onFailure { it.printStackTrace() }
        .getOrNull()
}

private fun writeDownload(context: Context, name: String, mime: String, content: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("could not create $name")
        context.contentResolver.openOutputStream(uri)?.use { os: OutputStream ->
            os.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("could not open output stream for $name")
    } else {
        // 旧Android向け: アプリ専用領域に保存。
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: error("no external files dir")
        val file = java.io.File(dir, name)
        file.writeText(content, Charsets.UTF_8)
    }
}

private fun escapeJson(input: String): String =
    input.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
