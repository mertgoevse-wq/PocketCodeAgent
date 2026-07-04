package com.pocketcodeagent.domain.mcp

/**
 * In-memory repository for MCP server configurations.
 *
 * For this phase, configs are kept in-memory only. Room persistence
 * can be added in a future phase with proper migration handling.
 */
class McpConnectorRepository {

    private val _servers = mutableListOf<McpServerConfig>()
    private val _tools = mutableMapOf<String, List<McpTool>>() // serverId -> tools
    private val _connectionStates = mutableMapOf<String, McpConnectionState>() // serverId -> state

    fun getServers(): List<McpServerConfig> = _servers.toList()

    fun getServer(id: String): McpServerConfig? = _servers.firstOrNull { it.id == id }

    fun addServer(config: McpServerConfig) {
        _servers.add(config)
        _connectionStates[config.id] = McpConnectionState.NOT_CONFIGURED
    }

    fun updateServer(id: String, update: (McpServerConfig) -> McpServerConfig) {
        val index = _servers.indexOfFirst { it.id == id }
        if (index >= 0) {
            _servers[index] = update(_servers[index])
        }
    }

    fun removeServer(id: String) {
        _servers.removeAll { it.id == id }
        _tools.remove(id)
        _connectionStates.remove(id)
    }

    fun getConnectionState(serverId: String): McpConnectionState =
        _connectionStates[serverId] ?: McpConnectionState.NOT_CONFIGURED

    fun setConnectionState(serverId: String, state: McpConnectionState) {
        _connectionStates[serverId] = state
    }

    fun getTools(serverId: String): List<McpTool> = _tools[serverId] ?: emptyList()

    fun setTools(serverId: String, tools: List<McpTool>) {
        _tools[serverId] = tools
    }

    fun getAllTools(): List<McpTool> = _tools.values.flatten()

    /**
     * Builds the MCP context string for agent prompts.
     * Only includes enabled, configured servers with their tool lists.
     */
    fun buildAgentContext(policy: McpPermissionPolicy = McpPermissionPolicy.DEFAULT_STRICT): String {
        val enabled = _servers.filter { it.enabled && it.isConfigured }
        if (enabled.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("### Available MCP Servers and Tools")
        sb.appendLine()
        for (server in enabled) {
            sb.appendLine("**${server.name}** (${server.transportType.displayName})")
            val tools = getTools(server.id)
            if (tools.isEmpty()) {
                sb.appendLine("  - No tools discovered yet")
            } else {
                for (tool in tools) {
                    val blocked = if (policy.isBlocked(tool)) " [BLOCKED]" else ""
                    sb.appendLine("  - ${tool.name}: ${tool.description} (${tool.riskLevel.label}$blocked)")
                }
            }
            sb.appendLine()
        }
        sb.appendLine("Note: No MCP tool is executed automatically. All tools require explicit user confirmation.")
        return sb.toString()
    }
}
