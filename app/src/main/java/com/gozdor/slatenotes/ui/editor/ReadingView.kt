package com.gozdor.slatenotes.ui.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gozdor.slatenotes.data.Md
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.file.FileSchemeHandler
import java.io.File

private const val LINK = "slate-link:"
private const val TAG = "slate-tag:"

private val imageExt = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

/** Turns Obsidian-specific syntax into plain Markdown links Markwon can render and we can intercept. */
object ReadingPreprocessor {
    private val fence = Regex("^\\s*(```|~~~)")

    fun prepare(body: String, resolveEmbed: (String) -> File?): String {
        val out = StringBuilder()
        var inFence = false
        val lines = body.split('\n')
        lines.forEachIndexed { i, line ->
            if (fence.containsMatchIn(line)) {
                inFence = !inFence
                out.append(line)
            } else if (inFence) {
                out.append(line)
            } else {
                // Even segments are outside `inline code`.
                val parts = line.split('`')
                parts.forEachIndexed { pi, part ->
                    if (pi > 0) out.append('`')
                    out.append(if (pi % 2 == 0 || pi == parts.lastIndex && parts.size % 2 == 0) transform(part, resolveEmbed) else part)
                }
            }
            if (i < lines.lastIndex) out.append('\n')
        }
        return out.toString()
    }

    private fun escape(s: String) = s.replace("[", "\\[").replace("]", "\\]")

    private fun transform(seg: String, resolveEmbed: (String) -> File?): String {
        var s = Md.wikiLink.replace(seg) { m ->
            val embed = m.groupValues[1] == "!"
            val inner = m.groupValues[2]
            val target = Md.linkTarget(inner)
            val alias = inner.substringAfter('|', "").trim()
            if (embed) {
                val f = resolveEmbed(inner)
                if (f != null && f.extension.lowercase() in imageExt) {
                    "![${escape(f.nameWithoutExtension)}](${Uri.fromFile(f)})"
                } else {
                    "[↪ ${escape(alias.ifEmpty { target })}]($LINK${Uri.encode(target)})"
                }
            } else {
                val shown = alias.ifEmpty {
                    val beforeAlias = inner.substringBefore('|').trim()
                    if (beforeAlias.contains('#')) beforeAlias.replace("#", " › ") else target
                }
                "[${escape(shown)}]($LINK${Uri.encode(target)})"
            }
        }
        s = Md.inlineTag.replace(s) { m ->
            val tag = m.groupValues[2]
            "${m.groupValues[1]}[#$tag]($TAG${Uri.encode(tag)})"
        }
        return s
    }
}

fun buildMarkwon(
    context: Context,
    accent: Int,
    codeBg: Int,
    text: Int,
    dim: Int,
    onLink: (String) -> Unit,
): Markwon =
    Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(ImagesPlugin.create { plugin -> plugin.addSchemeHandler(FileSchemeHandler.create()) })
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder
                    .linkColor(accent)
                    .codeTextColor(text)
                    .codeBackgroundColor(codeBg)
                    .codeBlockTextColor(text)
                    .codeBlockBackgroundColor(codeBg)
                    .blockQuoteColor(accent)
                    .thematicBreakColor(dim)
                    .headingBreakColor(0)
            }

            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link -> onLink(link) }
            }
        })
        .build()

@Composable
fun ReadingView(
    text: String,
    resolveEmbed: (String) -> File?,
    onOpenLink: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val fm = remember(text) { Md.frontmatter(text) }
    val body = remember(text, fm) { text.substring(fm?.end ?: 0) }
    val prepared = remember(body) { ReadingPreprocessor.prepare(body, resolveEmbed) }

    val markwon = remember(cs) {
        buildMarkwon(
            context,
            accent = cs.primary.toArgb(),
            codeBg = cs.surfaceVariant.toArgb(),
            text = cs.onSurface.toArgb(),
            dim = cs.outline.toArgb(),
        ) { link ->
            when {
                link.startsWith(LINK) -> onOpenLink(Uri.decode(link.removePrefix(LINK)))
                link.startsWith(TAG) -> onOpenTag(Uri.decode(link.removePrefix(TAG)))
                else -> openExternal(context, link)
            }
        }
    }

    Column(modifier) {
        if (fm != null && fm.entries.isNotEmpty()) {
            Properties(fm)
            Spacer(Modifier.height(12.dp))
        }
        val textColor = cs.onSurface.toArgb()
        AndroidView(
            factory = { ctx ->
                TextView(ctx).apply {
                    textSize = 16f
                    setLineSpacing(0f, 1.3f)
                }
            },
            update = { tv ->
                tv.setTextColor(textColor)
                markwon.setMarkdown(tv, prepared)
                tv.movementMethod = LinkMovementMethod.getInstance()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Properties(fm: Md.Frontmatter) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .background(cs.surfaceContainer, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text("Properties", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        fm.entries.forEach { (k, v) ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.width(110.dp))
                Text(v.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
            }
        }
    }
}

private fun openExternal(context: Context, link: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    } catch (_: SecurityException) {
    }
}
