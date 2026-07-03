package com.pocketcodeagent.domain.preview

sealed class PreviewTarget {
    data object None : PreviewTarget()
    data object WorkspaceStatic : PreviewTarget()
    data class File(val path: String, val fileName: String) : PreviewTarget()
    data class Url(val url: String) : PreviewTarget()

    fun displayText(): String = when (this) {
        is None -> "Kein Ziel"
        is WorkspaceStatic -> "Workspace (index.html)"
        is File -> fileName
        is Url -> url
    }
}
