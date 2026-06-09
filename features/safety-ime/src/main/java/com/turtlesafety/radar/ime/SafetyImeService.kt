package com.turtlesafety.radar.ime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setBackgroundColor(COLOR_BG)
            minimumHeight = dp(280)
        }

        banner = TextView(ctx).apply {
            text = if (initErrorMessage == null) "Turtle Safety Radar" else "初期化に失敗: $initErrorMessage"
            textSize = 16f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setTextColor(Color.WHITE)
            background = roundedDrawable(
                if (initErrorMessage == null) COLOR_OK else COLOR_STOP,
                radiusDp = 12,
            )
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(banner, marginParams(topDp = 0))

        val title = TextView(ctx).apply {
            text = "送信前リスクチェック"
            textSize = 14f
            setTextColor(COLOR_TITLE)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(2))
        }
        root.addView(title)

        status = TextView(ctx).apply {
            text = "このパネルは文字入力できません。通常キーボードで書いたあと、ここでチェックボタンを押してください。"
            textSize = 13f
            setPadding(0, 0, 0, dp(2))
            setTextColor(COLOR_BODY)
        }
        root.addView(status)

        detail = TextView(ctx).apply {
            text = "パスワードや認証コード欄は検査対象外です。"
            textSize = 12f
            setPadding(0, dp(2), 0, dp(8))
            setTextColor(COLOR_MUTED)
        }
        root.addView(detail)

        val checkButton = Button(ctx).apply {
            text = "現在のテキストをリスクチェック"
            isAllCaps = false
            setOnClickListener { onCheckClicked() }
        }
        root.addView(checkButton, marginParams(topDp = 8))

        val pickerButton = Button(ctx).apply {
            text = "別のキーボードに切り替える"
            isAllCaps = false
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
        root.addView(pickerButton, marginParams(topDp = 6))

        val footer = TextView(ctx).apply {
            text = "文字入力できない場合は、上のボタンで通常キーボードに戻してください。"
            textSize = 11f
            setPadding(0, dp(10), 0, 0)
            setTextColor(COLOR_MUTED)
        }
        root.addView(footer)

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
            .joinToString(separator = ", ") { categoryLabel(it) }
            .ifBlank { "カテゴリなし" }
        val excerpt = result.assessment.excerpt ?: "抜粋なし"
        return "カテゴリ: $categories\n抜粋: $excerpt"
    }

    /** 仕様書 §7 の日本語カテゴリ名で表示する。 */
    private fun categoryLabel(category: com.turtlesafety.radar.core.risk.RiskCategory): String =
        when (category) {
            com.turtlesafety.radar.core.risk.RiskCategory.CONTACT_EXCHANGE -> "連絡先交換"
            com.turtlesafety.radar.core.risk.RiskCategory.SECRECY -> "秘密化"
            com.turtlesafety.radar.core.risk.RiskCategory.MEETUP -> "会う約束"
            com.turtlesafety.radar.core.risk.RiskCategory.SEXUAL_REQUEST -> "性的・自撮り要求"
            com.turtlesafety.radar.core.risk.RiskCategory.COERCION -> "脅し・支配"
            com.turtlesafety.radar.core.risk.RiskCategory.IDENTITY_UNKNOWN -> "身元不明"
        }

    private fun updateBanner(
        level: PreSendChecker.WarningLevel,
        message: String?,
        details: String,
    ) {
        val (text, bgColor) = when (level) {
            PreSendChecker.WarningLevel.NONE ->
                (message ?: "問題は見つかりませんでした") to COLOR_OK
            PreSendChecker.WarningLevel.NOTICE ->
                level.displayMessage to COLOR_NOTICE
            PreSendChecker.WarningLevel.WARN ->
                level.displayMessage to COLOR_WARN
            PreSendChecker.WarningLevel.STOP ->
                level.displayMessage to COLOR_STOP
        }
        banner?.text = text
        banner?.background = roundedDrawable(bgColor, radiusDp = 12)
        val levelLabel = when (level) {
            PreSendChecker.WarningLevel.NONE -> "問題なし"
            PreSendChecker.WarningLevel.NOTICE -> "注意"
            PreSendChecker.WarningLevel.WARN -> "要確認"
            PreSendChecker.WarningLevel.STOP -> "高リスク"
        }
        val statusLine = services?.let {
            "感度: ${it.settingsStore.sensitivity.name} · AI: ${it.settingsStore.localAiMode.name} · 判定: $levelLabel"
        } ?: "(初期化エラー) · 判定: $levelLabel"
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
                    "通常キーボードで入力後に「現在のテキストをリスクチェック」を押してください。",
            )
        }
    }

    private fun marginParams(topDp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(topDp)
        }

    private fun roundedDrawable(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "SafetyImeService"

        /** リスク判定に渡す前後文字数。子どもの全文取得を避けるため上限を設ける。 */
        private const val MAX_PROBE_CHARS = 200

        private val COLOR_BG = Color.parseColor("#F5F8F4")
        private val COLOR_TITLE = Color.parseColor("#1B5E20")
        private val COLOR_BODY = Color.parseColor("#212121")
        private val COLOR_MUTED = Color.parseColor("#616161")

        private val COLOR_OK = Color.parseColor("#2E7D32")
        private val COLOR_NOTICE = Color.parseColor("#F9A825")
        private val COLOR_WARN = Color.parseColor("#EF6C00")
        private val COLOR_STOP = Color.parseColor("#C62828")
    }
}
