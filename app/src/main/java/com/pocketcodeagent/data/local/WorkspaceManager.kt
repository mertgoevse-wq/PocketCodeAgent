package com.pocketcodeagent.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pocketcodeagent.data.model.WorkspaceFile
import java.io.BufferedReader
import java.io.InputStreamReader

object WorkspaceManager {

    fun takePersistableUriPermission(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listFilesRecursive(context: Context, rootUri: Uri): List<WorkspaceFile> {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        return listFilesInternal(context, rootDoc)
    }

    private fun listFilesInternal(context: Context, parentDoc: DocumentFile): List<WorkspaceFile> {
        val list = mutableListOf<WorkspaceFile>()
        val files = parentDoc.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                list.add(
                    WorkspaceFile(
                        name = file.name ?: "",
                        uriString = file.uri.toString(),
                        isDirectory = true,
                        lastModified = file.lastModified(),
                        children = listFilesInternal(context, file)
                    )
                )
            } else {
                list.add(
                    WorkspaceFile(
                        name = file.name ?: "",
                        uriString = file.uri.toString(),
                        isDirectory = false,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        children = emptyList()
                    )
                )
            }
        }
        return list
    }

    fun readFileContent(context: Context, fileUri: Uri): String {
        val contentResolver = context.contentResolver
        val stringBuilder = StringBuilder()
        try {
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error reading file: ${e.message}"
        }
        return stringBuilder.toString()
    }

    fun writeFileContent(context: Context, fileUri: Uri, content: String): Boolean {
        val contentResolver = context.contentResolver
        try {
            contentResolver.openOutputStream(fileUri, "rwt")?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun createFile(context: Context, parentUri: Uri, mimeType: String, fileName: String): Uri? {
        val parentDoc = if (parentUri.scheme == "content" && parentUri.path?.contains("document") == true) {
            DocumentFile.fromSingleUri(context, parentUri)
        } else {
            DocumentFile.fromTreeUri(context, parentUri)
        } ?: return null
        
        val file = parentDoc.createFile(mimeType, fileName)
        return file?.uri
    }

    fun createDirectory(context: Context, parentUri: Uri, dirName: String): Uri? {
        val parentDoc = DocumentFile.fromTreeUri(context, parentUri) ?: return null
        val dir = parentDoc.createDirectory(dirName)
        return dir?.uri
    }

    fun deleteFile(context: Context, fileUri: Uri): Boolean {
        val fileDoc = DocumentFile.fromSingleUri(context, fileUri)
        return fileDoc?.delete() ?: false
    }

    fun findFileOrDirByRelativePath(context: Context, rootUri: Uri, relativePath: String): DocumentFile? {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        var current: DocumentFile? = rootDoc
        
        for (part in parts) {
            if (current == null || !current.isDirectory) return null
            current = current.findFile(part)
        }
        return current
    }

    fun createOrGetFileByRelativePath(context: Context, rootUri: Uri, relativePath: String): Uri? {
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return null
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        var currentDir: DocumentFile = rootDoc
        
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            var nextDir = currentDir.findFile(part)
            if (nextDir == null || !nextDir.isDirectory) {
                nextDir = currentDir.createDirectory(part)
            }
            if (nextDir == null) return null
            currentDir = nextDir
        }
        
        val fileName = parts.last()
        var targetFile = currentDir.findFile(fileName)
        if (targetFile == null) {
            targetFile = currentDir.createFile("text/plain", fileName)
        }
        return targetFile?.uri
    }
}
