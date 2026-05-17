package com.turtlesafety.radar.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 管理者 PIN の保存・検証。
 *
 * - 値は [EncryptedSharedPreferences] (AES256-GCM + AES256-SIV キー暗号化) に格納。
 * - 二重防御として PIN 自体は PBKDF2-HMAC-SHA256 のハッシュで保存し、平文は残さない。
 * - 検証は定数時間比較で行いタイミング攻撃を回避する。
 *
 * 仕様書 v0.4 §17.1: 「娘本人が設定変更できない」要件は本クラスの利用側 (Parent Console)
 * で実現する。本クラスは PIN 検証 API を提供するのみ。
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences by lazy { createEncryptedPrefs(context) }

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(pin.length in MIN_LENGTH..MAX_LENGTH) {
            "PIN must be $MIN_LENGTH..$MAX_LENGTH characters"
        }
        val salt = generateSalt()
        val hash = derivePbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, salt.toHex())
            .putString(KEY_HASH, hash.toHex())
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltHex = prefs.getString(KEY_SALT, null) ?: return false
        val storedHashHex = prefs.getString(KEY_HASH, null) ?: return false
        val candidate = derivePbkdf2(pin, saltHex.fromHex())
        return constantTimeEquals(candidate, storedHashHex.fromHex())
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        setPin(newPin)
        return true
    }

    fun clearPin() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).apply()
    }

    // -----------------------------------------------------------------------
    // crypto helpers
    // -----------------------------------------------------------------------

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun generateSalt(): ByteArray = ByteArray(SALT_BYTES).also {
        SecureRandom().nextBytes(it)
    }

    private fun derivePbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean =
        MessageDigest.isEqual(a, b)

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        require(length % 2 == 0) { "Invalid hex string" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 32

        private const val PREFS_NAME = "radar_secure_prefs"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        private const val PBKDF2_ITERATIONS = 100_000
    }
}
