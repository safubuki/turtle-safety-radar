package com.turtlesafety.radar.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.util.Log
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

    private var preSendChecker: PreSendChecker? = null
    private var services: com.turtlesafety.radar.core.CoreServices? = null
    private var initErrorMessage: String? = null

    private var banner: TextView? = null
    private var status: TextView? = null
    private var detail: TextView? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val s = Radar.services()
            services = s
            preSendChecker = PreSendChecker(
                riskEngine = s.riskEngine,
                repository = s.detectionLogRepository,
            )
        } catch (t: Throwable) {
            // Radar.init() が失敗していても IME 自体は起動できるようフォールバック。
            // ログだけ残し、パネルにはエラーを表示する。
            Log.e(TAG, "Safety IME init failed", t)
            initErrorMessage = t.message ?: t::class.java.simpleName
        }
    }

    override fun onCreateInputView(): View {
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            // 白背景アプリ上でも認識しやすい淡い色。
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            // 高さ 0 で透明に見えないよう最低高さを与える。
            minimumHeight = dp(260)
        }

        banner = TextView(ctx).apply {
            text = if (initErrorMessage == null)
                "Turtle Safety Radar"
            else
                "初期化に失敗: $initErrorMessage"
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setTextColor(Color.WHITE)
            setBackgroundColor(
                if (initErrorMessage == null) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            )
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(banner)

        status = TextView(ctx).apply {
            text = "これは文字入力キーボードではなく、送信前のチェック専用パネルです。"
            textSize = 14f
            setPadding(dp(4), dp(12), dp(4), dp(8))
            setTextColor(Color.parseColor("#1B5E20"))
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(status)

        detail = TextView(ctx).apply {
            text =
                "通常キーボード (Gboard など) で入力したあとに切り替えて使ってください。\n" +
                    "パスワードや認証コード欄は検査対象外です。"
            textSize = 13f
            setPadding(dp(4), dp(0), dp(4), dp(12))
            setTextColor(Color.parseColor("#333333"))
        }
        root.addView(detail)

        val checkButton = Button(ctx).apply {
            text = "現在のテキストをリスクチェック"
            setOnClickListener { onCheckClicked() }
        }
        root.addView(checkButton)

        val pickerButton = Button(ctx).apply {
            text = "別のキーボードに切り替える"
            setOnClickListener {
                try {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                            as android.view.inputmethod.InputMethodManager
                    imm.showInputMethodPicker()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to open IME picker", t)
                }
            }
        }
        root.addView(pickerButton)

        val footer = TextView(ctx).apply {
            text = "文字入力ができない場合は、上の「別のキーボードに切り替える」から元のキーボードに戻してください。"
            textSize = 12f
            setPadding(dp(4), dp(12), dp(4), dp(0))
            setTextColor(Color.parseColor("#555555"))
        }
        root.addView(footer)

        // LinearLayout の LayoutParams は親に追加されるとき置き換わるため、
        // root 自体には設定不要。MATCH_PARENT/WRAP_CONTENT は使わない。
        return root
    }

    private fun onCheckClicked() {
        val checker = preSendChecker
        if (checker == null) {
            updateBanner(
                level = PreSendChecker.WarningLevel.NONE,
                message = "初期化エラーのためチェックできません",
                details = initErrorMessage ?: "アプリを起動し直してください",
            )
            return
        }
        val inputType = currentInputEditorInfo?.inputType ?: 0
        if (ImeFieldSafety.shouldSkipInspection(inputType)) {
            updateBanner(
                level = PreSendChecker.WarningLevel.NONE,
                message = "この入力欄では安全のため検査しません",
                details = "パスワードや認証コードは取得・保存しません。",
            )
            return
        }
        val ic: InputConnection = currentInputConnection ?: run {
            updateBanner(
                level = PreSendChecker.WarningLevel.NONE,
                message = "テキストを取得できませんでした",
                details = "入力先アプリの制約により取得できない場合があります。",
            )
            return
        }
        val before = ic.getTextBeforeCursor(MAX_PROBE_CHARS, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(MAX_PROBE_CHARS, 0)?.toString().orEmpty()
        val combined = (before + after).trim()
        if (combined.isEmpty()) {
            updateBanner(
                level = PreSendChecker.WarningLevel.NONE,
                message = "テキストが見つかりませんでした",
                details = "カーソル周辺の文字がない場合は検査できません。",
            )
            return
        }
        val result = checker.check(combined)
        updateBanner(
            level = result.level,
            message = null,
            details = buildDetails(result),
        )
    }

    private fun buildDetails(result: PreSendChecker.CheckResult): String {
        val categories = result.assessment.categories
            .joinToString(separator = ", ") { it.name }
            .ifBlank { "カテゴリなし" }
        val excerpt = result.assessment.excerpt ?: "抜粋なし"
        return "カテゴリ: $categories\n抜粋: $excerpt"
    }

    private fun updateBanner(
        level: PreSendChecker.WarningLevel,
        message: String?,
        details: String,
    ) {
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
        val statusLine = services?.let {
            "チェック専用IME | 感度: ${it.settingsStore.sensitivity.name} | AI: ${it.settingsStore.localAiMode.name} | 警告レベル: ${level.name}"
        } ?: "チェック専用IME (初期化エラー) | 警告レベル: ${level.name}"
        status?.text = statusLine
        detail?.text = details
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // 入力ターゲットが変わったらバナーをリセット。
        val skipInspection = ImeFieldSafety.shouldSkipInspection(attribute?.inputType ?: 0)
        banner?.let {
            updateBanner(
                level = PreSendChecker.WarningLevel.NONE,
                message = if (skipInspection) "この入力欄では検査しません" else null,
                details = if (skipInspection)
                    "パスワードや認証コードは取得・保存しません。"
                else
                    "通常キーボードで入力後に「現在のテキストをリスクチェック」を押してください。文字入力そのものは行えません。",
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "SafetyImeService"

        /** リスク判定に渡す前後文字数。子どもの全文取得を避けるため上限を設ける。 */
        private const val MAX_PROBE_CHARS = 200
    }
}
