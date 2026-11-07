package com.example

import com.example.data.preferences.UserPreferencesManager
import com.example.security.AppLockManager
import com.example.tools.BackupCrypto
import com.example.tools.SnapshotRetention
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import javax.crypto.KeyGenerator

class LocalStorageSecurityTest {

    private val content = """{"format":"hanita-backup","note":"سلام هانیتا"}""".toByteArray(Charsets.UTF_8)

    // Fewer PBKDF2 rounds keep the tests fast; the format stores the count, so decryption still works.
    private val fastIterations = 10_000

    @Test
    fun `password backup round-trips`() {
        val encrypted = BackupCrypto.encryptWithPassword(content, "رمز-امن-۱۲۳", fastIterations)
        assertTrue(BackupCrypto.isPasswordEncrypted(encrypted))
        assertArrayEquals(content, BackupCrypto.decryptWithPassword(encrypted, "رمز-امن-۱۲۳"))
    }

    @Test
    fun `password backup does not contain the plain text`() {
        val encrypted = BackupCrypto.encryptWithPassword(content, "secret1", fastIterations)
        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("hanita-backup"))
    }

    @Test
    fun `wrong password or modified file is rejected`() {
        val encrypted = BackupCrypto.encryptWithPassword(content, "secret1", fastIterations)
        assertNull(BackupCrypto.decryptWithPassword(encrypted, "secret2"))
        val tampered = encrypted.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNull(BackupCrypto.decryptWithPassword(tampered, "secret1"))
    }

    @Test
    fun `plain json backups are not mistaken for encrypted ones`() {
        assertFalse(BackupCrypto.isPasswordEncrypted(content))
        assertNull(BackupCrypto.decryptWithPassword(content, "x"))
    }

    @Test
    fun `key based encryption round-trips`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val encrypted = BackupCrypto.encryptWithKey(content, key)
        assertArrayEquals(content, BackupCrypto.decryptWithKey(encrypted, key))
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        assertNull(BackupCrypto.decryptWithKey(encrypted, otherKey))
    }

    @Test
    fun `retention keeps newest per day for the last days and the fullest copy`() {
        val base = LocalDate.of(2026, 11, 1)
        val full = SnapshotRetention.Item(base, 1_000, records = 500)
        val items = mutableListOf(full)
        // Ten later days, two copies each; the later copy of each day is newer.
        for (d in 1..10) {
            val date = base.plusDays(d.toLong())
            items += SnapshotRetention.Item(date, d * 10_000L, records = 3)
            items += SnapshotRetention.Item(date, d * 10_000L + 5, records = 4)
        }
        val kept = SnapshotRetention.select(items, days = 7)
        assertEquals(8, kept.size)
        assertTrue(full in kept) // the real data survives even though it is 10 days old
        val keptDates = kept.filter { it != full }.map { it.date }.toSet()
        assertEquals((4..10).map { base.plusDays(it.toLong()) }.toSet(), keptDates)
        assertTrue(kept.filter { it != full }.all { it.records == 4 })
    }

    @Test
    fun `retention of an empty list is empty`() {
        assertTrue(SnapshotRetention.select(emptyList(), 7).isEmpty())
    }

    @Test
    fun `recovery code input is normalized`() {
        assertEquals("ABCD2345WXYZ", AppLockManager.normalizeRecoveryCode(" abcd-۲۳۴۵ wxyz "))
        assertEquals("ABCD-EFGH-JKMN", AppLockManager.formatRecoveryCode("ABCDEFGHJKMN"))
    }

    @Test
    fun `weights typed with Persian digits are understood`() {
        assertEquals("62.5", AppLockManager.normalizeDecimal("۶۲٫۵"))
        assertEquals("70.25", AppLockManager.normalizeDecimal("70,25"))
        assertEquals("58", AppLockManager.normalizeDecimal(" 58 kg"))
    }

    @Test
    fun `sign-up names are cleaned`() {
        assertEquals("مریم رضایی", UserPreferencesManager.sanitizeName("  مریم    رضایی "))
        assertEquals("", UserPreferencesManager.sanitizeName("   "))
        assertEquals(UserPreferencesManager.MAX_NAME_LENGTH, UserPreferencesManager.sanitizeName("ا".repeat(80)).length)
    }
}
