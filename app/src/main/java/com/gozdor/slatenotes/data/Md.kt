package com.gozdor.slatenotes.data

/**
 * Markdown helpers written against Obsidian's public docs for its syntax:
 * YAML frontmatter ("properties"), #tags (frontmatter and inline), and [[wikilinks]].
 */
object Md {

    data class Frontmatter(
        val entries: List<Pair<String, List<String>>>,
        /** Index in the note text just after the closing "---" line. */
        val end: Int,
    ) {
        fun values(key: String): List<String> =
            entries.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second ?: emptyList()
    }

    /** Tags: must contain at least one non-digit; letters, digits, _, -, / allowed. */
    val inlineTag = Regex("(^|[\\s(,])#([\\p{L}\\p{N}_/-]*[\\p{L}_/-][\\p{L}\\p{N}_/-]*)")

    /** Group 1 = "!" for embeds, group 2 = inner text. */
    val wikiLink = Regex("(!?)\\[\\[([^\\[\\]\\n]+?)\\]\\]")

    private val fence = Regex("^\\s*(```|~~~)")
    private val inlineCode = Regex("`[^`\\n]*`")

    fun frontmatter(text: String): Frontmatter? {
        if (!text.startsWith("---")) return null
        val firstNl = text.indexOf('\n')
        if (firstNl < 0) return null
        if (text.substring(0, firstNl).trimEnd() != "---") return null
        var pos = firstNl + 1
        val lines = mutableListOf<String>()
        while (pos <= text.length) {
            val nl = text.indexOf('\n', pos)
            val lineEnd = if (nl < 0) text.length else nl
            val line = text.substring(pos, lineEnd).trimEnd('\r')
            if (line.trimEnd() == "---") {
                val end = if (nl < 0) text.length else nl + 1
                return Frontmatter(parseYaml(lines), end)
            }
            lines += line
            if (nl < 0) break
            pos = nl + 1
        }
        return null
    }

    /** Small YAML subset: "key: value", "key: [a, b]", and "key:" followed by "- item" lines. */
    private fun parseYaml(lines: List<String>): List<Pair<String, List<String>>> {
        val out = mutableListOf<Pair<String, MutableList<String>>>()
        for (raw in lines) {
            if (raw.isBlank() || raw.trimStart().startsWith("#")) continue
            val trimmed = raw.trim()
            if (trimmed == "-" || trimmed.startsWith("- ")) {
                val item = unquote(trimmed.removePrefix("-").trim())
                if (item.isNotEmpty()) out.lastOrNull()?.second?.add(item)
                continue
            }
            if (raw[0].isWhitespace()) {
                val list = out.lastOrNull()?.second ?: continue
                if (list.isEmpty()) list.add(trimmed) else list[list.size - 1] = list.last() + " " + trimmed
                continue
            }
            val idx = raw.indexOf(':')
            if (idx <= 0) continue
            val key = raw.substring(0, idx).trim()
            val v = raw.substring(idx + 1).trim()
            val list = mutableListOf<String>()
            when {
                v.isEmpty() -> Unit
                v.startsWith("[") && v.endsWith("]") ->
                    v.substring(1, v.length - 1).split(',')
                        .map { unquote(it.trim()) }
                        .filterTo(list) { it.isNotEmpty() }
                else -> list.add(unquote(v))
            }
            out += key to list
        }
        return out.map { it.first to it.second.toList() }
    }

    private fun unquote(s: String): String =
        if (s.length >= 2 && ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith('\'') && s.endsWith('\''))))
            s.substring(1, s.length - 1) else s

    /** Blanks out fenced and inline code with spaces, keeping every character position the same. */
    fun stripCode(text: String): String {
        val sb = StringBuilder(text.length)
        var inFence = false
        var start = 0
        while (start <= text.length) {
            val nl = text.indexOf('\n', start)
            val end = if (nl < 0) text.length else nl
            val line = text.substring(start, end)
            if (fence.containsMatchIn(line)) {
                inFence = !inFence
                sb.append(" ".repeat(line.length))
            } else if (inFence) {
                sb.append(" ".repeat(line.length))
            } else {
                sb.append(inlineCode.replace(line) { " ".repeat(it.value.length) })
            }
            if (nl < 0) break
            sb.append('\n')
            start = nl + 1
        }
        return sb.toString()
    }

    fun tags(text: String, fm: Frontmatter?): Set<String> {
        val set = LinkedHashSet<String>()
        if (fm != null) {
            (fm.values("tags") + fm.values("tag"))
                .flatMap { it.split(',', ' ') }
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotEmpty() }
                .forEach { set.add(it) }
        }
        val body = stripCode(text.substring(fm?.end ?: 0))
        inlineTag.findAll(body).forEach { set.add(it.groupValues[2]) }
        return set
    }

    fun links(text: String, fm: Frontmatter?): List<String> {
        val body = stripCode(text.substring(fm?.end ?: 0))
        return wikiLink.findAll(body)
            .map { linkTarget(it.groupValues[2]) }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    fun aliases(fm: Frontmatter?): List<String> {
        if (fm == null) return emptyList()
        return (fm.values("aliases") + fm.values("alias")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** "Note#Heading|Alias" -> "Note" */
    fun linkTarget(inner: String): String =
        inner.substringBefore('|').substringBefore('#').substringBefore('^').trim()
}
