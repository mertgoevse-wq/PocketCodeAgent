package com.pocketcodeagent

import com.pocketcodeagent.domain.agent.AgentAction
import com.pocketcodeagent.domain.agent.AgentActionParser
import com.pocketcodeagent.domain.agent.AgentArtifact
import org.junit.Assert.*
import org.junit.Test

class AgentActionParserTest {

    @Test
    fun `plain chat text becomes a Note`() {
        val result = AgentActionParser.parse("Hello, please explain this code.")
        assertEquals(1, result.size)
        val artifact = result[0]
        assertEquals("Antwort", artifact.title)
        assertEquals(1, artifact.actions.size)
        assertTrue(artifact.actions[0] is AgentAction.Note)
    }

    @Test
    fun `single pocketArtifact with file action creates file`() {
        val xml = """
            <pocketArtifact title="Create main.kt">
                <pocketAction type="file" filePath="src/main.kt">
                    fun main() { println("Hello") }
                </pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        assertEquals(1, result.size)
        val actions = result[0].actions
        assertEquals(1, actions.size)
        assertTrue(actions[0] is AgentAction.CreateFile)
        val create = actions[0] as AgentAction.CreateFile
        assertEquals("src/main.kt", create.path)
        assertTrue(create.content.contains("fun main()"))
    }

    @Test
    fun `multiple pocketAction blocks in one artifact`() {
        val xml = """
            <pocketArtifact title="Multiple actions">
                <pocketAction type="file" filePath="src/A.kt">content A</pocketAction>
                <pocketAction type="file" filePath="src/B.kt">content B</pocketAction>
                <pocketAction type="note">a note here</pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        assertEquals(1, result.size)
        val actions = result[0].actions
        assertEquals(3, actions.size)
        assertTrue(actions[0] is AgentAction.CreateFile)
        assertTrue(actions[1] is AgentAction.CreateFile)
        assertTrue(actions[2] is AgentAction.Note)
    }

    @Test
    fun `shell command becomes RunCommand`() {
        val xml = """
            <pocketArtifact title="Run test">
                <pocketAction type="shell">
                    ./gradlew test
                </pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        val actions = result[0].actions
        assertEquals(1, actions.size)
        assertTrue(actions[0] is AgentAction.RunCommand)
        assertEquals("./gradlew test", (actions[0] as AgentAction.RunCommand).command)
    }

    @Test
    fun `preview action becomes OpenPreview`() {
        val xml = """
            <pocketArtifact title="Preview">
                <pocketAction type="preview">
                    http://127.0.0.1:5173
                </pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        val actions = result[0].actions
        assertEquals(1, actions.size)
        assertTrue(actions[0] is AgentAction.OpenPreview)
        assertEquals("http://127.0.0.1:5173", (actions[0] as AgentAction.OpenPreview).target)
    }

    @Test
    fun `note action becomes Note`() {
        val xml = """
            <pocketArtifact title="Notes">
                <pocketAction type="note">this is a note</pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        val actions = result[0].actions
        assertEquals(1, actions.size)
        assertTrue(actions[0] is AgentAction.Note)
        assertEquals("this is a note", (actions[0] as AgentAction.Note).text)
    }

    @Test
    fun `delete action becomes DeleteFile`() {
        val xml = """
            <pocketArtifact title="Delete">
                <pocketAction type="delete" filePath="old_file.txt">
                </pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        val actions = result[0].actions
        assertEquals(1, actions.size)
        assertTrue(actions[0] is AgentAction.DeleteFile)
        assertEquals("old_file.txt", (actions[0] as AgentAction.DeleteFile).path)
    }

    @Test
    fun `invalid XML does not crash`() {
        val result = AgentActionParser.parse("<broken><xml><<")
        assertEquals(1, result.size)
        assertTrue(result[0].actions[0] is AgentAction.Note)
    }

    @Test
    fun `unknown action type generates Warning and Note`() {
        val xml = """
            <pocketArtifact title="Unknown">
                <pocketAction type="invalidType" filePath="x.txt">content</pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        assertEquals(1, result.size)
        val warnings = result[0].parseWarnings
        assertTrue(warnings.any { it.contains("Unbekannt", ignoreCase = true) })
        assertTrue(result[0].actions[0] is AgentAction.Note)
    }

    @Test
    fun `JSON fallback with summary actions patches and commands`() {
        val json = """
            {
                "summary": "I updated the file.",
                "title": "Update",
                "patches": [
                    { "path": "src/main.kt", "newText": "new content", "oldText": "old", "action": "modify" }
                ],
                "commands": [
                    { "command": "./gradlew test", "reason": "Run tests" }
                ],
                "actions": [
                    { "type": "file", "path": "src/New.kt", "content": "new file" }
                ]
            }
        """.trimIndent()
        val result = AgentActionParser.parse(json)
        assertEquals(1, result.size)
        val actions = result[0].actions
        assertTrue(actions.any { it is AgentAction.Note && (it as AgentAction.Note).text.contains("updated") })
        assertTrue(actions.any { it is AgentAction.ModifyFile })
        assertTrue(actions.any { it is AgentAction.RunCommand })
        assertTrue(actions.any { it is AgentAction.CreateFile })
    }

    @Test
    fun `JSON fallback with legacy patches works`() {
        val json = """
            {
                "summary": "Done",
                "patches": [
                    { "path": "src/X.kt", "action": "modify", "oldText": "old", "newText": "new" }
                ]
            }
        """.trimIndent()
        val result = AgentActionParser.parse(json)
        assertTrue(result.isNotEmpty())
        val actions = result[0].actions
        assertTrue(actions.any { it is AgentAction.ModifyFile })
    }

    @Test
    fun `legacy commands JSON works`() {
        val json = """
            {
                "summary": "Run",
                "commands": [
                    { "command": "npm run dev", "reason": "start server" }
                ]
            }
        """.trimIndent()
        val result = AgentActionParser.parse(json)
        val actions = result[0].actions
        assertTrue(actions.any { it is AgentAction.RunCommand })
        assertEquals("npm run dev", (actions.first { it is AgentAction.RunCommand } as AgentAction.RunCommand).command)
    }

    @Test
    fun `modify action with oldText creates ModifyFile`() {
        val xml = """
            <pocketArtifact title="Modify">
                <pocketAction type="modify" filePath="src/X.kt" oldText="old">new</pocketAction>
            </pocketArtifact>
        """.trimIndent()
        val result = AgentActionParser.parse(xml)
        assertTrue(result[0].actions[0] is AgentAction.ModifyFile)
    }

    @Test
    fun `empty input returns empty list`() {
        val result = AgentActionParser.parse("   ")
        assertTrue(result.isEmpty())
    }
}
