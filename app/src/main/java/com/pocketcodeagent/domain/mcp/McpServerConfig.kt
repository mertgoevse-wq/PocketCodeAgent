package com.pocketcodeagent.domain.mcp

import java.util.UUID

/**
 * Configuration for a single MCP (Model Context Protocol) server.
 *
 * No clear-text secrets are stored directly — authAlias references a Keystore entry.
 * For this phase, configs are kept in-memory only (no Room persistence).
 */
data class McpServerConfig(
    val id: String = "mcp-${UUID.randomUUID().toString().take(8)}",
    val name: String = "",
    val endpointUrl: String = "",
    val transportType: McpTransportType = McpTransportType.HTTP,
    val enabled: Boolean = false,
    val requiresAuth: Boolean = false,
    val authAlias: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isConfigured: Boolean get() = name.isNotBlank() && endpointUrl.isNotBlank()

    val displaySummary: String
        get() = when {
            !isConfigured -> "Not configured"
            !enabled -> "$name — Disabled"
            else -> "$name — ${transportType.displayName}"
        }
}
