package com.pocketcodeagent.data.local

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.model.WorkspaceFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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
        val root = rootUri ?: return false
        return WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath) != null
    }

    override fun isDirectory(relativePath: String): Boolean {
        val root = rootUri ?: return false
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath)
        return doc?.isDirectory == true
    }

    override fun readFile(relativePath: String): String? {
        val root = rootUri ?: return null
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath) ?: return null
        return try {
            WorkspaceManager.readFileContent(context, doc.uri)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeFile(relativePath: String, content: String): Boolean {
        val root = rootUri ?: return false
        if (!hasWritePermission()) return false
        return try {
            val fileUri = WorkspaceManager.createOrGetFileByRelativePath(context, root, relativePath) ?: return false
            WorkspaceManager.writeFileContent(context, fileUri, content)
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteFile(relativePath: String): Boolean {
        val root = rootUri ?: return false
        if (!hasWritePermission()) return false
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, root, relativePath) ?: return false
        return doc.delete()
    }

    override fun createDirectory(relativePath: String): Boolean {
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
        val rootDoc = getRootDoc() ?: return false
        return rootDoc.canWrite()
    }

    override fun backupFile(relativePath: String): Boolean {
        val content = readFile(relativePath)
        return if (content != null) {
            // Write the backup as filepath.bak
            writeFile("$relativePath.bak", content)
        } else {
            // Marker backup to show it did not exist: write a special empty file named .deleted
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
            // The file did not exist before the patch, so to undo, we delete the file!
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
        val root = rootUri ?: return emptyList()
        return WorkspaceManager.listFilesRecursive(context, root)
    }
}
