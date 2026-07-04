package com.pocketcodeagent

import com.pocketcodeagent.domain.security.EmergencyStopState
import com.pocketcodeagent.domain.security.SensitiveAction
import com.pocketcodeagent.domain.security.SensitiveActionGuard
import org.junit.Assert.*
import org.junit.Test

class SensitiveActionTest {

    @Test
    fun `Normal allows all actions`() {
        for (action in SensitiveAction.entries) {
            assertFalse(
                "${action.displayName} should not be blocked in NORMAL",
                SensitiveActionGuard.isBlockedByEmergencyStop(action, EmergencyStopState.NORMAL)
            )
        }
    }

    @Test
    fun `API_CALLS_DISABLED blocks no sensitive actions`() {
        for (action in SensitiveAction.entries) {
            assertFalse(
                "${action.displayName} should not be blocked by API_CALLS_DISABLED",
                SensitiveActionGuard.isBlockedByEmergencyStop(action, EmergencyStopState.API_CALLS_DISABLED)
            )
        }
    }

    @Test
    fun `TERMINAL_DISABLED blocks COPY_CAUTION_COMMAND`() {
        assertTrue(
            SensitiveActionGuard.isBlockedByEmergencyStop(
                SensitiveAction.COPY_CAUTION_COMMAND,
                EmergencyStopState.TERMINAL_DISABLED
            )
        )
    }

    @Test
    fun `TERMINAL_DISABLED allows non-terminal actions`() {
        val allowed = SensitiveAction.entries.filter { it != SensitiveAction.COPY_CAUTION_COMMAND }
        for (action in allowed) {
            assertFalse(
                "${action.displayName} should be allowed in TERMINAL_DISABLED",
                SensitiveActionGuard.isBlockedByEmergencyStop(action, EmergencyStopState.TERMINAL_DISABLED)
            )
        }
    }

    @Test
    fun `EXPORTS_DISABLED blocks EXPORT_WORKSPACE`() {
        assertTrue(
            SensitiveActionGuard.isBlockedByEmergencyStop(
                SensitiveAction.EXPORT_WORKSPACE,
                EmergencyStopState.EXPORTS_DISABLED
            )
        )
    }

    @Test
    fun `EXPORTS_DISABLED blocks SHARE_FILE`() {
        assertTrue(
            SensitiveActionGuard.isBlockedByEmergencyStop(
                SensitiveAction.SHARE_FILE,
                EmergencyStopState.EXPORTS_DISABLED
            )
        )
    }

    @Test
    fun `EXPORTS_DISABLED allows non-export actions`() {
        val allowed = SensitiveAction.entries.filter {
            it != SensitiveAction.EXPORT_WORKSPACE && it != SensitiveAction.SHARE_FILE
        }
        for (action in allowed) {
            assertFalse(
                "${action.displayName} should be allowed in EXPORTS_DISABLED",
                SensitiveActionGuard.isBlockedByEmergencyStop(action, EmergencyStopState.EXPORTS_DISABLED)
            )
        }
    }

    @Test
    fun `ALL_DISABLED blocks all sensitive actions`() {
        for (action in SensitiveAction.entries) {
            assertTrue(
                "${action.displayName} should be blocked in ALL_DISABLED",
                SensitiveActionGuard.isBlockedByEmergencyStop(action, EmergencyStopState.ALL_DISABLED)
            )
        }
    }

    @Test
    fun `all actions require owner check`() {
        for (action in SensitiveAction.entries) {
            assertTrue(
                "${action.displayName} should require owner check",
                SensitiveActionGuard.requiresOwnerCheck(action)
            )
        }
    }
}
