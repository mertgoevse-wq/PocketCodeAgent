package com.pocketcodeagent.domain.security

sealed class OwnerAuthState {
    data object NotConfigured : OwnerAuthState()
    data object Locked : OwnerAuthState()
    data object Unlocked : OwnerAuthState()
    data class Failed(val reason: String) : OwnerAuthState()
    data object TemporarilyBlocked : OwnerAuthState()
}
