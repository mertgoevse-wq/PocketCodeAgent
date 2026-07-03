package com.pocketcodeagent.domain.agent

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

object AgentActionParser {
    private val artifactRegex = Regex(
        """<pocketArtifact\b([^>]*)>(.*?)</pocketArtifact>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val actionRegex = Regex(
        """<pocketAction\b([^>]*)>(.*?)</pocketAction>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val attrRegex = Regex("""(\w+)\s*=\s*("([^"]*)"|'([^']*)')""")

    fun parse(rawText: String): List<AgentArtifact> {
        val text = rawText.trim()
        if (text.isBlank()) return emptyList()

        parseJson(text)?.let { return it }

        val artifacts = parsePocketArtifacts(text)
        if (artifacts.isNotEmpty()) return artifacts

        val warnings = if (text.contains("<pocketArtifact", ignoreCase = true) ||
            text.contains("<pocketAction", ignoreCase = true)
        ) {
            listOf("Unvollstaendiges pocketArtifact/pocketAction Format erkannt; als Notiz angezeigt.")
        } else {
            emptyList()
        }

        return listOf(
            AgentArtifact(
                id = newId("artifact"),
                title = "Antwort",
                actions = listOf(AgentAction.Note(newId("note"), text)),
                rawText = text,
                parseWarnings = warnings
            )
        )
    }

    private fun parsePocketArtifacts(text: String): List<AgentArtifact> {
        return artifactRegex.findAll(text).map { match ->
            val attrs = parseAttributes(match.groupValues[1])
            val body = match.groupValues[2].trim()
            val warnings = mutableListOf<String>()
            val actions = actionRegex.findAll(body).mapNotNull { actionMatch ->
                val actionAttrs = parseAttributes(actionMatch.groupValues[1])
                val content = htmlDecode(actionMatch.groupValues[2].trim())
                parsePocketAction(actionAttrs, content, warnings)
            }.toList()

            if (actions.isEmpty()) {
                warnings.add("Artifact enthaelt keine vollstaendigen pocketAction-Bloecke.")
            }

            AgentArtifact(
                id = newId("artifact"),
                title = htmlDecode(attrs["title"].orEmpty()).ifBlank { "Agent Artifact" },
                actions = actions.ifEmpty {
                    listOf(AgentAction.Note(newId("note"), body.ifBlank { text }))
                },
                rawText = match.value,
                parseWarnings = warnings
            )
        }.toList()
    }

    private fun parsePocketAction(
        attrs: Map<String, String>,
        content: String,
        warnings: MutableList<String>
    ): AgentAction? {
        val type = attrs["type"]?.lowercase().orEmpty()
        val path = attrs["filePath"] ?: attrs["path"] ?: attrs["file"]
        val oldText = attrs["oldText"]?.let(::htmlDecode)

        return when (type) {
            "file" -> {
                if (path.isNullOrBlank()) {
                    warnings.add("file action ohne filePath ignoriert.")
                    null
                } else if (oldText != null) {
                    warnIfRestrictedPath(path, warnings)
                    AgentAction.ModifyFile(newId("modify"), path, oldText, content)
                } else {
                    warnIfRestrictedPath(path, warnings)
                    AgentAction.CreateFile(newId("create"), path, content)
                }
            }
            "modify" -> {
                if (path.isNullOrBlank()) {
                    warnings.add("modify action ohne filePath ignoriert.")
                    null
                } else {
                    warnIfRestrictedPath(path, warnings)
                    AgentAction.ModifyFile(newId("modify"), path, oldText, content)
                }
            }
            "delete" -> {
                if (path.isNullOrBlank()) {
                    warnings.add("delete action ohne filePath ignoriert.")
                    null
                } else {
                    warnIfRestrictedPath(path, warnings)
                    AgentAction.DeleteFile(newId("delete"), path)
                }
            }
            "shell", "command" -> {
                val command = content.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                if (command.isBlank()) {
                    warnings.add("shell action ohne Command ignoriert.")
                    null
                } else {
                    AgentAction.RunCommand(
                        id = newId("cmd"),
                        command = command,
                        reason = attrs["reason"]?.let(::htmlDecode),
                        riskLevel = CommandRiskScanner.scan(command)
                    )
                }
            }
            "preview" -> {
                if (content.isBlank()) {
                    warnings.add("preview action ohne Ziel ignoriert.")
                    null
                } else {
                    AgentAction.OpenPreview(newId("preview"), content)
                }
            }
            "note" -> AgentAction.Note(newId("note"), content)
            else -> {
                warnings.add("Unbekannter Action-Typ '$type' als Notiz behandelt.")
                AgentAction.Note(newId("note"), content.ifBlank { attrs.toString() })
            }
        }
    }

    private fun parseJson(text: String): List<AgentArtifact>? {
        val jsonText = stripJsonFence(text)
        if (!jsonText.trimStart().startsWith("{")) return null

        return try {
            val root = JsonParser.parseString(jsonText).asJsonObject
            val warnings = mutableListOf<String>()
            val actions = mutableListOf<AgentAction>()

            root.getAsJsonArrayOrNull("actions")?.let { array ->
                actions.addAll(parseJsonActions(array, warnings))
            }

            root.getAsJsonArrayOrNull("patches")?.let { array ->
                actions.addAll(parseLegacyPatches(array, warnings))
            }

            root.getAsJsonArrayOrNull("commands")?.let { array ->
                actions.addAll(parseLegacyCommands(array, warnings))
            }

            val summary = root.getStringOrNull("summary")
            if (!summary.isNullOrBlank()) {
                actions.add(0, AgentAction.Note(newId("note"), summary))
            }

            if (actions.isEmpty()) {
                actions.add(AgentAction.Note(newId("note"), text))
                warnings.add("JSON enthielt keine actions, patches oder commands.")
            }

            listOf(
                AgentArtifact(
                    id = newId("artifact"),
                    title = root.getStringOrNull("title") ?: "Agent Artifact",
                    actions = actions,
                    rawText = text,
                    parseWarnings = warnings
                )
            )
        } catch (_: Exception) {
            listOf(
                AgentArtifact(
                    id = newId("artifact"),
                    title = "Antwort",
                    actions = listOf(AgentAction.Note(newId("note"), text)),
                    rawText = text,
                    parseWarnings = listOf("JSON konnte nicht gelesen werden; als Notiz angezeigt.")
                )
            )
        }
    }

    private fun parseJsonActions(array: JsonArray, warnings: MutableList<String>): List<AgentAction> {
        return array.mapNotNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val type = obj.getStringOrNull("type")?.lowercase().orEmpty()
            val path = obj.getStringOrNull("path") ?: obj.getStringOrNull("filePath")
            when (type) {
                "file" -> {
                    if (path.isNullOrBlank()) {
                        warnings.add("JSON file action ohne path ignoriert.")
                        null
                    } else {
                        warnIfRestrictedPath(path, warnings)
                        val oldText = obj.getStringOrNull("oldText")
                        val content = obj.getStringOrNull("content") ?: obj.getStringOrNull("newText").orEmpty()
                        if (oldText != null) {
                            AgentAction.ModifyFile(newId("modify"), path, oldText, content)
                        } else {
                            AgentAction.CreateFile(newId("create"), path, content)
                        }
                    }
                }
                "modify" -> {
                    if (path.isNullOrBlank()) {
                        warnings.add("JSON modify action ohne path ignoriert.")
                        null
                    } else {
                        warnIfRestrictedPath(path, warnings)
                        AgentAction.ModifyFile(
                            id = newId("modify"),
                            path = path,
                            oldText = obj.getStringOrNull("oldText"),
                            newText = obj.getStringOrNull("newText") ?: obj.getStringOrNull("content").orEmpty()
                        )
                    }
                }
                "delete" -> {
                    if (path.isNullOrBlank()) {
                        warnings.add("JSON delete action ohne path ignoriert.")
                        null
                    } else {
                        warnIfRestrictedPath(path, warnings)
                        AgentAction.DeleteFile(newId("delete"), path)
                    }
                }
                "shell", "command" -> {
                    val command = obj.getStringOrNull("command").orEmpty()
                    if (command.isBlank()) {
                        warnings.add("JSON shell action ohne command ignoriert.")
                        null
                    } else {
                        AgentAction.RunCommand(
                            id = newId("cmd"),
                            command = command,
                            reason = obj.getStringOrNull("reason"),
                            riskLevel = CommandRiskScanner.scan(command)
                        )
                    }
                }
                "preview" -> {
                    val target = obj.getStringOrNull("target") ?: obj.getStringOrNull("url")
                    if (target.isNullOrBlank()) {
                        warnings.add("JSON preview action ohne target ignoriert.")
                        null
                    } else {
                        AgentAction.OpenPreview(newId("preview"), target)
                    }
                }
                "note" -> AgentAction.Note(newId("note"), obj.getStringOrNull("text").orEmpty())
                else -> {
                    warnings.add("Unbekannter JSON Action-Typ '$type' als Notiz behandelt.")
                    AgentAction.Note(newId("note"), obj.toString())
                }
            }
        }
    }

