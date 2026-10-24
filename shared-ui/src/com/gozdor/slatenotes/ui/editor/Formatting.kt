package com.gozdor.slatenotes.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Editing operations for the formatting toolbar and Enter-key list continuation. */
object Formatting {

    /** Wraps the selection in markers (or removes them if already wrapped). Empty selection puts the cursor between. */
    fun wrap(v: TextFieldValue, pre: String, post: String = pre): TextFieldValue {
        val t = v.text
        val s = v.selection.min
        val e = v.selection.max
        val selected = t.substring(s, e)
        if (selected.length >= pre.length + post.length && selected.startsWith(pre) && selected.endsWith(post)) {
            val inner = selected.substring(pre.length, selected.length - post.length)
            return TextFieldValue(t.replaceRange(s, e, inner), TextRange(s, s + inner.length))
        }
        val nt = t.substring(0, s) + pre + selected + post + t.substring(e)
        return if (s == e) TextFieldValue(nt, TextRange(s + pre.length))
        else TextFieldValue(nt, TextRange(s + pre.length, e + pre.length))
    }

    private fun lineBounds(t: String, cursor: Int): Pair<Int, Int> {
        val start = if (cursor <= 0) 0 else t.lastIndexOf('\n', cursor - 1) + 1
        val nl = t.indexOf('\n', start)
        return start to (if (nl < 0) t.length else nl)
    }

    /** Toggles a line prefix like "- " or "> ". If the line has one of [alternatives], it's swapped for [prefix]. */
    fun toggleLinePrefix(v: TextFieldValue, prefix: String, alternatives: List<String> = emptyList()): TextFieldValue {
        val t = v.text
        val (ls, le) = lineBounds(t, v.selection.min)
        val line = t.substring(ls, le)
        val indentLen = line.length - line.trimStart().length
        val body = line.substring(indentLen)
        val existing = (alternatives + prefix).sortedByDescending { it.length }.firstOrNull { body.startsWith(it) }
        val newBody = when (existing) {
            prefix -> body.removePrefix(prefix)
            null -> prefix + body
            else -> prefix + body.removePrefix(existing)
        }
        val newLine = line.substring(0, indentLen) + newBody
        val delta = newLine.length - line.length
        val nt = t.replaceRange(ls, le, newLine)
        val cursor = (v.selection.end + delta).coerceIn(ls, nt.length)
        return TextFieldValue(nt, TextRange(cursor))
    }

    /** Indents (tab) or unindents the current line, the way Obsidian nests list items. */
    fun indent(v: TextFieldValue, increase: Boolean): TextFieldValue {
        val t = v.text
        val (ls, le) = lineBounds(t, v.selection.min)
        val line = t.substring(ls, le)
        val newLine = if (increase) "\t" + line else when {
            line.startsWith("\t") -> line.substring(1)
            else -> line.substring((line.length - line.trimStart(' ').length).coerceAtMost(4))
        }
        val delta = newLine.length - line.length
        val nt = t.replaceRange(ls, le, newLine)
        return TextFieldValue(nt, TextRange((v.selection.end + delta).coerceIn(ls, nt.length)))
    }

    /** Cycles a line through "# ", "## ", "### " and back to plain text. */
    fun cycleHeading(v: TextFieldValue): TextFieldValue {
        val t = v.text
        val (ls, le) = lineBounds(t, v.selection.min)
        val line = t.substring(ls, le)
        val m = Regex("^(#{1,6}) ").find(line)
        val level = m?.groupValues?.get(1)?.length ?: 0
        val stripped = if (m != null) line.substring(m.value.length) else line
        val newLine = if (level in 1..2) "#".repeat(level + 1) + " " + stripped
        else if (level == 0) "# $stripped" else stripped
        val delta = newLine.length - line.length
        val nt = t.replaceRange(ls, le, newLine)
        return TextFieldValue(nt, TextRange((v.selection.end + delta).coerceIn(ls, nt.length)))
    }

    private val listItem = Regex("^(\\s*)(([-*+]) \\[[ xX]\\] |[-*+] |(\\d+)([.)]) )")

    /**
     * When Enter is pressed on a list line, start the next item. Enter on an empty item ends the list,
     * the same behaviour as Obsidian's editor.
     */
    fun continueList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        if (new.text.length != old.text.length + 1 || !new.selection.collapsed) return new
        val c = new.selection.start
        if (c == 0 || new.text[c - 1] != '\n') return new
        if (new.text.removeRange(c - 1, c) != old.text) return new

        val prevStart = if (c - 1 == 0) 0 else new.text.lastIndexOf('\n', c - 2) + 1
        val prevLine = new.text.substring(prevStart, c - 1)
        val m = listItem.find(prevLine) ?: return new

        if (prevLine.length == m.value.length) {
            // Empty item: remove the marker and the new line break.
            val nt = new.text.substring(0, prevStart) + new.text.substring(c)
            return TextFieldValue(nt, TextRange(prevStart))
        }
        val indent = m.groupValues[1]
        val next = when {
            m.groupValues[3].isNotEmpty() -> indent + m.groupValues[3] + " [ ] "
            m.groupValues[4].isNotEmpty() -> {
                val n = m.groupValues[4].toLongOrNull()?.plus(1) ?: 1
                indent + n + m.groupValues[5] + " "
            }
            else -> indent + m.groupValues[2]
        }
        val nt = new.text.substring(0, c) + next + new.text.substring(c)
        return TextFieldValue(nt, TextRange(c + next.length))
    }
}
