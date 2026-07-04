package com.pocketcodeagent.domain.security

import android.content.Context
import android.content.SharedPreferences
import com.pocketcodeagent.data.local.KeystoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

class OwnerSecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("owner_security_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoLockJob: Job? = null

    private val _authState = MutableStateFlow<OwnerAuthState>(OwnerAuthState.NotConfigured)
    val authState: StateFlow<OwnerAuthState> = _authState

    private val _emergencyStop = MutableStateFlow(EmergencyStopState.NORMAL)
    val emergencyStop: StateFlow<EmergencyStopState> = _emergencyStop

    private var failedAttempts = 0
    private val maxFailedAttempts = 5

    init {
        loadPersistedState()
        if (hasPinConfigured() && _authState.value !is OwnerAuthState.TemporarilyBlocked) {
            _authState.value = OwnerAuthState.Locked
        }
    }

    fun hasPinConfigured(): Boolean {
        return prefs.getString(PREF_PIN_HASH, null) != null
    }

    fun setPin(pin: String) {
        if (pin.length < 4) return
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        val encodedSalt = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        prefs.edit()
            .putString(PREF_PIN_SALT, KeystoreHelper.encrypt(encodedSalt))
            .putString(PREF_PIN_HASH, hash)
            .apply()
        _authState.value = OwnerAuthState.Unlocked
        scheduleAutoLock()
    }

    fun attemptPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val storedHash = prefs.getString(PREF_PIN_HASH, null) ?: return false
        val storedSalt = try {
            val decoded = KeystoreHelper.decrypt(prefs.getString(PREF_PIN_SALT, "") ?: "")
            if (decoded.isEmpty()) null
            else android.util.Base64.decode(decoded, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
        if (storedSalt == null) return false

        val computedHash = hashPin(pin, storedSalt)

        if (computedHash == storedHash) {
            failedAttempts = 0
            _authState.value = OwnerAuthState.Unlocked
            scheduleAutoLock()
            return true
        } else {
            failedAttempts++
            if (failedAttempts >= maxFailedAttempts) {
                _authState.value = OwnerAuthState.TemporarilyBlocked
                scheduleUnblock()
            } else {
                _authState.value = OwnerAuthState.Failed(
                    "Falsche PIN. ${maxFailedAttempts - failedAttempts} Versuche uebrig."
                )
            }
            return false
        }
    }

    fun unlockDirectly() {
        _authState.value = OwnerAuthState.Unlocked
        scheduleAutoLock()
    }

    fun resetPinSetup() {
        prefs.edit()
            .remove(PREF_PIN_HASH)
            .remove(PREF_PIN_SALT)
            .apply()
        failedAttempts = 0
        _authState.value = OwnerAuthState.NotConfigured
        cancelAutoLock()
    }

    fun lock() {
        cancelAutoLock()
        if (hasPinConfigured()) {
            _authState.value = OwnerAuthState.Locked
        }
    }

    fun onActivityPaused() {
        if (_authState.value is OwnerAuthState.Unlocked) {
            lock()
        }
    }

    fun setEmergencyStop(state: EmergencyStopState) {
        _emergencyStop.value = state
        prefs.edit().putString(PREF_EMERGENCY, state.name).apply()
    }

    fun isEmergencyActive(): Boolean {
        return _emergencyStop.value != EmergencyStopState.NORMAL
    }

    fun isApiCallBlocked(): Boolean {
        return _emergencyStop.value in setOf(
            EmergencyStopState.API_CALLS_DISABLED,
            EmergencyStopState.ALL_DISABLED
        )
    }

    fun isTerminalBlocked(): Boolean {
        return _emergencyStop.value in setOf(
            EmergencyStopState.TERMINAL_DISABLED,
            EmergencyStopState.ALL_DISABLED
        )
    }

    fun isExportBlocked(): Boolean {
        return _emergencyStop.value in setOf(
            EmergencyStopState.EXPORTS_DISABLED,
            EmergencyStopState.ALL_DISABLED
        )
    }

    fun isOwnerUnlocked(): Boolean {
        return _authState.value is OwnerAuthState.Unlocked
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun scheduleAutoLock() {
        cancelAutoLock()
        autoLockJob = scope.launch {
            delay(5 * 60 * 1000L)
            lock()
        }
    }

    private fun cancelAutoLock() {
        autoLockJob?.cancel()
        autoLockJob = null
    }

    private fun scheduleUnblock() {
        scope.launch {
            delay(60 * 1000L)
            failedAttempts = 0
            if (hasPinConfigured() && _authState.value is OwnerAuthState.TemporarilyBlocked) {
                _authState.value = OwnerAuthState.Locked
            }
        }
    }

    private fun loadPersistedState() {
        val emergencyName = prefs.getString(PREF_EMERGENCY, null)
        if (emergencyName != null) {
            try {
                _emergencyStop.value = EmergencyStopState.valueOf(emergencyName)
            } catch (_: Exception) {
                _emergencyStop.value = EmergencyStopState.NORMAL
            }
        }
    }

    private companion object {
        const val PREF_PIN_HASH = "owner_pin_hash"
        const val PREF_PIN_SALT = "owner_pin_salt"
        const val PREF_EMERGENCY = "owner_emergency_stop"
    }
}
