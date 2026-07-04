package com.pocketcodeagent.domain.context

object ContextBudget {
    const val DEFAULT_MAX_CHARS = 60_000
    const val ACTIVE_FILE_MAX_CHARS = 20_000
    const val RELEVANT_FILE_MAX_CHARS = 12_000
    const val BUILD_FILE_MAX_CHARS = 8_000
    const val SUMMARY_MAX_CHARS = 3_000
    const val MAX_RELEVANT_FILES = 8
    const val MAX_BUILD_FILES = 4

    // Files > 200 KB = automatic read-only, not included in full context
    const val LARGE_FILE_THRESHOLD_BYTES = 200_000L
    // Files > 500 KB = path/metadata only, no content
    const val HUGE_FILE_THRESHOLD_BYTES = 500_000L
}
