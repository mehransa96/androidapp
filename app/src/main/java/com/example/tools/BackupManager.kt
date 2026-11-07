package com.example.tools

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import com.example.data.model.CycleRecord
import com.example.data.model.DailyLog
import com.example.data.model.HealthEntry
import com.example.data.model.PhysicalSymptomLog
import com.example.data.preferences.WellnessPrefs
import com.example.security.AppLockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Local backup to a file the user picks (Storage Access Framework), optionally encrypted with a
 * password, restore from it, and a full local wipe. Nothing leaves the device unless the user
 * moves the file themselves. The app-lock PIN is never written to the backup.
 */
object BackupManager {
    const val FORMAT = "hanita-backup"
    const val VERSION = 1
    private const val USER_PREFS = "user_prefs"

    data class Summary(val logs: Int, val cycles: Int, val symptoms: Int, val healthEntries: Int)

    fun suggestedFileName(encrypted: Boolean = false): String =
        if (encrypted) "hanita-backup-${LocalDate.now()}.hanita" else "hanita-backup-${LocalDate.now()}.json"

    /** Every record and setting of the app as one JSON document (the app-lock PIN is never included). */
    suspend fun buildJson(context: Context): Pair<JSONObject, Summary> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val logs = db.dailyLogDao().getAllLogsList()
        val cycles = db.cycleRecordDao().getAllCycleRecordsList()
        val symptoms = db.physicalSymptomDao().getAllSymptomsList()
        val entries = db.healthEntryDao().getAllList()

        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("dailyLogs", JSONArray(logs.map { it.toJson() }))
            .put("cycleRecords", JSONArray(cycles.map { it.toJson() }))
            .put("physicalSymptoms", JSONArray(symptoms.map { it.toJson() }))
            .put("healthEntries", JSONArray(entries.map { it.toJson() }))
            .put("userPrefs", prefsToJson(context, USER_PREFS))
            .put("wellnessPrefs", prefsToJson(context, WellnessPrefs.NAME))
            .put("partnerPrefs", prefsToJson(context, PartnerPrefs.NAME))
        root to Summary(logs.size, cycles.size, symptoms.size, entries.size)
    }

    /**
     * Writes a backup file. With a [password] the content is encrypted (AES-256-GCM, key derived
     * from the password with PBKDF2), so the file is unreadable to anyone without the password.
     */
    suspend fun export(context: Context, uri: Uri, password: String? = null): Summary = withContext(Dispatchers.IO) {
        val (root, summary) = buildJson(context)
        val plain = root.toString().toByteArray(Charsets.UTF_8)
        val bytes = if (password.isNullOrEmpty()) plain else BackupCrypto.encryptWithPassword(plain, password)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("فایل مقصد باز نشد")
        stream.use { it.write(bytes) }
        summary
    }

    /** Thrown when a backup file is encrypted and no (or a wrong) password was given. */
    class PasswordRequiredException(val wrongPassword: Boolean) :
        Exception(if (wrongPassword) "رمز فایل پشتیبان اشتباه است" else "این فایل پشتیبان رمزدار است")

    /** Adds/overwrites records from the file. Existing records with other keys are kept. */
    suspend fun restore(context: Context, uri: Uri, password: String? = null): Summary = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            // A real backup is a few MB at most; refuse anything absurdly large.
            val data = input.readBytes()
            require(data.size < 50 * 1024 * 1024) { "حجم فایل بیش از حد مجاز است" }
            data
        } ?: throw IllegalStateException("فایل باز نشد")
        val plain = if (BackupCrypto.isPasswordEncrypted(bytes)) {
            if (password.isNullOrEmpty()) throw PasswordRequiredException(wrongPassword = false)
            BackupCrypto.decryptWithPassword(bytes, password) ?: throw PasswordRequiredException(wrongPassword = true)
        } else {
            bytes
        }
        applyJson(context, JSONObject(String(plain, Charsets.UTF_8)))
    }

    /** Merges a backup document into the database and settings. */
    suspend fun applyJson(context: Context, root: JSONObject): Summary = withContext(Dispatchers.IO) {
        require(root.optString("format") == FORMAT) { "این فایل پشتیبان هانیتا نیست" }
        require(root.optInt("version", 0) in 1..VERSION) { "نسخه فایل پشتیبان پشتیبانی نمی‌شود" }

        val logs = root.optJSONArray("dailyLogs").objects().mapNotNull { it.toDailyLog() }
        val cycles = root.optJSONArray("cycleRecords").objects().mapNotNull { it.toCycleRecord() }
        val symptoms = root.optJSONArray("physicalSymptoms").objects().mapNotNull { it.toSymptom() }
        val entries = root.optJSONArray("healthEntries").objects().mapNotNull { it.toHealthEntry() }

        val db = AppDatabase.getDatabase(context)
        db.dailyLogDao().insertLogs(logs)
        db.cycleRecordDao().insertCycleRecords(cycles)
        // Auto-generated ids of another install would overwrite unrelated rows here, so these
        // records get fresh ids and exact duplicates (same backup restored twice) are skipped.
        val symptomKeys = db.physicalSymptomDao().getAllSymptomsList()
            .map { listOf(it.dateStr, it.symptomName, it.severity, it.timestamp) }.toHashSet()
        val newSymptoms = symptoms.filter { symptomKeys.add(listOf(it.dateStr, it.symptomName, it.severity, it.timestamp)) }
            .map { it.copy(id = 0) }
        db.physicalSymptomDao().insertSymptoms(newSymptoms)
        val entryKeys = db.healthEntryDao().getAllList()
            .map { listOf(it.dateStr, it.type, it.value, it.num1, it.num2, it.timestamp) }.toHashSet()
        val newEntries = entries.filter { entryKeys.add(listOf(it.dateStr, it.type, it.value, it.num1, it.num2, it.timestamp)) }
            .map { it.copy(id = 0) }
        db.healthEntryDao().insertAll(newEntries)
        root.optJSONObject("userPrefs")?.let { jsonToPrefs(context, USER_PREFS, it) }
        root.optJSONObject("wellnessPrefs")?.let { jsonToPrefs(context, WellnessPrefs.NAME, it) }
        root.optJSONObject("partnerPrefs")?.let { jsonToPrefs(context, PartnerPrefs.NAME, it) }
        // The shared settings object must see the restored values without a process restart.
        com.example.data.preferences.UserPreferencesManager(context).reload()
        Summary(logs.size, cycles.size, newSymptoms.size, newEntries.size)
    }

    /** Deletes every local record, setting, automatic backup and the app lock. */
    @android.annotation.SuppressLint("ApplySharedPref") // must be on disk before the app restarts
    suspend fun wipeAll(context: Context) = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).clearAllTables()
        // The medicine-alarm store is left in place so the reschedule below can cancel old alarms.
        listOf(USER_PREFS, WellnessPrefs.NAME, PartnerPrefs.NAME, AppLockManager.NAME, "hanita_permissions")
            .forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        LocalSnapshots.deleteAll(context)
        com.example.data.preferences.UserPreferencesManager(context).reload()
        WellnessReminders.scheduleAll(context, null, 28)
        com.example.utils.ReminderScheduler.rescheduleFromStorage(context)
        CycleWidgetProvider.requestUpdate(context)
    }

    // --- JSON mapping -------------------------------------------------------------------------

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun isDate(s: String) = runCatching { LocalDate.parse(s) }.isSuccess

    private fun DailyLog.toJson() = JSONObject()
        .put("dateStr", dateStr).put("periodFlow", periodFlow).put("symptoms", symptoms)
        .put("moods", moods).put("notes", notes).put("emoji", emoji).put("takenMeds", takenMeds)
        .put("waterIntake", waterIntake).put("bbt", bbt ?: JSONObject.NULL)
        .put("weight", weight ?: JSONObject.NULL).put("updatedAt", updatedAt)

    private fun JSONObject.toDailyLog(): DailyLog? {
        val date = optString("dateStr")
        if (!isDate(date)) return null
        return DailyLog(
            dateStr = date,
            periodFlow = optString("periodFlow", "NONE").ifBlank { "NONE" },
            symptoms = optString("symptoms"),
            moods = optString("moods"),
            notes = optString("notes"),
            emoji = optString("emoji"),
            takenMeds = optString("takenMeds"),
            waterIntake = optInt("waterIntake", 0),
            bbt = optNullableDouble("bbt")?.toFloat(),
            weight = optNullableDouble("weight")?.toFloat(),
            updatedAt = optLong("updatedAt", 0L)
        )
    }

    private fun CycleRecord.toJson() = JSONObject()
        .put("id", id).put("startDate", startDate).put("endDate", endDate).put("notes", notes)

    private fun JSONObject.toCycleRecord(): CycleRecord? {
        val id = optString("id")
        if (id.isBlank()) return null
        return CycleRecord(id = id, startDate = optString("startDate"), endDate = optString("endDate"), notes = optString("notes"))
    }

    private fun PhysicalSymptomLog.toJson() = JSONObject()
        .put("id", id).put("dateStr", dateStr).put("symptomName", symptomName).put("severity", severity)
        .put("severityScore", severityScore).put("category", category).put("notes", notes).put("timestamp", timestamp)

    private fun JSONObject.toSymptom(): PhysicalSymptomLog? {
        val date = optString("dateStr")
        val name = optString("symptomName")
        if (!isDate(date) || name.isBlank()) return null
        return PhysicalSymptomLog(
            id = optLong("id", 0L),
            dateStr = date,
            symptomName = name,
            severity = optString("severity", "MODERATE"),
            severityScore = optInt("severityScore", 2).coerceIn(1, 3),
            category = optString("category", "عمومی"),
            notes = optString("notes"),
            timestamp = optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun HealthEntry.toJson() = JSONObject()
        .put("id", id).put("dateStr", dateStr).put("type", type).put("value", value)
        .put("num1", num1 ?: JSONObject.NULL).put("num2", num2 ?: JSONObject.NULL)
        .put("notes", notes).put("timestamp", timestamp)

    private fun JSONObject.toHealthEntry(): HealthEntry? {
        val date = optString("dateStr")
        val type = optString("type")
        if (!isDate(date) || type.isBlank()) return null
        return HealthEntry(
            id = optLong("id", 0L),
            dateStr = date,
            type = type,
            value = optString("value"),
            num1 = optNullableInt("num1"),
            num2 = optNullableInt("num2"),
            notes = optString("notes"),
            timestamp = optLong("timestamp", System.currentTimeMillis())
        )
    }

    /** Only primitive values and string sets are stored in our SharedPreferences. */
    private fun prefsToJson(context: Context, name: String): JSONObject {
        val out = JSONObject()
        context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
            val typed = when (value) {
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is String -> JSONObject().put("t", "s").put("v", value)
                is Set<*> -> JSONObject().put("t", "ss").put("v", JSONArray(value.filterIsInstance<String>()))
                else -> null
            }
            if (typed != null) out.put(key, typed)
        }
        return out
    }

    @android.annotation.SuppressLint("ApplySharedPref") // restored values must be on disk before the restart
    private fun jsonToPrefs(context: Context, name: String, json: JSONObject) {
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
        json.keys().forEach { key ->
            val item = json.optJSONObject(key) ?: return@forEach
            when (item.optString("t")) {
                "b" -> editor.putBoolean(key, item.optBoolean("v"))
                "i" -> editor.putInt(key, item.optInt("v"))
                "l" -> editor.putLong(key, item.optLong("v"))
                "f" -> editor.putFloat(key, item.optDouble("v").toFloat())
                "s" -> editor.putString(key, item.optString("v"))
                "ss" -> {
                    val arr = item.optJSONArray("v")
                    editor.putStringSet(key, (0 until (arr?.length() ?: 0)).map { arr!!.optString(it) }.toSet())
                }
            }
        }
        editor.commit()
    }
}
