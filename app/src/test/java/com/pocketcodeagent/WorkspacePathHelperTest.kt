package com.pocketcodeagent

import com.pocketcodeagent.data.model.WorkspaceFile
import com.pocketcodeagent.domain.workspace.WorkspacePathHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspacePathHelperTest {
    @Test
    fun `findRelativePathByUri resolves nested file from workspace tree`() {
        val files = listOf(
            WorkspaceFile(
                name = "src",
                uriString = "content://tree/src",
                isDirectory = true,
                children = listOf(
                    WorkspaceFile(
                        name = "Main.kt",
                        uriString = "content://tree/src/Main.kt",
                        isDirectory = false
                    )
                )
            )
        )

        assertEquals(
            "src/Main.kt",
            WorkspacePathHelper.findRelativePathByUri(files, "content://tree/src/Main.kt")
        )
    }

    @Test
    fun `safeRelativePath rejects content uri and falls back to file name`() {
        assertEquals(
            "Main.kt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(),
                fileUri = "content://tree/src/Main.kt",
                openFileRelativePath = "content://tree/src/Main.kt",
                fallbackFileName = "Main.kt"
            )
        )
    }

    // ── Path safety tests ────────────────────────────────────────────────────
    @Test
    fun `safeRelativePath blocks dot dot traversal and falls back`() {
        assertEquals(
            "test.txt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "../etc/passwd",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath blocks absolute path and falls back`() {
        assertEquals(
            "test.txt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "/etc/passwd",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath blocks windows absolute path and falls back`() {
        assertEquals(
            "test.txt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "C:\\Windows\\System32",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath blocks content uri and falls back`() {
        assertEquals(
            "test.txt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "content://com.android/sensitive",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath allows paths with normal folder names`() {
        assertEquals(
            "src/components/Button.kt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "src/components/Button.kt",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath allows normal root files`() {
        assertEquals(
            "index.html",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "index.html",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath allows normal nested files`() {
        assertEquals(
            "src/components/Button.kt",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "src/components/Button.kt",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath allows styles css`() {
        assertEquals(
            "styles.css",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "styles.css",
                fallbackFileName = "test.txt"
            )
        )
    }

    @Test
    fun `safeRelativePath allows app js`() {
        assertEquals(
            "app.js",
            WorkspacePathHelper.safeRelativePath(
                files = emptyList(), fileUri = null,
                openFileRelativePath = "app.js",
                fallbackFileName = "test.txt"
            )
        )
    }
}
