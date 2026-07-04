package com.pocketcodeagent.domain.language

enum class LanguageMode(val label: String, val localeTag: String?) {
    System("System", null),
    German("Deutsch", "de"),
    English("English", "en")
}
