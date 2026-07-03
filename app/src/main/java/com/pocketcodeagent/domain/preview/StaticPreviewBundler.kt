package com.pocketcodeagent.domain.preview

import android.content.Context
import android.net.Uri
import com.pocketcodeagent.data.local.DocumentFileWorkspace
import com.pocketcodeagent.data.local.WorkspaceManager

data class StaticPreviewResult(
    val html: String,
    val sourcePath: String,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val loadedCssFiles: List<String> = emptyList(),
    val loadedJsFiles: List<String> = emptyList()
)

class StaticPreviewBundler(private val context: Context) {

    fun bundleFromWorkspace(
        workspace: DocumentFileWorkspace,
        rootUriString: String,
        startFileName: String? = null
    ): StaticPreviewResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val loadedCss = mutableListOf<String>()
        val loadedJs = mutableListOf<String>()

        val rootUri = Uri.parse(rootUriString)

        val indexPath = if (startFileName != null) {
            startFileName
        } else {
            findIndexHtml(workspace, rootUriString) ?: run {
                return StaticPreviewResult(
                    html = "<html><body><h3 style='color:#888;text-align:center;padding-top:100px;font-family:sans-serif;'>Keine index.html im Workspace gefunden.</h3></body></html>",
                    sourcePath = "",
                    errors = listOf("Keine index.html gefunden. Getestet: index.html, public/index.html, src/index.html")
                )
            }
        }

        val rawHtml = workspace.readFile(indexPath)
        if (rawHtml == null) {
            return StaticPreviewResult(
                html = "<html><body><h3 style='color:#888;text-align:center;padding-top:100px;font-family:sans-serif;'>Datei nicht lesbar: $indexPath</h3></body></html>",
                sourcePath = indexPath,
                errors = listOf("Konnte $indexPath nicht lesen.")
            )
        }

        val baseDir = indexPath.substringBeforeLast("/", "")

        val bundled = bundleHtml(rawHtml, baseDir, workspace, rootUri, rootUriString, warnings, errors, loadedCss, loadedJs)

