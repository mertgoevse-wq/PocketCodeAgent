package com.pocketcodeagent.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.local.DocumentFileWorkspace
import com.pocketcodeagent.data.local.DocumentFileWorkspaceImpl
import com.pocketcodeagent.data.local.WorkspaceManager
import com.pocketcodeagent.data.model.WorkspaceFile

enum class DiffLineType {
    ADDED, REMOVED, UNCHANGED
}

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val lineNumber: Int
)

class WorkspaceRepository(val context: Context) {
    val workspace: DocumentFileWorkspace = DocumentFileWorkspaceImpl(context)

    fun persistWorkspacePermission(uri: Uri) {
        WorkspaceManager.takePersistableUriPermission(context, uri)
    }

    fun loadWorkspaceFiles(rootUri: Uri): List<WorkspaceFile> {
        return WorkspaceManager.listFilesRecursive(context, rootUri)
    }

    fun readFile(fileUriString: String): String {
        return try {
            WorkspaceManager.readFileContent(context, Uri.parse(fileUriString))
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    fun writeFile(fileUriString: String, content: String): Boolean {
        return try {
            WorkspaceManager.writeFileContent(context, Uri.parse(fileUriString), content)
        } catch (e: Exception) {
            false
        }
    }

    fun createFile(parentUriString: String, mimeType: String, fileName: String): Uri? {
        return try {
            WorkspaceManager.createFile(context, Uri.parse(parentUriString), mimeType, fileName)
        } catch (e: Exception) {
            null
        }
    }

    fun createDirectory(parentUriString: String, dirName: String): Uri? {
        return try {
            WorkspaceManager.createDirectory(context, Uri.parse(parentUriString), dirName)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteFile(fileUriString: String): Boolean {
        return try {
            WorkspaceManager.deleteFile(context, Uri.parse(fileUriString))
        } catch (e: Exception) {
            false
        }
    }

    fun getFileUriByRelativePath(rootUriString: String, relativePath: String): Uri? {
        val rootUri = Uri.parse(rootUriString)
        val docFile = WorkspaceManager.findFileOrDirByRelativePath(context, rootUri, relativePath)
        return docFile?.uri
    }

    fun createOrGetFileUriByRelativePath(rootUriString: String, relativePath: String): Uri? {
        val rootUri = Uri.parse(rootUriString)
        return WorkspaceManager.createOrGetFileByRelativePath(context, rootUri, relativePath)
    }

    fun computeDiff(original: String, modified: String): List<DiffLine> {
        val originalLines = original.split("\n")
        val modifiedLines = modified.split("\n")
        val diffList = mutableListOf<DiffLine>()
        
        var i = 0
        var j = 0
        var lineNum = 1
        
        while (i < originalLines.size || j < modifiedLines.size) {
            if (i < originalLines.size && j < modifiedLines.size) {
                if (originalLines[i] == modifiedLines[j]) {
                    diffList.add(DiffLine(DiffLineType.UNCHANGED, originalLines[i], lineNum++))
                    i++
                    j++
                } else {
                    // Simple heuristic diff: check if modified line is added or original is removed
                    // Check if j+1 matches i
                    if (j + 1 < modifiedLines.size && originalLines[i] == modifiedLines[j + 1]) {
                        diffList.add(DiffLine(DiffLineType.ADDED, modifiedLines[j], lineNum++))
                        j++
                    } else if (i + 1 < originalLines.size && originalLines[i + 1] == modifiedLines[j]) {
                        diffList.add(DiffLine(DiffLineType.REMOVED, originalLines[i], lineNum++))
                        i++
                    } else {
                        // Replace (one removed, one added)
                        diffList.add(DiffLine(DiffLineType.REMOVED, originalLines[i], lineNum))
                        diffList.add(DiffLine(DiffLineType.ADDED, modifiedLines[j], lineNum++))
                        i++
                        j++
                    }
                }
            } else if (i < originalLines.size) {
                diffList.add(DiffLine(DiffLineType.REMOVED, originalLines[i], lineNum++))
                i++
            } else {
                diffList.add(DiffLine(DiffLineType.ADDED, modifiedLines[j], lineNum++))
                j++
            }
        }
        return diffList
    }
}
