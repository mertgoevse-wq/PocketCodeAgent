package com.pocketcodeagent.domain.workspace

import com.pocketcodeagent.data.model.WorkspaceFile

object WorkspacePathHelper {
    fun findRelativePathByUri(files: List<WorkspaceFile>, fileUri: String?): String? {
        if (fileUri.isNullOrBlank()) return null
        return findRelativePathByUriInternal(files, fileUri, prefix = "")
    }

    fun safeRelativePath(
        files: List<WorkspaceFile>,
        fileUri: String?,
        openFileRelativePath: String?,
        fallbackFileName: String?
    ): String {
        return sanitizeRelativePath(openFileRelativePath)
            ?: sanitizeRelativePath(findRelativePathByUri(files, fileUri))
            ?: sanitizeRelativePath(fallbackFileName)
            ?: "unknown"
    }

    private fun findRelativePathByUriInternal(
        files: List<WorkspaceFile>,
        fileUri: String,
        prefix: String
    ): String? {
        for (file in files) {
            val path = if (prefix.isBlank()) file.name else "$prefix/${file.name}"
            if (file.uriString == fileUri) return path
            if (file.isDirectory) {
                findRelativePathByUriInternal(file.children, fileUri, path)?.let { return it }
            }
        }
        return null
    }

    private fun sanitizeRelativePath(path: String?): String? {
        val normalized = path
            ?.trim()
            ?.replace('\\', '/')
            ?.trim('/')
            ?: return null

        if (normalized.isBlank()) return null
        if (normalized.startsWith("content:", ignoreCase = true)) return null
        if (normalized.startsWith("file:", ignoreCase = true)) return null
        if (normalized.startsWith("/")) return null
        if (Regex("^[A-Za-z]:").containsMatchIn(normalized)) return null

        val parts = normalized.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        if (parts.any { it == "." || it == ".." }) return null

        return parts.joinToString("/")
    }
}
