package com.turtlesafety.radar.parent

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turtlesafety.radar.core.Radar
import com.turtlesafety.radar.core.security.PinManager
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.guard.ChecklistCategory
import com.turtlesafety.radar.guard.ChecklistRepository
import com.turtlesafety.radar.notif.NotificationListenerPermission
import com.turtlesafety.radar.ime.SafetyImePermission
import com.turtlesafety.radar.parent.system.PermissionStateMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ParentConsoleViewModel(application: Application) : AndroidViewModel(application) {

    private val services = Radar.services()
    private val checklistRepo = ChecklistRepository.create(application)
    private val permissionMonitor = PermissionStateMonitor(
        context = application,
        repository = services.detectionLogRepository,
    )

    private val _state = MutableStateFlow(ParentConsoleState())
    val state: StateFlow<ParentConsoleState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        _state.update {
            it.copy(
                gateStatus = if (services.pinManager.isPinSet()) GateStatus.LOCKED else GateStatus.UNINITIALIZED,
                sensitivity = services.settingsStore.sensitivity,
                monitoredApps = services.settingsStore.monitoredApps,
                checklistTotal = checklistRepo.progress().total,
                checklistCheckedCount = checklistRepo.progress().checked,
            )
        }
        viewModelScope.launch {
            services.detectionLogRepository.observeRecent(limit = 200).collect { logs ->
                _state.update { it.copy(recentLogs = logs, totalLogCount = logs.size) }
            }
        }
    }

    // -----------------------------------------------------------------------
    // PIN gate
    // -----------------------------------------------------------------------

    fun setupPin(pin: String) {
        if (pin.length < PinManager.MIN_LENGTH) {
            _state.update { it.copy(gateError = "PIN は ${PinManager.MIN_LENGTH} 文字以上で入力してください") }
            return
        }
        services.pinManager.setPin(pin)
        _state.update { it.copy(gateStatus = GateStatus.UNLOCKED, gateError = null) }
    }

    fun verifyPin(pin: String) {
        if (services.pinManager.verifyPin(pin)) {
            _state.update { it.copy(gateStatus = GateStatus.UNLOCKED, gateError = null) }
        } else {
            _state.update { it.copy(gateError = "PIN が違います") }
        }
    }

    fun lock() {
        _state.update { it.copy(gateStatus = GateStatus.LOCKED, gateError = null) }
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    fun navigate(screen: Screen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    // -----------------------------------------------------------------------
    // Settings
    // -----------------------------------------------------------------------

    fun setSensitivity(sensitivity: Sensitivity) {
        services.settingsStore.sensitivity = sensitivity
        _state.update { it.copy(sensitivity = sensitivity) }
    }

    fun toggleMonitored(packageName: String) {
        val current = services.settingsStore.monitoredApps
        val updated = if (packageName in current) current - packageName else current + packageName
        services.settingsStore.monitoredApps = updated
        _state.update { it.copy(monitoredApps = updated) }
    }

    // -----------------------------------------------------------------------
    // Permissions / status
    // -----------------------------------------------------------------------

    fun refreshPermissions(context: Context) {
        viewModelScope.launch {
            // 直近の保存状態と比較して、無効化があれば SYSTEM ログを残す。
            permissionMonitor.snapshotAndReport()
            _state.update {
                it.copy(
                    notificationListenerEnabled = NotificationListenerPermission.isRadarListenerGranted(context),
                    safetyImeEnabled = SafetyImePermission.isEnabled(context),
                    safetyImeIsDefault = SafetyImePermission.isDefault(context),
                )
            }
        }
    }

    fun openNotificationListenerSettings(context: Context) =
        NotificationListenerPermission.openSettings(context)

    fun openImeSettings(context: Context) =
        SafetyImePermission.openImeSettings(context)

    fun showImePicker(context: Context) =
        SafetyImePermission.showPicker(context)

    // -----------------------------------------------------------------------
    // Logs
    // -----------------------------------------------------------------------

    fun clearAllLogs() {
        viewModelScope.launch {
            services.detectionLogRepository.clearAll()
        }
    }

    fun acknowledge(id: Long) {
        viewModelScope.launch { services.detectionLogRepository.acknowledge(id) }
    }

    // -----------------------------------------------------------------------
    // Checklist
    // -----------------------------------------------------------------------

    fun checklistSnapshot(): Map<ChecklistCategory, List<ChecklistRepository.ChecklistEntry>> =
        checklistRepo.snapshot()

    fun setChecklistItem(id: String, checked: Boolean) {
        checklistRepo.setChecked(id, checked)
        val progress = checklistRepo.progress()
        _state.update {
            it.copy(
                checklistRevision = it.checklistRevision + 1,
                checklistCheckedCount = progress.checked,
                checklistTotal = progress.total,
            )
        }
    }
}
