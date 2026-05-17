package com.turtlesafety.radar.parent.system

import android.content.Context
import android.content.Intent
import com.turtlesafety.radar.core.log.DetectionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExportFormatter {

    fun buildText(logs: List<DetectionLog>): String = buildString {
        appendLine("Turtle Safety Radar Log Export")
        appendLine("exportedAt=${formatTimestamp(System.currentTimeMillis())}")
        appendLine("count=${logs.size}")
        appendLine()

        logs.forEach { log ->
            appendLine("id=${log.id}")
            appendLine("timestamp=${formatTimestamp(log.timestamp)}")
            appendLine("source=${log.source}")
            appendLine("appName=${log.appName.orEmpty()}")
            appendLine("score=${log.score}")
            appendLine("categories=${log.categories}")
            appendLine("acknowledged=${log.acknowledged}")
            appendLine("reason=${log.reason}")
            appendLine("excerpt=${log.excerpt.orEmpty()}")
            appendLine("---")
        }
    }

    fun share(context: Context, logs: List<DetectionLog>) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Turtle Safety Radar Logs")
            putExtra(Intent.EXTRA_TEXT, buildText(logs))
        }
        context.startActivity(Intent.createChooser(intent, "ログを共有").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun formatTimestamp(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN)
        return fmt.format(Date(millis))
    }
}