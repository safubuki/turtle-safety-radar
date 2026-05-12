package com.turtlesafety.radar.ime

import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.turtlesafety.radar.core.Radar

/**
 * Safety IME 本体。MVP では「フルキーボード」ではなく
 * 「リスクチェックパネル」として動作する (仕様書 §12 の方針)。
 *
 * - ユーザーは通常の IME で入力後、本 IME に切り替える。
 * - 「リスクチェック」を押すと現在入力中のテキストをリスクエンジンに通し、
 *   結果のレベルに応じてバナーを表示する。
 * - 中リスク以上は [PreSendChecker] が DetectionLog に保存する。
 *
 * フル日本語入力対応は将来フェーズで検討する。
 */
class SafetyImeService : InputMethodService() {

    private lateinit var preSendChecker: PreSendChecker

    private var banner: TextView? = null
    private var status: TextView? = null

    override fun onCreate() {
        super.onCreate()
        val services = Radar.services()
        preSendChecker = PreSendChecker(
            riskEngine = services.riskEngine,
            repository = services.detectionLogRepository,
        )
    }

    override fun onCreateInputView(): View {
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(Color.parseColor("#FAFAFA"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        banner = TextView(ctx).apply {
            text = "Turtle Safety Radar"
            textSize = 16f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E7D32"))
            gravity = Gravity.CENTER
        }
        root.addView(banner)

        status = TextView(ctx).apply {
            text = "「リスクチェック」ボタンで送信前に内容を確認できます。"
            textSize = 14f
            setPadding(dp(4), dp(12), dp(4), dp(12))
            setTextColor(Color.parseColor("#333333"))
        }
        root.addView(status)

        val checkButton = Button(ctx).apply {
            text = "リスクチェック"
            setOnClickListener { onCheckClicked() }
        }
        root.addView(checkButton)

        val pickerButton = Button(ctx).apply {
            text = "別の入力方式に切り替え"
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                        as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
        }
        root.addView(pickerButton)

        return root
    }

    private fun onCheckClicked() {
        val ic: InputConnection = currentInputConnection ?: run {
            updateBanner(PreSendChecker.WarningLevel.NONE, message = "テキストを取得できませんでした")
            return
        }
        val before = ic.getTextBeforeCursor(MAX_PROBE_CHARS, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(MAX_PROBE_CHARS, 0)?.toString().orEmpty()
        val combined = (before + after).trim()
        if (combined.isEmpty()) {
            updateBanner(PreSendChecker.WarningLevel.NONE, message = "テキストが見つかりませんでした")
            return
        }
        val result = preSendChecker.check(combined)
        updateBanner(result.level, message = null)
    }

    private fun updateBanner(level: PreSendChecker.WarningLevel, message: String?) {
        val (text, bgColor) = when (level) {
            PreSendChecker.WarningLevel.NONE ->
                (message ?: "問題は見つかりませんでした") to "#2E7D32"
            PreSendChecker.WarningLevel.NOTICE ->
                level.displayMessage to "#F9A825"
            PreSendChecker.WarningLevel.WARN ->
                level.displayMessage to "#EF6C00"
            PreSendChecker.WarningLevel.STOP ->
                level.displayMessage to "#C62828"
        }
        banner?.text = text
        banner?.setBackgroundColor(Color.parseColor(bgColor))
        status?.text = "感度: 標準 | 警告レベル: ${level.name}"
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // 入力ターゲットが変わったらバナーをリセット。
        banner?.let { updateBanner(PreSendChecker.WarningLevel.NONE, message = null) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        /** リスク判定に渡す前後文字数。子どもの全文取得を避けるため上限を設ける。 */
        private const val MAX_PROBE_CHARS = 200
    }
}

