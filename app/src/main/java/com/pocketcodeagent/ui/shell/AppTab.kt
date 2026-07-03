package com.pocketcodeagent.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
    CHAT("Chat", Icons.Default.Psychology),
    FILES("Files", Icons.Default.FolderOpen),
    CODE("Code", Icons.Default.Code),
    DIFF("Diff", Icons.Default.Compare),
    PREVIEW("Preview", Icons.Default.Web),
    TERMINAL("Terminal", Icons.Default.Terminal)
}
