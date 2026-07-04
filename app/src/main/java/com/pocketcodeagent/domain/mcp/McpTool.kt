package com.pocketcodeagent.domain.mcp

import java.util.UUID

/**
 * A single tool exposed by an MCP server.
 *
 * Tools are never auto-executed. Every tool invocation requires explicit
 * user confirmation based on McpPermissionPolicy.
 */
data class McpTool(
    val id: String = "mcp-tool-${UUID.randomUUID().toString().take(8)}",
    val serverId: String,
    val serverName: String,
    val name: String,
    val description: String = "",
    val inputSchemaSummary: String = "",
    val riskLevel: McpToolRiskLevel = McpToolRiskLevel.READ_ONLY
)

enum class McpToolRiskLevel(val label: String, val description: String) {
    READ_ONLY("Read-Only", "Tool queries or reads data without side effects"),
    SAFE_MUTATION("Safe Mutation", "Tool modifies local data safely"),
    DESTRUCTIVE("Destructive", "Tool can delete data or execute commands"),
    NETWORK("Network", "Tool makes outbound network requests"),
    FILESYSTEM("Filesystem", "Tool reads or writes to the local filesystem"),
    UNKNOWN("Unknown", "Tool risk cannot be determined from schema")
}
