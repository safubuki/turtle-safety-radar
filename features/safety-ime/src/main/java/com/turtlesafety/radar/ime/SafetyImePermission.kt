package com.turtlesafety.radar.ime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/**
 * Safety IME の有効化状態を確認するヘルパー。
 *
 * - [isEnabled]: 端末に "有効な入力方式" として登録されているか。
 * - [isDefault]: 現在のデフォルト IME が Safety IME か。
 * - [openImeSettings] / [showPicker]: 保護者が状態変更へ誘導するための導線。
 */
object SafetyImePermission {

    private const val DEFAULT_IME_KEY = "default_input_method"
    private const val ENABLED_IMES_KEY = "enabled_input_methods"

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, ENABLED_IMES_KEY) ?: return false
        val expected = component(context).flattenToString()
        return flat.split(":").any { it.startsWith(expected) }
    }

    fun isDefault(context: Context): Boolean {
        val current = Settings.Secure.getString(context.contentResolver, DEFAULT_IME_KEY) ?: return false
        val expected = component(context).flattenToString()
        return current == expected
    }

    fun openImeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** IME 切替ピッカーを表示する。アプリ UI からの呼び出し用。 */
    fun showPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun component(context: Context): ComponentName =
        ComponentName(context, SafetyImeService::class.java)
}
