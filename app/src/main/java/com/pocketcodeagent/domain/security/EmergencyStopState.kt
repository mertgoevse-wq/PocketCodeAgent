package com.pocketcodeagent.domain.security

enum class EmergencyStopState(val label: String, val description: String) {
    NORMAL("Normal", "Alle Funktionen aktiv"),
    API_CALLS_DISABLED("API deaktiviert", "Keine echten API-Requests"),
    TERMINAL_DISABLED("Terminal deaktiviert", "Kein Terminal-Zugriff"),
    EXPORTS_DISABLED("Export deaktiviert", "Kein Export/Share"),
    ALL_DISABLED("Alle deaktiviert", "Alle sensiblen Aktionen gesperrt")
}