        return StaticPreviewResult(
            html = bundled,
            sourcePath = indexPath,
            warnings = warnings,
            errors = errors,
            loadedCssFiles = loadedCss,
            loadedJsFiles = loadedJs
        )
    }

    private fun findIndexHtml(workspace: DocumentFileWorkspace, rootUriString: String): String? {
        val candidates = listOf("index.html", "public/index.html", "src/index.html")
        for (candidate in candidates) {
            if (workspace.exists(candidate) && !workspace.isDirectory(candidate)) {
                return candidate
            }
        }
        val uri = Uri.parse(rootUriString)
        for (candidate in candidates) {
            val doc = WorkspaceManager.findFileOrDirByRelativePath(context, uri, candidate)
            if (doc != null && doc.isFile) {
                return candidate
            }
        }
        return null
    }

    private fun bundleHtml(
        rawHtml: String,
        baseDir: String,
        workspace: DocumentFileWorkspace,
        rootUri: Uri,
        rootUriString: String,
        warnings: MutableList<String>,
        errors: MutableList<String>,
        loadedCss: MutableList<String>,
        loadedJs: MutableList<String>
    ): String {
        var result = rawHtml

        result = replaceCssLinks(result, baseDir, workspace, rootUri, rootUriString, warnings, errors, loadedCss)
        result = replaceJsScripts(result, baseDir, workspace, rootUri, rootUriString, warnings, errors, loadedJs)

        return result
    }

    private fun replaceCssLinks(
        html: String,
        baseDir: String,
        workspace: DocumentFileWorkspace,
        rootUri: Uri,
        rootUriString: String,
        warnings: MutableList<String>,
        errors: MutableList<String>,
        loadedCss: MutableList<String>
    ): String {
        val linkRegexHrefFirst = Regex(
            """<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']stylesheet["'][^>]*/?>""",
            RegexOption.IGNORE_CASE
        )
        val linkRegexRelFirst = Regex(
            """<link\s+[^>]*rel\s*=\s*["']stylesheet["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*/?>""",
            RegexOption.IGNORE_CASE
        )
        var result = html
        result = linkRegexHrefFirst.replace(result) { match ->
            val href = match.groupValues[1]
            val resolved = resolvePath(href, baseDir)
            if (resolved == null) {
                warnings.add("CSS (extern): $href (externe URL — unverändert)")
                match.value
            } else {
                val cssContent = readFileSafe(resolved, workspace, rootUri, rootUriString)
                if (cssContent != null) {
                    loadedCss.add(resolved)
                    "<style>/* bundled: $resolved */\n$cssContent\n</style>"
                } else {
                    warnings.add("CSS nicht lesbar: $resolved — Link bleibt erhalten")
                    match.value
                }
            }
        }
        result = linkRegexRelFirst.replace(result) { match ->
            val href = match.groupValues[1]
            val resolved = resolvePath(href, baseDir)
            if (resolved == null) {
                warnings.add("CSS (extern): $href (externe URL — unverändert)")
                match.value
            } else {
                val cssContent = readFileSafe(resolved, workspace, rootUri, rootUriString)
                if (cssContent != null) {
                    loadedCss.add(resolved)
                    "<style>/* bundled: $resolved */\n$cssContent\n</style>"
                } else {
                    warnings.add("CSS nicht lesbar: $resolved — Link bleibt erhalten")
                    match.value
                }
            }
        }
        return result
    }

    private fun replaceJsScripts(
        html: String,
        baseDir: String,
        workspace: DocumentFileWorkspace,
        rootUri: Uri,
        rootUriString: String,
        warnings: MutableList<String>,
        errors: MutableList<String>,
        loadedJs: MutableList<String>
    ): String {
        val scriptRegex = Regex(
            """<script\s+[^>]*src\s*=\s*["']([^"']+)["'][^>]*>\s*</script>""",
            RegexOption.IGNORE_CASE
        )
        return scriptRegex.replace(html) { match ->
            val src = match.groupValues[1]
            val resolved = resolvePath(src, baseDir)

            if (resolved == null) {
                if (src.startsWith("http://") || src.startsWith("https://")) {
                    warnings.add("JS (extern): $src (externe URL — unverändert)")
                } else {
                    warnings.add("JS blockiert: $src (unsicherer Pfad)")
                }
                match.value
            } else {
                val jsContent = readFileSafe(resolved, workspace, rootUri, rootUriString)
                if (jsContent != null) {
                    loadedJs.add(resolved)
                    "<script>/* bundled: $resolved */\n$jsContent\n</script>"
                } else {
                    warnings.add("JS nicht lesbar: $resolved — Link bleibt erhalten")
                    match.value
                }
            }
        }
    }

    private fun resolvePath(href: String, baseDir: String): String? {
        if (href.startsWith("http://") || href.startsWith("https://") ||
            href.startsWith("//") || href.startsWith("data:") ||
            href.startsWith("content:") || href.startsWith("file://")
        ) {
            return null
        }
        if (href.contains("../") || href.startsWith("/") ||
            href.contains(":\\") || href.startsWith("\\\\")
        ) {
            return null
        }
        val cleaned = href.trimStart('.', '/')
        return if (baseDir.isEmpty()) cleaned else "$baseDir/$cleaned"
    }

    private fun readFileSafe(
        relativePath: String,
        workspace: DocumentFileWorkspace,
        rootUri: Uri,
        rootUriString: String
    ): String? {
        if (workspace.exists(relativePath) && !workspace.isDirectory(relativePath)) {
            return workspace.readFile(relativePath)
        }
        val doc = WorkspaceManager.findFileOrDirByRelativePath(context, rootUri, relativePath)
        if (doc != null && doc.isFile) {
            return try {
                WorkspaceManager.readFileContent(context, doc.uri)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}
