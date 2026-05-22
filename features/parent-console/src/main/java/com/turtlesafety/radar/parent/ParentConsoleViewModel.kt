package com.turtlesafety.radar.parent

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turtlesafety.radar.core.Radar
import com.turtlesafety.radar.core.risk.DetectionSource
import com.turtlesafety.radar.core.settings.LocalAiMode
import com.turtlesafety.radar.core.security.PinManager
import com.turtlesafety.radar.core.settings.Sensitivity
import com.turtlesafety.radar.media.MediaChecker
import com.turtlesafety.radar.notif.NotificationListenerPermission
import com.turtlesafety.radar.ime.SafetyImePermission
import com.turtlesafety.radar.parent.guard.ChecklistRepository
import com.turtlesafety.radar.parent.system.ParentNotificationPermission
import com.turtlesafety.radar.parent.system.PermissionStateMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ParentConsoleViewModel(application: Application) : AndroidViewModel(application) {

    private val services = Radar.services()
    private val permissionMonitor = PermissionStateMonitor(
        context = application,
        repository = services.detectionLogRepository,
    )
    private val mediaChecker = MediaChecker(
        context = application,
        riskEngine = services.riskEngine,
        repository = services.detectionLogRepository,
    )
    private val checklistRepository = ChecklistRepository(application)

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
                mediaCheckerEnabled = services.settingsStore.mediaCheckerEnabled,
                localAiMode = services.settingsStore.localAiMode,
                checklistState = checklistRepository.snapshot(),
                checklistProgress = checklistRepository.progress(),
            )
        }
        viewModelScope.launch {
            services.detectionLogRepository.observeRecent(limit = 200).collect { logs ->
                _state.update {
                    it.copy(
                        recentLogs = logs,
                        totalLogCount = logs.size,
                        unacknowledgedLogCount = logs.count { log -> !log.acknowledged },
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // External Guard checklist (仕様書 §6 / §17.5)
    // -----------------------------------------------------------------------

    fun setChecklistItem(itemId: String, value: Boolean) {
        checklistRepository.setChecked(itemId, value)
        _state.update {
            it.copy(
                checklistState = checklistRepository.snapshot(),
                checklistProgress = checklistRepository.progress(),
            )
        }
    }

    // -----------------------------------------------------------------------
    // Monitored apps (仕様書 §5.6)
    // -----------------------------------------------------------------------

    fun addMonitoredApp(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return
        services.settingsStore.addMonitoredApp(normalized)
        _state.update { it.copy(monitoredApps = services.settingsStore.monitoredApps) }
    }

    fun removeMonitoredApp(packageName: String) {
        services.settingsStore.removeMonitoredApp(packageName)
        _state.update { it.copy(monitoredApps = services.settingsStore.monitoredApps) }
    }

    // -----------------------------------------------------------------------
    // PIN gate
    // -----------------------------------------------------------------------

    fun setupPin(pin: String) {
        if (pin.length < PinManager.MIN_LENGTH) {
            _state.update { it.copy(gateError = "PIN は ${PinManager.MIN_LENGTH} 文字以上で入力してください") }
            return
        }
        runCatching { services.pinManager.setPin(pin) }
            .onSuccess {
                _state.update { it.copy(gateStatus = GateStatus.UNLOCKED, gateError = null) }
            }
            .onFailure { t ->
                _state.update {
                    it.copy(gateError = "PIN の保存に失敗しました: ${t.message ?: t::class.java.simpleName}")
                }
            }
    }

    fun verifyPin(pin: String) {
        runCatching { services.pinManager.verifyPin(pin) }
            .onSuccess { ok ->
                if (ok) {
                    _state.update { it.copy(gateStatus = GateStatus.UNLOCKED, gateError = null) }
                } else {
                    _state.update { it.copy(gateError = "PIN が違います") }
                }
            }
            .onFailure { t ->
                _state.update {
                    it.copy(gateError = "PIN の検証に失敗しました: ${t.message ?: t::class.java.simpleName}")
                }
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

    fun setMediaCheckerEnabled(enabled: Boolean) {
        services.settingsStore.mediaCheckerEnabled = enabled
        _state.update { it.copy(mediaCheckerEnabled = enabled) }
    }

    fun setLocalAiMode(mode: LocalAiMode) {
        services.settingsStore.localAiMode = mode
        _state.update { it.copy(localAiMode = mode) }
    }

    fun runMediaCheck(uri: Uri) {
        if (!services.settingsStore.mediaCheckerEnabled) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    mediaScanInProgress = true,
                    mediaScanError = null,
                )
            }
            runCatching {
                mediaChecker.analyze(uri)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        mediaScanInProgress = false,
                        lastMediaScanResult = result,
                        mediaScanError = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        mediaScanInProgress = false,
                        mediaScanError = error.message ?: "画像の検査に失敗しました",
                    )
                }
            }
        }
    }

    fun runSelfTest() {
        viewModelScope.launch {
            val report = StringBuilder()
            // 1. Risk engine sanity check
            runCatching {
                val a = services.riskEngine.assess(
                    text = "親には内緒でLINE教えて",
                    source = DetectionSource.IME,
                    appName = null,
                )
                report.append("リスク判定: OK (score=${a.score}, カテゴリ=${a.categories.size}件)\n")
            }.onFailure {
                report.append("リスク判定: 失敗 - ${it.message}\n")
            }
            // 2. DB write/read sanity check
            runCatching {
                val a = services.riskEngine.assess(
                    text = "[診断テスト] このログはセルフテストで作成されました",
                    source = DetectionSource.IME,
                    appName = null,
                )
                val id = services.detectionLogRepository.record(
                    assessment = a.copy(
                        score = 1,
                        reason = "セルフテスト: DB 書き込み確認",
                    ),
                    source = DetectionSource.IME,
                    appName = "self-test",
                )
                val total = services.detectionLogRepository.count()
                report.append("ログDB書き込み: OK (id=$id, 総件数=$total)\n")
            }.onFailure {
                report.append("ログDB書き込み: 失敗 - ${it.message}\n")
            }
            // 3. Settings store sanity check
            runCatching {
                val s = services.settingsStore.sensitivity
                val apps = services.settingsStore.monitoredApps.size
                report.append("設定読込: OK (感度=${s.name}, 対象アプリ=${apps}件)\n")
            }.onFailure {
                report.append("設定読込: 失敗 - ${it.message}\n")
            }
            // 4. PinManager state
            runCatching {
                val set = services.pinManager.isPinSet()
                report.append("PIN保存: ${if (set) "OK (設定済み)" else "未設定"}\n")
            }.onFailure {
                report.append("PIN保存: 失敗 - ${it.message}\n")
            }
            _state.update { it.copy(selfTestReport = report.toString().trimEnd()) }
        }
    }

    fun clearSelfTest() {
        _state.update { it.copy(selfTestReport = null) }
    }

    fun previewQuickCheck(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) {
            _state.update {
                it.copy(
                    quickCheckAssessment = null,
                    quickCheckError = "テストしたい文章を入力してください",
                )
            }
            return
        }

        val assessment = services.riskEngine.assess(
            text = normalized,
            source = DetectionSource.IME,
            appName = null,
        )
        _state.update {
            it.copy(
                quickCheckAssessment = assessment,
                quickCheckError = null,
            )
        }
    }

    fun clearQuickCheck() {
        _state.update {
            it.copy(
                quickCheckAssessment = null,
                quickCheckError = null,
            )
        }
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
                    parentNotificationsEnabled = ParentNotificationPermission.isEnabled(context),
                    accessibilityEnabled = com.turtlesafety.radar.watcher.AccessibilityWatcherPermission.isEnabled(context),
                )
            }
        }
    }

    fun openParentNotificationSettings(context: Context) =
        ParentNotificationPermission.openSettings(context)

    fun openNotificationListenerSettings(context: Context) =
        NotificationListenerPermission.openSettings(context)

    fun openImeSettings(context: Context) =
        SafetyImePermission.openImeSettings(context)

    fun showImePicker(context: Context) =
        SafetyImePermission.showPicker(context)

    fun openAccessibilitySettings(context: Context) =
        com.turtlesafety.radar.watcher.AccessibilityWatcherPermission.openSettings(context)

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
}
