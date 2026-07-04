package com.pocketcodeagent.domain.mcp

enum class McpConnectionState(val label: String, val description: String) {
    DISABLED("Disabled", "Server is disabled by user"),
    NOT_CONFIGURED("Not Configured", "Server has no endpoint or name set"),
    CONNECTING("Connecting", "Attempting to connect to the MCP server"),
    CONNECTED("Connected", "Successfully connected and tools available"),
    ERROR("Error", "Connection failed — check endpoint and network")
}
