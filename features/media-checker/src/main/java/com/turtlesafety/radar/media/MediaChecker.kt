package com.turtlesafety.radar.media

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.RiskEngine
import kotlinx.coroutines.tasks.await

class MediaChecker(
    private val context: Context,
    private val riskEngine: RiskEngine,
    private val repository: DetectionLogRepository,
) {

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
            )
            .build(),
    )

    private val textRecognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build(),
    )

    suspend fun analyze(uri: Uri): MediaScanResult {
        val image = InputImage.fromFilePath(context, uri)
        val qrPayloads = barcodeScanner.process(image).await()
            .mapNotNull { it.rawValue?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val extractedText = textRecognizer.process(image).await().text.orEmpty()

        val result = MediaSignalAnalyzer.analyze(
            extractedText = extractedText,
            qrPayloads = qrPayloads,
            riskEngine = riskEngine,
        )
        if (result.score >= MIN_LOG_SCORE) {
            repository.record(
                assessment = result.assessment,
                source = result.source,
                appName = null,
            )
        }
        return result
    }

    companion object {
        const val MIN_LOG_SCORE = 2
    }
}