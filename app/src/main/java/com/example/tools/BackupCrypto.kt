package com.example.tools

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encryption used for backups. Pure JVM code (no Android types) so it is covered by unit tests.
 *
 * Password-protected backup file layout:
 * `HNTB` magic (4) | format version (1) | PBKDF2 iterations (4) | salt (16) | GCM IV (12) | ciphertext+tag
 *
 * AES-256-GCM authenticates the content, so a wrong password or a modified file is detected
 * instead of silently restoring garbage.
 */
object BackupCrypto {
    private val MAGIC = byteArrayOf('H'.code.toByte(), 'N'.code.toByte(), 'T'.code.toByte(), 'B'.code.toByte())
    private const val FILE_VERSION: Byte = 1
    const val PBKDF2_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val HEADER_BYTES = 4 + 1 + 4 + SALT_BYTES + IV_BYTES

    /** Minimum length of a backup password. */
    const val MIN_PASSWORD_LENGTH = 6

    private val random = SecureRandom()

    fun isPasswordEncrypted(bytes: ByteArray): Boolean =
        bytes.size > HEADER_BYTES && MAGIC.indices.all { bytes[it] == MAGIC[it] }

    fun encryptWithPassword(plain: ByteArray, password: String, iterations: Int = PBKDF2_ITERATIONS): ByteArray {
        require(password.isNotEmpty()) { "password is empty" }
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(MAGIC)
        val encrypted = cipher.doFinal(plain)
        return ByteBuffer.allocate(HEADER_BYTES + encrypted.size)
            .put(MAGIC)
            .put(FILE_VERSION)
            .putInt(iterations)
            .put(salt)
            .put(iv)
            .put(encrypted)
            .array()
    }

    /** Returns the plain content, or null when the password is wrong or the file was changed. */
    fun decryptWithPassword(bytes: ByteArray, password: String): ByteArray? {
        if (!isPasswordEncrypted(bytes) || password.isEmpty()) return null
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(MAGIC.size)
        val version = buffer.get()
        if (version != FILE_VERSION) return null
        val iterations = buffer.int
        // Refuse absurd values from a tampered header instead of hanging the phone.
        if (iterations !in 10_000..2_000_000) return null
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(MAGIC)
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    /** AES-GCM with an existing key (used with the Android Keystore key of automatic backups). */
    fun encryptWithKey(plain: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return ByteBuffer.allocate(1 + iv.size + encrypted.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()
    }

    fun decryptWithKey(bytes: ByteArray, key: SecretKey): ByteArray? = try {
        val ivSize = bytes[0].toInt()
        require(ivSize in 12..16)
        val iv = bytes.copyOfRange(1, 1 + ivSize)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.doFinal(bytes, 1 + ivSize, bytes.size - 1 - ivSize)
    } catch (e: Exception) {
        null
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try {
            // PBKDF2WithHmacSHA1 is available on every supported Android version (API 24+).
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
