package com.pocketcodeagent.data.util

import com.pocketcodeagent.data.repository.DiffLine
import com.pocketcodeagent.data.repository.DiffLineType

object DiffGenerator {
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
                    // Heuristic diff
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
