package com.pocketcodeagent.domain.security

enum class SensitiveAction(val displayName: String, val description: String) {
    VIEW_API_KEY("API-Key anzeigen", "Zeigt den gespeicherten API-Key im Klartext an"),
    DELETE_PROVIDER("Provider loeschen", "Entfernt einen Provider unwiderruflich"),
    EXPORT_WORKSPACE("Workspace exportieren", "Exportiert Workspace als ZIP-Datei"),
    SHARE_FILE("Datei teilen", "Teilt Dateiinhalte per Intent"),
    COPY_CAUTION_COMMAND("CAUTION Command kopieren", "Kopiert ein als CAUTION markiertes Shell-Command"),
    CONFIRM_DELETE_PATCH("Delete Patch bestaetigen", "Bestaetigt das Loeschen einer Datei per Patch"),
    APPLY_DESTRUCTIVE_PATCH("Destruktiven Patch anwenden", "Wendet einen DELETE-Patch an"),
    CLEAR_SESSION("Session loeschen", "Loescht alle Chat-Nachrichten und Commands"),
    CHANGE_EMERGENCY_STOP("Emergency Stop aendern", "Aendert den Emergency-Stop-Status")
}

object SensitiveActionGuard {
    fun requiresOwnerCheck(action: SensitiveAction): Boolean = true

    fun isBlockedByEmergencyStop(
        action: SensitiveAction,
        stopState: EmergencyStopState
    ): Boolean {
        return when (stopState) {
            EmergencyStopState.NORMAL -> false
            EmergencyStopState.API_CALLS_DISABLED -> false
            EmergencyStopState.TERMINAL_DISABLED -> action == SensitiveAction.COPY_CAUTION_COMMAND
            EmergencyStopState.EXPORTS_DISABLED -> action in setOf(
                SensitiveAction.EXPORT_WORKSPACE,
                SensitiveAction.SHARE_FILE
            )
            EmergencyStopState.ALL_DISABLED -> true
        }
    }
}
