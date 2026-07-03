package com.pocketcodeagent.data.local

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.model.WorkspaceFile

interface DocumentFileWorkspace {
    fun setRootUri(uriString: String)
    fun getRootUriString(): String?
    fun exists(relativePath: String): Boolean
    fun isDirectory(relativePath: String): Boolean
    fun readFile(relativePath: String): String?
    fun writeFile(relativePath: String, content: String): Boolean
    fun deleteFile(relativePath: String): Boolean
    fun createDirectory(relativePath: String): Boolean
    fun hasWritePermission(): Boolean
    fun backupFile(relativePath: String): Boolean
    fun restoreBackup(relativePath: String): Boolean
    fun listFiles(): List<WorkspaceFile>
}

class DocumentFileWorkspaceImpl(private val context: Context) : DocumentFileWorkspace {
    private var rootUri: Uri? = null

    // Simulated in-memory filesystem for Demo Mode
    private val demoFileContents = mutableMapOf(
        "index.html" to """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>PocketCodeAgent Demo</title>
                <style>
                    body {
                        background-color: #121214;
                        color: #F8F9FE;
                        font-family: system-ui, -apple-system, sans-serif;
                        text-align: center;
                        padding-top: 100px;
                    }
                    h1 { color: #5E72E4; font-size: 32px; margin-bottom: 8px; }
                    p { color: #ADB5BD; font-size: 16px; }
                    .badge {
                        background-color: #2DCE89;
                        color: #121214;
                        padding: 6px 12px;
                        border-radius: 20px;
                        font-weight: bold;
                        display: inline-block;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <h1>Hallo Welt! 👋</h1>
                <p>Das ist eine Live-Vorschau im PocketCodeAgent Demo-Modus.</p>
                <div class="badge">Kein API-Key benötigt</div>
                <script>
                    console.log("WebView Console captured successfully! 🚀");
                    console.warn("Information: Demo Mode is currently active.");
                </script>
            </body>
            </html>
        """.trimIndent(),
        "script.js" to """
            console.log("Hello from script.js inside PocketCodeAgent! 💻");
            function testDemo() {
                return "Demo Mode Active";
            }
        """.trimIndent(),
        "styles.css" to """
            body {
                margin: 0;
                padding: 0;
                background-color: #121214;
            }
        """.trimIndent(),
        "package.json" to """
            {
              "name": "demo-project",
              "private": true,
              "version": "0.0.0",
              "type": "module",
              "dependencies": {
                "react": "^18.2.0"
              },
              "devDependencies": {
                "vite": "^5.0.0"
              }
            }
        """.trimIndent()
    )

    private fun isDemo(): Boolean {
        return rootUri?.toString() == "demo://workspace"
    }

    override fun setRootUri(uriString: String) {
        rootUri = Uri.parse(uriString)
    }

    override fun getRootUriString(): String? {
        return rootUri?.toString()
    }

    private fun getRootDoc(): DocumentFile? {
        val uri = rootUri ?: return null
        return DocumentFile.fromTreeUri(context, uri)
    }

    override fun exists(relativePath: String): Boolean {
        if (isDemo()) {
            return demoFileContents.containsKey(relativePath)
        }
        val root = rootUri ?: return false
        return WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath) != null
    }

    override fun isDirectory(relativePath: String): Boolean {
        if (isDemo()) return false
        val root = rootUri ?: return false
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath)
        return doc?.isDirectory == true
    }

    override fun readFile(relativePath: String): String? {
        if (isDemo()) {
            return demoFileContents[relativePath]
        }
        val root = rootUri ?: return null
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath) ?: return null
        return try {
            WorkspaceManager.readFileContent(context, doc.uri)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeFile(relativePath: String, content: String): Boolean {
        if (isDemo()) {
            demoFileContents[relativePath] = content
            return true
        }
        val root = rootUri ?: return false
        if (!hasWritePermission()) return false
        return try {
            val fileUri = WorkspaceManager.createOrGetFileByRelativePath(context, root, relativePath) ?: return false
            WorkspaceManager.writeFileContent(context, fileUri, content)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteFile(relativePath: String): Boolean {
        if (isDemo()) {
            demoFileContents.remove(relativePath)
            return true
        }
        val root = rootUri ?: return false
        if (!hasWritePermission()) return false
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath) ?: return false
        return doc.delete()
    }

    override fun createDirectory(relativePath: String): Boolean {
        if (isDemo()) return true
        val root = rootUri ?: return false
        if (!hasWritePermission()) return false
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        var currentDir = getRootDoc() ?: return false
        
        for (part in parts) {
            var nextDir = currentDir.findFile(part)
            if (nextDir == null || !nextDir.isDirectory) {
                nextDir = currentDir.createDirectory(part)
            }
            if (nextDir == null) return false
            currentDir = nextDir
        }
        return true
    }

    override fun hasWritePermission(): Boolean {
        if (isDemo()) return true
        val rootDoc = getRootDoc() ?: return false
        return rootDoc.canWrite()
    }

    override fun backupFile(relativePath: String): Boolean {
        val content = readFile(relativePath)
        return if (content != null) {
            writeFile("$relativePath.bak", content)
        } else {
            writeFile("$relativePath.deleted_bak", "new_file_backup")
        }
    }

    override fun restoreBackup(relativePath: String): Boolean {
        val backupPath = "$relativePath.bak"
        val deletedBackupPath = "$relativePath.deleted_bak"
        
        return if (exists(backupPath)) {
            val backupContent = readFile(backupPath) ?: return false
            val success = writeFile(relativePath, backupContent)
            if (success) {
                deleteFile(backupPath)
            }
            success
        } else if (exists(deletedBackupPath)) {
            val success = deleteFile(relativePath)
            if (success) {
                deleteFile(deletedBackupPath)
            }
            success
        } else {
            false
        }
    }

    override fun listFiles(): List<WorkspaceFile> {
        if (isDemo()) {
            return demoFileContents.map { (path, content) ->
                WorkspaceFile(
                    name = path,
                    uriString = "demo://$path",
                    isDirectory = false,
                    size = content.length.toLong(),
                    lastModified = System.currentTimeMillis()
                )
            }
        }
        val root = rootUri ?: return emptyList()
        return WorkspaceManager.listFilesRecursive(context, root)
    }
}