    private fun parseLegacyPatches(array: JsonArray, warnings: MutableList<String>): List<AgentAction> {
        return array.mapNotNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val path = obj.getStringOrNull("path")
            if (path.isNullOrBlank()) {
                warnings.add("Legacy patch ohne path ignoriert.")
                return@mapNotNull null
            }
            warnIfRestrictedPath(path, warnings)
            when (obj.getStringOrNull("action")?.lowercase()) {
                "delete" -> AgentAction.DeleteFile(newId("delete"), path)
                "modify" -> AgentAction.ModifyFile(
                    id = newId("modify"),
                    path = path,
                    oldText = obj.getStringOrNull("oldText"),
                    newText = obj.getStringOrNull("newText").orEmpty()
                )
                else -> AgentAction.CreateFile(
                    id = newId("create"),
                    path = path,
                    content = obj.getStringOrNull("newText") ?: obj.getStringOrNull("content").orEmpty()
                )
            }
        }
    }

    private fun parseLegacyCommands(array: JsonArray, warnings: MutableList<String>): List<AgentAction> {
        return array.mapNotNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val command = obj.getStringOrNull("command").orEmpty()
            if (command.isBlank()) {
                warnings.add("Legacy command ohne command ignoriert.")
                null
            } else {
                AgentAction.RunCommand(
                    id = newId("cmd"),
                    command = command,
                    reason = obj.getStringOrNull("reason"),
                    riskLevel = CommandRiskScanner.scan(command)
                )
            }
        }
    }

    private fun parseAttributes(input: String): Map<String, String> {
        return attrRegex.findAll(input).associate { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[3].ifBlank { match.groupValues[4] }
            key to htmlDecode(value)
        }
    }

    private fun stripJsonFence(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.substringAfter('\n', cleaned)
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter('\n', cleaned)
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.dropLast(3)
        }
        return cleaned.trim()
    }

    private fun htmlDecode(value: String): String {
        return value
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }

    private fun warnIfRestrictedPath(path: String, warnings: MutableList<String>) {
        val blockedSegments = setOf("example", "sample", "demo", "playground", "starter", "template")
        val segments = path.replace('\\', '/').split('/').map { it.lowercase() }
        val blocked = segments.firstOrNull { it in blockedSegments }
        if (blocked != null) {
            warnings.add("Pfad '$path' enthaelt verbotenen Ordnernamen '$blocked'.")
        }
        if (path.contains("com.example", ignoreCase = true)) {
            warnings.add("Pfad '$path' enthaelt verbotenes Package com.example.")
        }
    }

    private fun JsonObject.getStringOrNull(name: String): String? {
        val value = get(name) ?: return null
        return if (value.isJsonNull) null else value.asString
    }

    private fun JsonObject.getAsJsonArrayOrNull(name: String): JsonArray? {
        val value = get(name) ?: return null
        return if (value.isJsonArray) value.asJsonArray else null
    }

    private fun com.google.gson.JsonElement.asJsonObjectOrNull(): JsonObject? {
        return if (isJsonObject) asJsonObject else null
    }

    private fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
}
