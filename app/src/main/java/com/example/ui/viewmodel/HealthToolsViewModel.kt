package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.HealthEntry
import com.example.data.model.HealthEntryType
import com.example.data.preferences.UserPreferencesManager
import com.example.data.preferences.WellnessPrefs
import com.example.domain.CycleInsights
import com.example.domain.CyclePredictor
import com.example.domain.PillPack
import com.example.tools.BackupManager
import com.example.tools.DoctorReportPdf
import com.example.tools.LocalSnapshots
import com.example.utils.RescheduleWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** State and actions of the health-tools and privacy screens (kept apart from the main ViewModel). */
class HealthToolsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.healthEntryDao()
    private val wellness = WellnessPrefs(application)
    private val userPrefs = UserPreferencesManager(application)

    val entries: StateFlow<List<HealthEntry>> =
        dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val bleedingDays = db.dailyLogDao().getPeriodDays().map { logs ->
        logs.mapNotNull { runCatching { LocalDate.parse(it.dateStr) }.getOrNull() }
    }

    data class CycleState(
        val prediction: CyclePredictor.Prediction,
        val today: CyclePredictor.DayStatus,
        val insights: CycleInsights.Result
    )

    // Follows a changed cycle length setting too (it used to be read once).
    val cycle: StateFlow<CycleState?> = combine(bleedingDays, userPrefs.averageCycleLength) { days, defaultLength ->
        val today = LocalDate.now()
        val prediction = CyclePredictor.predict(days, defaultLength, today)
        CycleState(
            prediction = prediction,
            today = CyclePredictor.dayStatus(today, prediction, days.contains(today), today),
            insights = CycleInsights.analyze(days, today)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class PillSettings(
        val enabled: Boolean,
        val start: LocalDate?,
        val type: PillPack.Type,
        val hour: Int,
        val minute: Int
    )

    private val _pill = MutableStateFlow(readPill())
    val pill: StateFlow<PillSettings> = _pill.asStateFlow()

    private val _breastReminder = MutableStateFlow(wellness.breastExamReminder)
    val breastReminder: StateFlow<Boolean> = _breastReminder.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private fun readPill() = PillSettings(wellness.pillEnabled, wellness.pillStart, wellness.pillType, wellness.pillHour, wellness.pillMinute)

    // --- Daily health entries -----------------------------------------------------------------

    /** Adds an entry; for one-per-day types the previous value of that day is replaced. */
    fun addEntry(entry: HealthEntry) {
        viewModelScope.launch {
            if (entry.type in HealthEntryType.ONE_PER_DAY) dao.deleteForDay(entry.dateStr, entry.type)
            dao.insert(entry)
        }
    }

    fun clearDay(date: LocalDate, type: String) {
        viewModelScope.launch { dao.deleteForDay(date.toString(), type) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    fun setPillTaken(date: LocalDate, taken: Boolean) {
        viewModelScope.launch {
            dao.deleteForDay(date.toString(), HealthEntryType.PILL)
            if (taken) dao.insert(HealthEntry(dateStr = date.toString(), type = HealthEntryType.PILL, value = "TAKEN"))
        }
    }

    // --- Settings -----------------------------------------------------------------------------

    fun updatePill(enabled: Boolean, start: LocalDate?, type: PillPack.Type, hour: Int, minute: Int) {
        wellness.pillEnabled = enabled
        wellness.pillStart = start
        wellness.pillType = type
        wellness.pillHour = hour
        wellness.pillMinute = minute
        _pill.value = readPill()
        RescheduleWorker.runNow(getApplication())
    }

    fun setBreastReminder(enabled: Boolean) {
        wellness.breastExamReminder = enabled
        _breastReminder.value = enabled
        RescheduleWorker.runNow(getApplication())
    }

    // --- Files --------------------------------------------------------------------------------

    private fun runFileTask(success: (String) -> String = { it }, block: suspend () -> String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                _message.value = success(block())
            } catch (e: Exception) {
                _message.value = "خطا: ${e.message ?: "عملیات انجام نشد"}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun exportPdf(uri: Uri) = runFileTask {
        DoctorReportPdf.write(getApplication(), uri)
        "گزارش PDF ذخیره شد. می‌توانید آن را برای پزشک بفرستید یا چاپ کنید."
    }

    fun exportBackup(uri: Uri, password: String? = null) = runFileTask {
        val s = BackupManager.export(getApplication(), uri, password)
        val lockNote = if (password.isNullOrEmpty()) "" else " فایل با رمز شما قفل شد."
        "پشتیبان ذخیره شد: ${s.logs} روز ثبت‌شده، ${s.symptoms} علامت و ${s.healthEntries} مورد سلامت.$lockNote"
    }

    /** Set while a restore waits for the password of an encrypted backup file. */
    data class PasswordRequest(val uri: Uri, val wrongPassword: Boolean)

    private val _passwordRequest = MutableStateFlow<PasswordRequest?>(null)
    val passwordRequest: StateFlow<PasswordRequest?> = _passwordRequest.asStateFlow()
    fun cancelPasswordRequest() { _passwordRequest.value = null }

    /** [onDone] runs after a successful restore so the app can restart with the restored data. */
    fun restoreBackup(uri: Uri, password: String? = null, onDone: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _passwordRequest.value = null
        viewModelScope.launch {
            try {
                // A safety copy of the current state first, so a wrong file can be undone.
                LocalSnapshots.takeIfDue(getApplication(), force = true)
                BackupManager.restore(getApplication(), uri, password)
                onDone()
            } catch (e: BackupManager.PasswordRequiredException) {
                _passwordRequest.value = PasswordRequest(uri, e.wrongPassword)
            } catch (e: Exception) {
                _message.value = "بازیابی انجام نشد: ${e.message ?: "فایل نامعتبر است"}"
            } finally {
                _busy.value = false
            }
        }
    }

    // --- Automatic encrypted safety copies ------------------------------------------------------

    private val _snapshots = MutableStateFlow<List<LocalSnapshots.Snapshot>>(emptyList())
    val snapshots: StateFlow<List<LocalSnapshots.Snapshot>> = _snapshots.asStateFlow()

    fun refreshSnapshots() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _snapshots.value = LocalSnapshots.list(getApplication())
        }
    }

    fun takeSnapshotNow() = runFileTask {
        val ok = LocalSnapshots.takeIfDue(getApplication(), force = true)
        _snapshots.value = LocalSnapshots.list(getApplication())
        if (ok) "نسخه امن خودکار به‌روز شد." else "هنوز اطلاعاتی برای ذخیره وجود ندارد."
    }

    fun restoreSnapshot(snapshot: LocalSnapshots.Snapshot, onDone: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                LocalSnapshots.restore(getApplication(), snapshot)
                onDone()
            } catch (e: Exception) {
                _message.value = "بازیابی انجام نشد: ${e.message ?: "نسخه پشتیبان خوانا نیست"}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun wipeAll(onDone: () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                BackupManager.wipeAll(getApplication())
                onDone()
            } catch (e: Exception) {
                _message.value = "حذف داده‌ها انجام نشد: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }
}
