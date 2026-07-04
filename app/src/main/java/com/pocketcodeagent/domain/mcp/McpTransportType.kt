package com.pocketcodeagent.domain.mcp

/**
 * Transport types supported for connecting to an MCP server.
 * Android cannot run arbitrary local MCP servers in the background.
 * Connections can be via Termux-local processes, remote servers, or desktop bridges.
 */
enum class McpTransportType(val displayName: String, val description: String) {
    HTTP("HTTP", "Connect to an MCP server via HTTP REST endpoint"),
    SSE("SSE", "Server-Sent Events stream from a remote MCP server"),
    WEBSOCKET("WebSocket", "Persistent WebSocket connection to an MCP server"),
    TERMUX_BRIDGE("Termux Bridge", "Connect to a local MCP server running in Termux"),
    CUSTOM("Custom", "Custom transport (user-configured bridge or proxy)")
}
