package com.example.tools

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Automatic, encrypted safety copies of all user data, kept in the app's private storage.
 *
 * - One snapshot per day (the newest of the day replaces the older one), the last [KEEP] days are kept.
 * - Each file is encrypted with AES-256-GCM using a key that lives in the Android Keystore
 *   (hardware-backed on most phones) and can never be read out of the device.
 * - They protect against a damaged database, a failed upgrade or a mistaken restore: the user can
 *   go back to any of the last days from the privacy screen, and the app offers it by itself when
 *   the database is found empty for a registered user.
 */
object LocalSnapshots {
    private const val TAG = "LocalSnapshots"
    private const val DIR = "safety_snapshots"
    private const val KEY_ALIAS = "hanita_snapshot_key_v1"
    private const val KEEP = 7
    private const val PREFS = "hanita_snapshots"
    private const val KEY_LAST = "last_snapshot_at"
    /** Do not write more often than this when the app simply goes to background. */
    private const val MIN_INTERVAL_MS = 3 * 60 * 60 * 1000L

    private val mutex = Mutex()

    /** [records] = number of stored rows, kept in the file name so pruning never needs to decrypt. */
    data class Snapshot(val file: File, val date: LocalDate, val createdAt: Long, val records: Int)

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    fun lastSnapshotAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST, 0L)

    /** Writes a snapshot unless one was written recently (or [force]). Never throws. */
    suspend fun takeIfDue(context: Context, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val now = System.currentTimeMillis()
                if (!force && now - lastSnapshotAt(context) < MIN_INTERVAL_MS) return@withLock false
                val (root, summary) = BackupManager.buildJson(context)
                // An empty database is never written over good copies (e.g. right after a wipe or corruption).
                if (summary.logs + summary.cycles + summary.symptoms + summary.healthEntries == 0) return@withLock false
                val key = getOrCreateKey() ?: return@withLock false
                val bytes = BackupCrypto.encryptWithKey(root.toString().toByteArray(Charsets.UTF_8), key)
                val records = summary.logs + summary.cycles + summary.symptoms + summary.healthEntries
                val name = "snapshot-${LocalDate.now()}-$now-$records"
                val tmp = File(dir(context), "$name.tmp")
                tmp.writeBytes(bytes)
                // Written under a temporary name first: a crash while writing never leaves a half-written snapshot.
                if (!tmp.renameTo(File(dir(context), "$name.bin"))) {
                    tmp.delete()
                    return@withLock false
                }
                prune(context)
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST, now).apply()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Snapshot failed: ${e.javaClass.simpleName}")
                false
            }
        }
    }

    /** Fire-and-forget version for lifecycle callbacks; finishes even if the screen is closed. */
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun takeInBackground(context: Context) {
        val app = context.applicationContext
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) { takeIfDue(app) }
    }

    fun list(context: Context): List<Snapshot> =
        dir(context).listFiles { f -> f.name.startsWith("snapshot-") && f.name.endsWith(".bin") }
            .orEmpty()
            .mapNotNull { f -> parse(f) }
            .sortedByDescending { it.createdAt }

    /** Reads a snapshot; null if it cannot be decrypted (for example after moving to a new phone). */
    suspend fun read(snapshot: Snapshot): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val key = getOrCreateKey(createIfMissing = false) ?: return@withContext null
            val plain = BackupCrypto.decryptWithKey(snapshot.file.readBytes(), key) ?: return@withContext null
            JSONObject(String(plain, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.w(TAG, "Snapshot unreadable: ${e.javaClass.simpleName}")
            null
        }
    }

    /** Number of saved days and records in a snapshot, for showing before a restore. */
    fun recordCount(root: JSONObject): Int =
        listOf("dailyLogs", "cycleRecords", "physicalSymptoms", "healthEntries").sumOf { root.optJSONArray(it)?.length() ?: 0 }

    suspend fun restore(context: Context, snapshot: Snapshot): BackupManager.Summary {
        val root = read(snapshot) ?: throw IllegalStateException("این نسخه پشتیبان خوانا نیست")
        return BackupManager.applyJson(context, root)
    }

    fun deleteAll(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun formatDate(snapshot: Snapshot): String {
        val local = Instant.ofEpochMilli(snapshot.createdAt).atZone(ZoneId.systemDefault())
        val jalali = com.example.utils.JalaliCalendar.gregorianToJalali(local.toLocalDate()).formatLong()
        return "%s، ساعت %02d:%02d".format(jalali, local.hour, local.minute)
    }

    /** File name: snapshot-YYYY-MM-DD-<epochMillis>-<records>.bin */
    private fun parse(f: File): Snapshot? {
        val parts = f.name.removePrefix("snapshot-").removeSuffix(".bin").split("-")
        if (parts.size != 5) return null
        val date = runCatching { LocalDate.parse("${parts[0]}-${parts[1]}-${parts[2]}") }.getOrNull() ?: return null
        val createdAt = parts[3].toLongOrNull() ?: return null
        val records = parts[4].toIntOrNull() ?: return null
        return Snapshot(f, date, createdAt, records)
    }

    /**
     * Keeps the newest copy of each of the last [KEEP] days, and always the largest copy ever made, so
     * a nearly empty state (e.g. after a reset and a new sign-up) can never push out the real data.
     */
    private fun prune(context: Context) {
        val all = list(context)
        val keep = SnapshotRetention.select(all.map { SnapshotRetention.Item(it.date, it.createdAt, it.records) }, KEEP)
        all.filterNot { SnapshotRetention.Item(it.date, it.createdAt, it.records) in keep }.forEach { it.file.delete() }
        dir(context).listFiles { f -> f.name.endsWith(".tmp") }?.forEach { it.delete() }
    }

    private fun getOrCreateKey(createIfMissing: Boolean = true): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        if (!createIfMissing) return null
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
