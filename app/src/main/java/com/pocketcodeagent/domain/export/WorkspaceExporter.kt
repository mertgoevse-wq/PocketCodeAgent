package com.pocketcodeagent.domain.export

import android.content.Context
import android.net.Uri
import com.pocketcodeagent.data.local.WorkspaceManager
import com.pocketcodeagent.domain.context.ContextSanitizer
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WorkspaceExporter {

    // Files/folders always ignored during export
    private val alwaysIgnoreDirs = setOf(
        "node_modules", ".git", ".gradle", "build", "dist",
        ".idea", ".vscode", ".next", "out", "__pycache__"
    )

    // Files always excluded from export (security)
    private val alwaysIgnoreFiles = setOf(
        ".env", "google-services.json", "local.properties",
        "*.keystore", "*.jks", "secrets.properties", "credentials.json"
    )

    // File extensions always excluded
    private val alwaysIgnoreExtensions = setOf(
        "apk", "zip", "keystore", "jks"
    )

    // Max file size for export (50 MB) — larger files produce a warning
    private const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024

    data class ExportProgress(
        val filesProcessed: Int = 0,
        val filesTotal: Int = 0,
        val currentFile: String = "",
        val warnings: List<String> = emptyList(),
        val isComplete: Boolean = false
    )

    /**
     * Export workspace files to a ZIP output stream, skipping ignored paths and secrets.
     * @param context Android context
     * @param rootUri SAF tree URI of the workspace
     * @param outputStream destination stream (e.g. SAF create document)
     * @param onProgress optional progress callback
     * @return list of warning messages
     */
    fun exportToZip(
        context: Context,
        rootUri: Uri,
        outputStream: OutputStream,
        onProgress: ((ExportProgress) -> Unit)? = null
    ): List<String> {
        val warnings = mutableListOf<String>()
        val flatFiles = flattenFiles(context, rootUri)
        val totalFiles = flatFiles.size

        ZipOutputStream(outputStream).use { zip ->
            flatFiles.forEachIndexed { index, (relativePath, fileUri) ->
                val fileName = relativePath.substringAfterLast("/")
                val ext = fileName.substringAfterLast(".", "")

                // Filter ignored dirs
                if (relativePath.split("/").any { it in alwaysIgnoreDirs }) {
                    warnings.add("Skipped (ignored dir): $relativePath")
                    return@forEachIndexed
                }

                // Filter ignored files/extensions
                if (fileName in alwaysIgnoreFiles || ext in alwaysIgnoreExtensions) {
                    warnings.add("Skipped (sensitive): $relativePath")
                    return@forEachIndexed
                }

                // Filter secret patterns in filename
                if (ContextSanitizer.isSensitiveFileName(fileName)) {
                    warnings.add("Skipped (secret file): $relativePath")
                    return@forEachIndexed
                }

                try {
                    val content = WorkspaceManager.readFileContent(context, fileUri)
                    val sizeBytes = content.toByteArray(Charsets.UTF_8).size.toLong()

                    if (sizeBytes > MAX_FILE_SIZE_BYTES) {
                        warnings.add("Skipped (too large, ${sizeBytes / 1024} KB): $relativePath")
                        return@forEachIndexed
                    }

                    val entry = ZipEntry(relativePath)
                    zip.putNextEntry(entry)
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                } catch (e: Exception) {
                    warnings.add("Failed to read: $relativePath — ${e.message}")
                }

                onProgress?.invoke(
                    ExportProgress(
                        filesProcessed = index + 1,
                        filesTotal = totalFiles,
                        currentFile = relativePath,
                        warnings = warnings.toList()
                    )
                )
            }
        }

        onProgress?.invoke(
            ExportProgress(
                filesProcessed = totalFiles,
                filesTotal = totalFiles,
                warnings = warnings.toList(),
                isComplete = true
            )
        )

        return warnings
    }

    /**
     * Check if a filename contains secrets that should not be shared.
     */
    fun containsSecrets(content: String): Boolean {
        val sanitized = ContextSanitizer.redactSummary(content)
        return sanitized != content // Secrets found if redaction changed the text
    }

    // ─── Internal ──────────────────────────────────────────────────────────

    private data class FlatEntry(val path: String, val uri: Uri)

    private fun flattenFiles(context: Context, rootUri: Uri): List<FlatEntry> {
        val files = WorkspaceManager.listFilesRecursive(context, rootUri)
        return flattenRecursive(files, "")
    }

    private fun flattenRecursive(
        files: List<com.pocketcodeagent.data.model.WorkspaceFile>,
        prefix: String
    ): List<FlatEntry> {
        val result = mutableListOf<FlatEntry>()
        for (file in files) {
            val path = if (prefix.isEmpty()) file.name else "$prefix/${file.name}"
            if (file.isDirectory) {
                result.addAll(flattenRecursive(file.children, path))
            } else {
                result.add(FlatEntry(path, Uri.parse(file.uriString)))
            }
        }
        return result
    }
}
