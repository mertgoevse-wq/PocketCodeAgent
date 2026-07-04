package com.pocketcodeagent.domain.mcp

/**
 * Security policy for MCP tool execution.
 *
 * This policy is applied per-tool before any execution is allowed.
 * All tools are blocked by default — the user must explicitly approve
 * each server and each tool category.
 */
data class McpPermissionPolicy(
    val requireConfirmationForAllTools: Boolean = true,
    val allowReadOnlyTools: Boolean = false,
    val blockDestructiveTools: Boolean = true,
    val blockNetworkExfiltration: Boolean = true,
    val allowedWorkspaceOnly: Boolean = true
) {
    /** Returns true if the tool is allowed to run without additional confirmation. */
    fun isAutoApproved(tool: McpTool): Boolean = false // Never auto-approve in MVP

    /** Returns true if the tool is completely blocked regardless of confirmation. */
    fun isBlocked(tool: McpTool): Boolean = when {
        blockDestructiveTools && tool.riskLevel == McpToolRiskLevel.DESTRUCTIVE -> true
        blockNetworkExfiltration && tool.riskLevel == McpToolRiskLevel.NETWORK -> true
        allowedWorkspaceOnly && tool.riskLevel == McpToolRiskLevel.FILESYSTEM -> false // allowed if workspace-scoped
        else -> false
    }

    companion object {
        val DEFAULT_LENIENT = McpPermissionPolicy(
            requireConfirmationForAllTools = true,
            allowReadOnlyTools = true,
            blockDestructiveTools = true,
            blockNetworkExfiltration = true,
            allowedWorkspaceOnly = true
        )

        val DEFAULT_STRICT = McpPermissionPolicy(
            requireConfirmationForAllTools = true,
            allowReadOnlyTools = false,
            blockDestructiveTools = true,
            blockNetworkExfiltration = true,
            allowedWorkspaceOnly = true
        )
    }
}
