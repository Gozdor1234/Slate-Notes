package com.gozdor.slatenotes.data

import java.io.File

data class IndexedNote(
    val file: File,
    val rel: String,
    val name: String,
    val modified: Long,
    val content: String,
    val lower: String,
    val tags: Set<String>,
    val links: List<String>,
    val aliases: List<String>,
) {
    companion object {
        fun of(f: File, rel: String, text: String): IndexedNote {
            val fm = Md.frontmatter(text)
            return IndexedNote(
                file = f,
                rel = rel,
                name = f.nameWithoutExtension,
                modified = f.lastModified(),
                content = text,
                lower = text.lowercase(),
                tags = Md.tags(text, fm),
                links = Md.links(text, fm),
                aliases = Md.aliases(fm),
            )
        }
    }
}

data class SearchHit(val note: IndexedNote, val snippet: String?)

class VaultIndex(
    /** Keyed by absolute path. */
    val notes: Map<String, IndexedNote>,
    /** Non-Markdown files keyed by lowercase file name. */
    val attachments: Map<String, File>,
) {
    /** Tag (display casing of first occurrence) to number of notes using it. */
    val tagCounts: List<Pair<String, Int>> by lazy {
        val display = LinkedHashMap<String, String>()
        val counts = HashMap<String, Int>()
        for (n in notes.values) {
            for (t in n.tags.map { it }.distinctBy { it.lowercase() }) {
                val k = t.lowercase()
                display.putIfAbsent(k, t)
                counts[k] = (counts[k] ?: 0) + 1
            }
        }
        display.entries.map { it.value to (counts[it.key] ?: 0) }.sortedBy { it.first.lowercase() }
    }

    /** Resolves a [[link]] target the way Obsidian does: path match if it has "/", otherwise by file name, shortest path wins. */
    fun resolve(target: String): IndexedNote? {
        val t = target.trim().removeSuffix(".md").replace('\\', '/').trimStart('/')
        if (t.isEmpty()) return null
        val tl = t.lowercase()
        val candidates = notes.values.filter { n ->
            val relNoExt = n.rel.removeSuffix(".md").lowercase()
            if (tl.contains('/')) relNoExt == tl || relNoExt.endsWith("/$tl") else n.name.lowercase() == tl
        }
        return candidates.minByOrNull { it.rel.length }
            ?: notes.values.firstOrNull { n -> n.aliases.any { it.equals(t, ignoreCase = true) } }
    }

    fun resolveAttachment(root: File, target: String): File? {
        val t = target.substringBefore('|').substringBefore('#').trim()
        if (t.isEmpty()) return null
        if (t.contains('/')) {
            val f = File(root, t)
            if (f.isFile) return f
        }
        return attachments[t.substringAfterLast('/').lowercase()]
    }

    fun withNote(n: IndexedNote): VaultIndex = VaultIndex(notes + (n.file.absolutePath to n), attachments)

    /**
     * Plain words must all appear in the title or body (case-insensitive).
     * "tag:#name" (or "tag:name") filters to notes with that tag or a nested tag under it.
     */
    fun search(query: String, limit: Int = 200): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val tokens = q.split(Regex("\\s+"))
        val tagTokens = tokens.filter { it.startsWith("tag:", ignoreCase = true) }
            .map { it.substring(4).removePrefix("#").lowercase() }
            .filter { it.isNotEmpty() }
        val words = tokens.filterNot { it.startsWith("tag:", ignoreCase = true) }.map { it.lowercase() }

        data class Scored(val note: IndexedNote, val nameMatch: Boolean, val snippet: String?)

        val hits = notes.values.mapNotNull { n ->
            val tagsLower = n.tags.map { it.lowercase() }
            if (tagTokens.any { t -> tagsLower.none { it == t || it.startsWith("$t/") } }) return@mapNotNull null
            val nameL = n.name.lowercase()
            if (!words.all { nameL.contains(it) || n.lower.contains(it) }) return@mapNotNull null
            val nameMatch = words.isNotEmpty() && words.all { nameL.contains(it) }
            val snippet = words.firstNotNullOfOrNull { w ->
                val i = n.lower.indexOf(w)
                if (i < 0) null else snippetAt(n.content, i, w.length)
            }
            Scored(n, nameMatch, snippet)
        }
        return hits
            .sortedWith(compareByDescending<Scored> { it.nameMatch }.thenByDescending { it.note.modified })
            .take(limit)
            .map { SearchHit(it.note, it.snippet) }
    }

    private fun snippetAt(content: String, index: Int, len: Int): String {
        val i = index.coerceIn(0, content.length)
        val start = (i - 40).coerceAtLeast(0)
        val end = (i + len + 80).coerceAtMost(content.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < content.length) "…" else ""
        return prefix + content.substring(start, end).replace(Regex("\\s+"), " ").trim() + suffix
    }

    companion object {
        val EMPTY = VaultIndex(emptyMap(), emptyMap())
    }
}
