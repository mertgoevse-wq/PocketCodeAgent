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
}
