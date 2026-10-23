package com.gozdor.slatenotes.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.gozdor.slatenotes.data.Md

data class HighlightColors(
    val accent: Color,
    val dim: Color,
    val codeBg: Color,
    val highlightBg: Color,
)

/**
 * Styles Markdown in place while editing (headings bigger, syntax markers dimmed, links and tags tinted).
 * It never changes the text, so cursor positions map 1:1.
 */
class MarkdownHighlighter(private val c: HighlightColors) : VisualTransformation {
    private var lastText: String? = null
    private var lastResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val cached = lastResult
        val out = if (s == lastText && cached != null) cached else highlight(s).also {
            lastText = s
            lastResult = it
        }
        return TransformedText(out, OffsetMapping.Identity)
    }

    private val mono = SpanStyle(fontFamily = FontFamily.Monospace)
    private val heading = Regex("^(#{1,6})\\s")
    private val quote = Regex("^\\s*(>+)")
    private val listMarker = Regex("^\\s*([-*+]|\\d+[.)])\\s")
    private val task = Regex("^\\s*[-*+]\\s\\[([ xX])\\]")
    private val fence = Regex("^\\s*(```|~~~)")
    private val inlineCode = Regex("`[^`\\n]+`")
    private val bold = Regex("(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\1")
    private val italic = Regex("(?<![*\\w])([*_])(?=\\S)([^*_\\n]+?)(?<=\\S)\\1(?![*\\w])")
    private val strike = Regex("~~(?=\\S)(.+?)(?<=\\S)~~")
    private val mark = Regex("==(?=\\S)(.+?)(?<=\\S)==")
    private val mdLink = Regex("!?\\[[^\\]\\n]*\\]\\([^)\\n]*\\)")
    private val url = Regex("https?://[^\\s)>\\]]+")

    private val headingSizes = listOf(1.6f, 1.4f, 1.25f, 1.15f, 1.07f, 1.0f)

    fun highlight(s: String): AnnotatedString = buildAnnotatedString {
        append(s)
        var bodyStart = 0
        Md.frontmatter(s)?.let { fm ->
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = c.dim), 0, fm.end)
            bodyStart = fm.end
        }

        var inFence = false
        var start = bodyStart
        while (start <= s.length) {
            val nl = s.indexOf('\n', start)
            val end = if (nl < 0) s.length else nl
            val line = s.substring(start, end)

            if (fence.containsMatchIn(line)) {
                inFence = !inFence
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = c.dim), start, end)
            } else if (inFence) {
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = c.codeBg), start, end)
            } else {
                styleLine(line, start)
            }
            if (nl < 0) break
            start = nl + 1
        }
    }

    private fun AnnotatedString.Builder.styleLine(line: String, off: Int) {
        heading.find(line)?.let { m ->
            val level = m.groupValues[1].length
            addStyle(
                SpanStyle(fontSize = headingSizes[level - 1].em, fontWeight = FontWeight.Bold),
                off, off + line.length,
            )
            addStyle(SpanStyle(color = c.dim), off, off + m.groupValues[1].length)
        }
        quote.find(line)?.let { m ->
            addStyle(SpanStyle(color = c.accent), off + m.range.first, off + m.range.last + 1)
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), off, off + line.length)
        }
        task.find(line)?.let { m ->
            if (m.groupValues[1].equals("x", ignoreCase = true)) {
                addStyle(
                    SpanStyle(color = c.dim, textDecoration = TextDecoration.LineThrough),
                    off + m.range.last + 1, off + line.length,
                )
            }
        }
        listMarker.find(line)?.let { m ->
            val g = m.groups[1]!!
            addStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold), off + g.range.first, off + g.range.last + 1)
        }

        // Inline styles. Code spans first so later rules can skip them.
        val codeRanges = inlineCode.findAll(line).map { it.range }.toList()
        for (r in codeRanges) {
            addStyle(mono.copy(background = c.codeBg), off + r.first, off + r.last + 1)
        }
        fun inCode(i: Int) = codeRanges.any { i in it }

        fun wrapped(re: Regex, marker: Int, style: SpanStyle) {
            for (m in re.findAll(line)) {
                if (inCode(m.range.first)) continue
                val a = off + m.range.first
                val b = off + m.range.last + 1
                addStyle(style, a, b)
                addStyle(SpanStyle(color = c.dim), a, a + marker)
                addStyle(SpanStyle(color = c.dim), b - marker, b)
            }
        }
        wrapped(bold, 2, SpanStyle(fontWeight = FontWeight.Bold))
        wrapped(italic, 1, SpanStyle(fontStyle = FontStyle.Italic))
        wrapped(strike, 2, SpanStyle(textDecoration = TextDecoration.LineThrough))
        wrapped(mark, 2, SpanStyle(background = c.highlightBg))

        for (m in Md.wikiLink.findAll(line)) {
            if (inCode(m.range.first)) continue
            val a = off + m.range.first
            val b = off + m.range.last + 1
            val markerLen = if (m.groupValues[1] == "!") 3 else 2
            addStyle(SpanStyle(color = c.accent), a, b)
            addStyle(SpanStyle(color = c.dim), a, a + markerLen)
            addStyle(SpanStyle(color = c.dim), b - 2, b)
        }
        for (m in mdLink.findAll(line)) {
            if (inCode(m.range.first)) continue
            addStyle(SpanStyle(color = c.accent), off + m.range.first, off + m.range.last + 1)
        }
        for (m in url.findAll(line)) {
            if (inCode(m.range.first)) continue
            addStyle(SpanStyle(color = c.accent, textDecoration = TextDecoration.Underline), off + m.range.first, off + m.range.last + 1)
        }
        for (m in Md.inlineTag.findAll(line)) {
            val g = m.groups[2] ?: continue
            if (inCode(g.range.first)) continue
            // include the "#" just before the tag name
            addStyle(SpanStyle(color = c.accent), off + g.range.first - 1, off + g.range.last + 1)
        }
    }
}
