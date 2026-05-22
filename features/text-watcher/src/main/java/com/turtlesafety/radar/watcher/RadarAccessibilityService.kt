package com.turtlesafety.radar.watcher

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.turtlesafety.radar.core.Radar
import com.turtlesafety.radar.core.log.DetectionLogRepository
import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.risk.RiskAssessment
import com.turtlesafety.radar.core.risk.RiskCategory
import com.turtlesafety.radar.core.risk.RiskEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全 IME・全アプリの入力テキスト変化を傍受し、その場でリスク判定する中核サービス。
 *
 * 仕様書 §5.3 の Safety IME 「送信前の文章を端末内で安全チェックする」目的を、
 * AccessibilityService 経由で実現する(子どもが Gboard 等を使っていても監視可能)。
 *
 * - typeViewTextChanged を契機にフォーカス中入力フィールドのテキストを取得
 * - 同一テキストの連続検出はデバウンスで間引く
 * - パスワード/認証コードらしき欄は SkipInspection (input field privacy)
 * - 高リスクのみ最小限ログを保存(全文ログ化はしない)
 * - スコア >= 4 のとき子ども側へ短いトースト警告 (仕様書 §10)
 */
class RadarAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var riskEngine: RiskEngine? = null
    private var repository: DetectionLogRepository? = null
    private var initError: String? = null

    /** 同一テキストの再判定を防ぐ短期メモリ(プロセスメモリのみ、永続化しない)。 */
    private var lastEvaluatedText: String? = null
    private var lastEvaluatedAt: Long = 0L

    /** 子ども側警告のスロットリング (連打を避ける)。 */
    private var lastChildWarnAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        try {
            val services = Radar.services()
            riskEngine = services.riskEngine
            repository = services.detectionLogRepository
        } catch (t: Throwable) {
            Log.e(TAG, "Radar.services() failed in accessibility service", t)
            initError = t.message ?: t::class.java.simpleName
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "RadarAccessibilityService connected; initError=$initError")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (riskEngine == null || repository == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        val source = event.source ?: return
        try {
            if (!source.isEditable) return
            if (source.isPassword) return
            val text = source.text?.toString().orEmpty().trim()
            if (text.length < MIN_TEXT_LENGTH) return
            if (text == lastEvaluatedText) return

            val pkg = event.packageName?.toString()
            scheduleEvaluate(text, pkg)
        } finally {
            // AccessibilityNodeInfo はリサイクルが推奨。
            runCatching { source.recycle() }
        }
    }

    private fun scheduleEvaluate(text: String, packageName: String?) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            val now = System.currentTimeMillis()
            if (text == lastEvaluatedText && (now - lastEvaluatedAt) < REPEAT_SUPPRESS_MS) return@launch
            lastEvaluatedText = text
            lastEvaluatedAt = now

            val engine = riskEngine ?: return@launch
            val repo = repository ?: return@launch

            val assessment = runCatching {
                engine.assess(text = text, source = DetectionSource.IME, appName = packageName)
            }.getOrNull() ?: return@launch

            if (assessment.score >= MIN_LOG_SCORE) {
                runCatching {
                    repo.record(
                        assessment = assessment,
                        source = DetectionSource.IME,
                        appName = packageName,
                    )
                }
            }

            // 仕様書 §10: 子どもへ警告を表示する。
            // 高リスクのみ、かつ短時間に連発しないよう一定間隔で抑制する。
            if (assessment.score >= MIN_CHILD_WARN_SCORE) {
                val warnNow = System.currentTimeMillis()
                if (warnNow - lastChildWarnAt >= CHILD_WARN_SUPPRESS_MS) {
                    lastChildWarnAt = warnNow
                    showChildWarning(assessment)
                }
            }
        }
    }

    private fun showChildWarning(assessment: RiskAssessment) {
        val message = childWarningMessageFor(assessment)
        mainHandler.post {
            runCatching {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun childWarningMessageFor(assessment: RiskAssessment): String {
        val categories = assessment.categories
        // 仕様書 §10 の例文に沿ったメッセージを優先カテゴリで選ぶ。
        return when {
            RiskCategory.SEXUAL_REQUEST in categories ->
                "写真や自撮りを送る前に保護者に相談してください"
            RiskCategory.COERCION in categories ->
                "脅されていると感じたらすぐに家族に相談してください"
            RiskCategory.MEETUP in categories ->
                "知らない人と会う約束は危険です。家族に相談してください"
            RiskCategory.CONTACT_EXCHANGE in categories ->
                "知らない人に連絡先や QR / ID を送るのは危険です"
            RiskCategory.SECRECY in categories ->
                "親に内緒のやり取りに注意。不安なときは家族に相談を"
            RiskCategory.IDENTITY_UNKNOWN in categories ->
                "相手のことが分からない時は連絡先を渡さないでください"
            else -> "今のやり取りには注意が必要です。家族に相談してください"
        }
    }

    override fun onInterrupt() {
        // no-op: 中断時に追加処理なし
    }

    override fun onDestroy() {
        super.onDestroy()
        debounceJob?.cancel()
    }

    companion object {
        private const val TAG = "RadarA11yService"

        /** 過剰反応を避ける入力長下限。 */
        private const val MIN_TEXT_LENGTH = 4

        /** 連続入力中の判定間引き。子どもがタイプ中に毎打鍵で走らせない。 */
        private const val DEBOUNCE_MS = 700L

        /** 同一テキストの再判定抑制窓。 */
        private const val REPEAT_SUPPRESS_MS = 5_000L

        /** ログ保存閾値(仕様書 §11: 高リスクのみ最小限ログ)。 */
        private const val MIN_LOG_SCORE = 3

        /** 子ども側警告の発火閾値 (仕様書 §10: 高リスク相当)。 */
        private const val MIN_CHILD_WARN_SCORE = 4

        /** 子ども側警告のスロットリング (連打防止)。 */
        private const val CHILD_WARN_SUPPRESS_MS = 30_000L
    }
}

/** AccessibilityNodeInfo は Iterable ではないので拡張で扱う。 */
private operator fun AccessibilityNodeInfo.iterator(): Iterator<AccessibilityNodeInfo> =
    object : Iterator<AccessibilityNodeInfo> {
        private var index = 0
        override fun hasNext(): Boolean = index < childCount
        override fun next(): AccessibilityNodeInfo = getChild(index++)
    }
