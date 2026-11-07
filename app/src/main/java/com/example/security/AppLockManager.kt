package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Lets screens that open a system picker skip the lock that would follow the app going to background. */
object AppLockSession {
    @Volatile
    var skipNextLock: Boolean = false
}

/**
 * App lock (PIN + optional biometrics). Only a salted PBKDF2 hash of the PIN is stored, never
 * the PIN itself. After [MAX_ATTEMPTS] wrong tries the pad is blocked for a while.
 */
class AppLockManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    val isEnabled: Boolean get() = prefs.getString(KEY_HASH, null) != null

    /** Number of digits of the saved PIN, so the pad can check it as soon as it is complete. */
    val pinLength: Int get() = prefs.getInt(KEY_LENGTH, 4)

    var biometricEnabled: Boolean
        get() = isEnabled && prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** Hide app content in the recent-apps switcher and block screenshots. */
    var secureScreen: Boolean
        get() = prefs.getBoolean(KEY_SECURE_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()

    fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN must be 4 to 6 digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, salt.toHex())
            .putString(KEY_HASH, hash(pin, salt).toHex())
            .putInt(KEY_LENGTH, pin.length)
            .putInt(KEY_FAILS, 0)
            .putLong(KEY_BLOCKED_UNTIL, 0L)
            .apply()
    }

    val hasRecoveryCode: Boolean get() = prefs.getString(KEY_RECOVERY_HASH, null) != null

    /**
     * Creates a new one-time recovery code (shown to the user once) that can reset a forgotten PIN.
     * Without it a forgotten PIN would mean losing all data, because nothing is stored on a server.
     * Only a salted hash of the code is kept.
     */
    fun createRecoveryCode(): String {
        val random = SecureRandom()
        val raw = (1..RECOVERY_LENGTH).map { RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)] }.joinToString("")
        val salt = ByteArray(16).also { random.nextBytes(it) }
        prefs.edit()
            .putString(KEY_RECOVERY_SALT, salt.toHex())
            .putString(KEY_RECOVERY_HASH, hash(raw, salt).toHex())
            .apply()
        return formatRecoveryCode(raw)
    }

    /** Checks the recovery code and, if it matches, replaces the PIN. Uses the same attempt limit as the PIN. */
    fun resetPinWithRecoveryCode(code: String, newPin: String): Boolean {
        if (!isValidPin(newPin) || remainingBlockMillis() > 0) return false
        val salt = prefs.getString(KEY_RECOVERY_SALT, null)?.fromHex() ?: return false
        val expected = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        val ok = constantTimeEquals(hash(normalizeRecoveryCode(code), salt).toHex(), expected)
        if (ok) {
            setPin(newPin)
        } else {
            val fails = prefs.getInt(KEY_FAILS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILS, fails)
            if (fails >= MAX_ATTEMPTS) {
                editor.putInt(KEY_FAILS, 0).putLong(KEY_BLOCKED_UNTIL, SystemClock.elapsedRealtime() + BLOCK_MILLIS)
            }
            editor.apply()
        }
        return ok
    }

    fun disable() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).remove(KEY_BIOMETRIC).remove(KEY_LENGTH)
            .remove(KEY_RECOVERY_HASH).remove(KEY_RECOVERY_SALT)
            .putInt(KEY_FAILS, 0).putLong(KEY_BLOCKED_UNTIL, 0L).apply()
    }

    /** Milliseconds the pad stays blocked after too many wrong attempts (0 = not blocked). */
    fun remainingBlockMillis(): Long {
        val until = prefs.getLong(KEY_BLOCKED_UNTIL, 0L)
        val now = SystemClock.elapsedRealtime()
        // elapsedRealtime restarts on reboot; a stale value larger than the block time is ignored.
        val left = until - now
        return if (left in 1..BLOCK_MILLIS) left else 0L
    }

    fun verify(pin: String): Boolean {
        if (remainingBlockMillis() > 0) return false
        val salt = prefs.getString(KEY_SALT, null)?.fromHex() ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        val ok = constantTimeEquals(hash(pin, salt).toHex(), expected)
        if (ok) {
            prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_BLOCKED_UNTIL, 0L).apply()
        } else {
            val fails = prefs.getInt(KEY_FAILS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILS, fails)
            if (fails >= MAX_ATTEMPTS) {
                editor.putInt(KEY_FAILS, 0).putLong(KEY_BLOCKED_UNTIL, SystemClock.elapsedRealtime() + BLOCK_MILLIS)
            }
            editor.apply()
        }
        return ok
    }

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILS, 0)

    /** Biometric prompt from the platform (no extra library). Available on Android 10+. */
    fun isBiometricAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val manager = appContext.getSystemService(android.hardware.biometrics.BiometricManager::class.java) ?: return false
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK)
        } else {
            @Suppress("DEPRECATION")
            manager.canAuthenticate()
        }
        return status == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val NAME = "hanita_security"
        const val MAX_ATTEMPTS = 5
        const val BLOCK_MILLIS = 30_000L
        private const val ITERATIONS = 20_000
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_SECURE_SCREEN = "secure_screen"
        private const val KEY_FAILS = "failed_attempts"
        private const val KEY_BLOCKED_UNTIL = "blocked_until"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val RECOVERY_LENGTH = 12
        /** No 0/O, 1/I/L: the code is easy to copy by hand without mistakes. */
        private const val RECOVERY_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

        fun formatRecoveryCode(raw: String): String = raw.chunked(4).joinToString("-")

        /** Accepts the code with or without dashes/spaces, in any case, typed with Persian digits too. */
        fun normalizeRecoveryCode(input: String): String = buildString {
            input.uppercase().forEach { c ->
                when (c) {
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    in 'A'..'Z', in '0'..'9' -> append(c)
                }
            }
        }

        /** Persian (۰-۹) and Arabic-Indic (٠-٩) digits typed on a Persian keyboard become 0-9. */
        fun normalizeDigits(input: String): String = buildString {
            input.forEach { c ->
                when (c) {
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    in '0'..'9' -> append(c)
                }
            }
        }

        /** "۶۲٫۵" or "62,5" typed on a Persian keyboard becomes "62.5" (other characters are dropped). */
        fun normalizeDecimal(input: String): String = buildString {
            input.trim().forEach { c ->
                when (c) {
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    in '0'..'9' -> append(c)
                    '.', ',', '٫', '/' -> if (!contains('.')) append('.')
                }
            }
        }

        fun isValidPin(pin: String): Boolean = pin.length in 4..6 && pin.all { it in '0'..'9' }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
        private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        private fun constantTimeEquals(a: String, b: String): Boolean {
            if (a.length != b.length) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
            return diff == 0
        }
    }
}
