package com.gozdor.slatenotes.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.data.Md
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.editor.Formatting
import com.gozdor.slatenotes.ui.editor.HighlightColors
import com.gozdor.slatenotes.ui.editor.MarkdownHighlighter
import com.gozdor.slatenotes.ui.theme.Lucide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DesktopNoteView(vm: AppViewModel, tabId: Long, path: String) {
    val file = remember(path) { File(path) }
    val cs = MaterialTheme.colorScheme
    val settings = vm.settings
    val reading = vm.isReading(tabId)

    var loaded by remember(path) { mutableStateOf(false) }
    var missing by remember(path) { mutableStateOf(false) }
    var value by remember(path) { mutableStateOf(TextFieldValue("")) }
    var lastSaved by remember(path) { mutableStateOf("") }
    var title by remember(path) { mutableStateOf(TextFieldValue(file.nameWithoutExtension)) }
    var titleFocused by remember(path) { mutableStateOf(false) }
    var pendingName by remember(path) { mutableStateOf<String?>(null) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var bodyTop by remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    val highlighter = remember(cs) {
        MarkdownHighlighter(
            HighlightColors(
                accent = cs.primary,
                dim = cs.onSurfaceVariant.copy(alpha = 0.6f),
                codeBg = cs.surfaceVariant,
                highlightBg = cs.primary.copy(alpha = 0.25f),
            )
        )
    }

    LaunchedEffect(path) {
        val t = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrNull() }
        if (t == null) {
            missing = true
            return@LaunchedEffect
        }
        value = TextFieldValue(t)
        lastSaved = t
        loaded = true
        if (vm.justCreated == path) {
            vm.justCreated = null
            title = title.copy(selection = TextRange(0, title.text.length))
            titleFocus.requestFocus()
        } else if (!reading) {
            bodyFocus.requestFocus()
        }
    }

    // Pick up edits that arrived from elsewhere (e.g. synced from another device), but never
    // overwrite text you've typed and not saved yet.
    LaunchedEffect(vm.index) {
        if (!loaded || value.text != lastSaved) return@LaunchedEffect
        val disk = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrNull() } ?: return@LaunchedEffect
        if (disk != lastSaved && value.text == lastSaved) {
            value = TextFieldValue(disk, TextRange(value.selection.start.coerceAtMost(disk.length)))
            lastSaved = disk
        }
    }

    LaunchedEffect(value.text) {
        if (!loaded || value.text == lastSaved) return@LaunchedEffect
        delay(600)
        vm.saveNote(path, value.text)
        lastSaved = value.text
    }
    DisposableEffect(path) {
        onDispose { if (loaded && value.text != lastSaved) vm.saveNote(path, value.text) }
    }

    fun commitTitle() {
        val newName = title.text.trim()
        if (newName == pendingName) return
        if (newName.isEmpty() || newName == file.nameWithoutExtension) {
            title = TextFieldValue(file.nameWithoutExtension)
            return
        }
        pendingName = newName
        lastSaved = value.text
        vm.renameNote(path, newName, value.text) {
            pendingName = null
            title = TextFieldValue(file.nameWithoutExtension)
        }
    }

    val startRename = remember { vm.renameRequest }
    LaunchedEffect(vm.renameRequest) {
        if (vm.renameRequest == startRename) return@LaunchedEffect
        delay(50)
        title = title.copy(selection = TextRange(0, title.text.length))
        titleFocus.requestFocus()
    }

    // Outline clicks jump to a heading.
    LaunchedEffect(vm.jump, loaded) {
        val j = vm.jump ?: return@LaunchedEffect
        if (j.path != path || !loaded) return@LaunchedEffect
        vm.jump = null
        vm.setReading(tabId, false)
        val off = j.offset.coerceIn(0, value.text.length)
        value = value.copy(selection = TextRange(off))
        delay(120)
        bodyFocus.requestFocus()
        layout?.let { l -> scroll.animateScrollTo((bodyTop + l.getLineTop(l.getLineForOffset(off))).toInt().coerceAtLeast(0)) }
    }

    LaunchedEffect(vm.insertRequest) {
        val ins = vm.insertRequest ?: return@LaunchedEffect
        vm.insertRequest = null
        if (reading || !loaded) return@LaunchedEffect
        val s = value.selection.min
        val e = value.selection.max
        value = TextFieldValue(value.text.replaceRange(s, e, ins), TextRange(s + ins.length))
    }

    Column(Modifier.fillMaxSize()) {
        ViewHeader(vm, path) {
            HoverIcon(
                if (reading) Lucide.Pencil else Lucide.BookOpen,
                if (reading) "Current view: reading. Click to edit (Ctrl+E)" else "Current view: editing. Click to read (Ctrl+E)",
            ) { vm.setReading(tabId, !reading) }
            Box {
                HoverIcon(Lucide.EllipsisVertical, "More options") { menu = true }
                DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(10.dp), containerColor = cs.surface) {
                    MenuItem(if (vm.isBookmarked(path)) "Remove bookmark" else "Bookmark") { menu = false; vm.toggleBookmark(path) }
                    MenuItem("Rename…") { menu = false; vm.requestRename() }
                    MenuItem("Move file to…") { menu = false; vm.moveTarget = file }
                    MenuItem("Open in new tab") { menu = false; vm.openNote(file, newTab = true) }
                    HorizontalDivider(color = cs.outlineVariant)
                    MenuItem("Delete file") {
                        menu = false
                        if (vm.settings.confirmDelete) confirmDelete = true else vm.delete(file)
                    }
                }
            }
        }

        if (missing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This file can't be opened.", color = cs.onSurfaceVariant)
            }
            return@Column
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Readable line length, like Obsidian's default.
            Column(
                Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
            ) {
                if (settings.showInlineTitle) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it.copy(text = it.text.replace("\n", "")) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = cs.onSurface, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            commitTitle()
                            bodyFocus.requestFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, bottom = 16.dp)
                            .focusRequester(titleFocus)
                            .onPreviewKeyEvent {
                                if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                                    commitTitle()
                                    bodyFocus.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            .onFocusChanged {
                                if (titleFocused && !it.isFocused) commitTitle()
                                titleFocused = it.isFocused
                            },
                    )
                } else {
                    Spacer(Modifier.height(28.dp))
                }

                if (reading) {
                    ReadingView(vm, value.text, settings.fontSize) { line ->
                        value = TextFieldValue(toggleTask(value.text, line), value.selection)
                    }
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = { nv -> value = if (settings.continueLists) Formatting.continueList(value, nv) else nv },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = cs.onSurface,
                            fontSize = settings.fontSize.sp,
                            lineHeight = (settings.fontSize * 1.5f).sp,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        visualTransformation = highlighter,
                        onTextLayout = { layout = it },
                        decorationBox = { inner ->
                            Box {
                                if (value.text.isEmpty()) {
                                    Text("Start writing…", color = cs.onSurfaceVariant.copy(alpha = 0.6f), fontSize = settings.fontSize.sp)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 400.dp)
                            .focusRequester(bodyFocus)
                            .onGloballyPositioned { bodyTop = it.positionInParent().y }
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                val op: ((TextFieldValue) -> TextFieldValue)? = when {
                                    e.isCtrlPressed && e.key == Key.B -> { v -> Formatting.wrap(v, "**") }
                                    e.isCtrlPressed && e.key == Key.I -> { v -> Formatting.wrap(v, "*") }
                                    e.isCtrlPressed && e.key == Key.K -> { v -> Formatting.wrap(v, "[", "]()") }
                                    !e.isCtrlPressed && e.key == Key.Tab -> { v -> Formatting.indent(v, !e.isShiftPressed) }
                                    else -> null
                                }
                                if (op != null) {
                                    value = op(value)
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                }
                Spacer(Modifier.height(240.dp))
            }
        }
    }

    if (confirmDelete) {
        com.gozdor.slatenotes.ui.screens.ConfirmDialog(
            title = "Delete \"${file.nameWithoutExtension}\"?",
            message = if (vm.settings.deletePermanently) "This can't be undone." else "It will be moved to the vault's .trash folder.",
            confirm = "Delete",
            onDismiss = { confirmDelete = false },
        ) {
            confirmDelete = false
            lastSaved = value.text
            vm.delete(file)
        }
    }
}

/** Flips "- [ ]" and "- [x]" on one line (0-based, counting from the top of the file). */
fun toggleTask(text: String, line: Int): String {
    val lines = text.split('\n').toMutableList()
    if (line !in lines.indices) return text
    val l = lines[line]
    lines[line] = when {
        Regex("^(\\s*[-*+]\\s)\\[ \\]").containsMatchIn(l) -> l.replaceFirst("[ ]", "[x]")
        Regex("^(\\s*[-*+]\\s)\\[[xX]\\]").containsMatchIn(l) -> l.replaceFirst(Regex("\\[[xX]\\]"), "[ ]")
        else -> l
    }
    return lines.joinToString("\n")
}

// ------------------------------------------------------------------ reading view

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Para(val text: String) : Block
    data class Item(val indent: Int, val marker: String, val task: Boolean?, val text: String, val line: Int) : Block
    data class Quote(val text: String) : Block
    data class Code(val text: String) : Block
    data object Rule : Block
    data class Img(val target: String) : Block
    data class Table(val rows: List<List<String>>) : Block
}

private val imageExt = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

/** Splits a note body into blocks. [firstLine] is the body's line number in the whole file. */
private fun parseBlocks(body: String, firstLine: Int): List<Block> {
    val out = mutableListOf<Block>()
    val lines = body.split('\n')
    val para = StringBuilder()
    fun flush() {
        if (para.isNotBlank()) out += Block.Para(para.toString().trimEnd())
        para.clear()
    }
    var i = 0
    while (i < lines.size) {
        val l = lines[i]
        val t = l.trim()
        when {
            t.startsWith("```") || t.startsWith("~~~") -> {
                flush()
                val fence = t.take(3)
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(fence)) {
                    code.append(lines[i]).append('\n')
                    i++
                }
                out += Block.Code(code.toString().trimEnd('\n'))
            }
            t.isEmpty() -> flush()
            Regex("^#{1,6}\\s+").containsMatchIn(t) -> {
                flush()
                val level = t.takeWhile { it == '#' }.length
                out += Block.Heading(level, t.drop(level).trim().trimEnd('#').trim())
            }
            Regex("^(---+|\\*\\*\\*+|___+)$").matches(t) -> {
                flush()
                out += Block.Rule
            }
            Regex("^!\\[\\[([^\\]]+)]]$").matches(t) -> {
                flush()
                out += Block.Img(t.removePrefix("![[").removeSuffix("]]"))
            }
            t.startsWith(">") -> {
                flush()
                val q = StringBuilder()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    q.append(lines[i].trim().removePrefix(">").removePrefix(" ")).append('\n')
                    i++
                }
                out += Block.Quote(q.toString().trimEnd())
                continue
            }
            t.startsWith("|") && i + 1 < lines.size && Regex("^\\|?\\s*:?-+").containsMatchIn(lines[i + 1].trim()) -> {
                flush()
                val rows = mutableListOf<List<String>>()
                rows += cells(t)
                i += 2
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows += cells(lines[i].trim())
                    i++
                }
                out += Block.Table(rows)
                continue
            }
            else -> {
                val indent = l.length - l.trimStart().length
                val task = Regex("^[-*+]\\s\\[([ xX])]\\s?(.*)$").find(t)
                val bullet = Regex("^[-*+]\\s(.*)$").find(t)
                val num = Regex("^(\\d+)[.)]\\s(.*)$").find(t)
                when {
                    task != null -> {
                        flush()
                        out += Block.Item(indent, "", task.groupValues[1] != " ", task.groupValues[2], firstLine + i)
                    }
                    bullet != null -> {
                        flush()
                        out += Block.Item(indent, "•", null, bullet.groupValues[1], firstLine + i)
                    }
                    num != null -> {
                        flush()
                        out += Block.Item(indent, num.groupValues[1] + ".", null, num.groupValues[2], firstLine + i)
                    }
                    else -> {
                        if (para.isNotEmpty()) para.append('\n')
                        para.append(l.trim())
                    }
                }
            }
        }
        i++
    }
    flush()
    return out
}

private fun cells(row: String): List<String> = row.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

private val inlinePattern = Regex(
    "(`[^`]+`)|(\\*\\*[^*]+\\*\\*)|(__[^_]+__)|(~~[^~]+~~)|(==[^=]+==)|(\\*[^*\\s][^*]*\\*)|(_[^_\\s][^_]*_)" +
        "|(\\[\\[[^\\]]+]])|(\\[[^\\]]+]\\([^)]+\\))|(https?://[^\\s)]+)|((?<=^|\\s)#[\\p{L}\\p{N}_/-]*[\\p{L}_/-][\\p{L}\\p{N}_/-]*)",
)

/** Inline Markdown to styled text with clickable [[links]], #tags and web links. */
@Composable
private fun inlineMd(text: String, vm: AppViewModel): AnnotatedString {
    val cs = MaterialTheme.colorScheme
    val linkStyle = TextLinkStyles(SpanStyle(color = cs.primary))
    return remember(text, cs) {
        buildAnnotatedString {
            var last = 0
            for (m in inlinePattern.findAll(text)) {
                append(text.substring(last, m.range.first))
                val s = m.value
                when {
                    s.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = cs.surfaceVariant)) { append(s.trim('`')) }
                    s.startsWith("**") || s.startsWith("__") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.drop(2).dropLast(2)) }
                    s.startsWith("~~") -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(s.drop(2).dropLast(2)) }
                    s.startsWith("==") -> withStyle(SpanStyle(background = cs.primary.copy(alpha = 0.25f))) { append(s.drop(2).dropLast(2)) }
                    s.startsWith("[[") -> {
                        val inner = s.removePrefix("[[").removeSuffix("]]")
                        val target = Md.linkTarget(inner)
                        val shown = inner.substringAfter('|', "").ifEmpty { inner.substringBefore('|').replace("#", " › ") }
                        withLink(LinkAnnotation.Clickable("note", linkStyle) { vm.openLink(target) }) { append(shown) }
                    }
                    s.startsWith("[") -> {
                        val label = s.substring(1, s.indexOf("]("))
                        val url = s.substring(s.indexOf("](") + 2, s.length - 1)
                        withLink(LinkAnnotation.Clickable("url", linkStyle) { vm.platform.openUrl(url) }) { append(label) }
                    }
                    s.startsWith("http") -> withLink(LinkAnnotation.Clickable("url", linkStyle) { vm.platform.openUrl(s) }) { append(s) }
                    s.startsWith("#") -> withLink(LinkAnnotation.Clickable("tag", linkStyle) { vm.openTag(s.drop(1)) }) { append(s) }
                    else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.drop(1).dropLast(1)) }
                }
                last = m.range.last + 1
            }
            append(text.substring(last))
        }
    }
}

@Composable
private fun ReadingView(vm: AppViewModel, text: String, fontSize: Int, onToggleTask: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fm = remember(text) { Md.frontmatter(text) }
    val firstLine = remember(text, fm) { text.substring(0, fm?.end ?: 0).count { it == '\n' } }
    val blocks = remember(text, fm) { parseBlocks(text.substring(fm?.end ?: 0), firstLine) }
    val base = fontSize.sp
    Column {
        if (fm != null && fm.entries.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, cs.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Text("Properties", fontSize = 12.sp, color = cs.onSurfaceVariant)
                fm.entries.forEach { (k, v) ->
                    Row(Modifier.padding(top = 4.dp)) {
                        Text(k, fontSize = 13.sp, color = cs.onSurfaceVariant, modifier = Modifier.width(140.dp))
                        Text(v.joinToString(", "), fontSize = 13.sp)
                    }
                }
            }
        }
        for (b in blocks) {
            when (b) {
                is Block.Heading -> Text(
                    inlineMd(b.text, vm),
                    fontSize = (fontSize * listOf(1.8f, 1.5f, 1.3f, 1.15f, 1.05f, 1f)[b.level - 1]).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                is Block.Para -> Text(inlineMd(b.text, vm), fontSize = base, lineHeight = (fontSize * 1.5f).sp, modifier = Modifier.padding(vertical = 6.dp))
                is Block.Item -> Row(
                    Modifier.padding(start = (b.indent * 6).dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (b.task != null) {
                        Checkbox(
                            checked = b.task,
                            onCheckedChange = { onToggleTask(b.line) },
                            colors = CheckboxDefaults.colors(checkedColor = cs.primary),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Text(b.marker, fontSize = base, color = cs.primary, modifier = Modifier.width(22.dp))
                    }
                    Text(
                        inlineMd(b.text, vm),
                        fontSize = base,
                        color = if (b.task == true) cs.onSurfaceVariant else cs.onSurface,
                        textDecoration = if (b.task == true) TextDecoration.LineThrough else null,
                    )
                }
                is Block.Quote -> Row(Modifier.padding(vertical = 6.dp)) {
                    Box(Modifier.width(3.dp).height(24.dp * (b.text.count { it == '\n' } + 1)).background(cs.primary))
                    Spacer(Modifier.width(12.dp))
                    Text(inlineMd(b.text, vm), fontSize = base, fontStyle = FontStyle.Italic)
                }
                is Block.Code -> Text(
                    b.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (fontSize - 2).sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(cs.surfaceVariant, RoundedCornerShape(6.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                )
                Block.Rule -> HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                is Block.Img -> {
                    val f = remember(b.target, vm.index) { vm.resolveAttachment(b.target) }
                    if (f != null && f.extension.lowercase() in imageExt) {
                        val bmp by produceState<ImageBitmap?>(null, f) {
                            value = withContext(Dispatchers.IO) { vm.platform.loadImage(f, 2000) }
                        }
                        bmp?.let { Image(it, contentDescription = f.name, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) }
                    } else {
                        Text(
                            inlineMd("[[${b.target}]]", vm),
                            fontSize = base,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
                is Block.Table -> Column(
                    Modifier
                        .padding(vertical = 6.dp)
                        .border(1.dp, cs.outlineVariant, RoundedCornerShape(6.dp)),
                ) {
                    b.rows.forEachIndexed { ri, r ->
                        Row(Modifier.background(if (ri == 0) cs.surfaceContainer else cs.background)) {
                            r.forEach { cell ->
                                Text(
                                    inlineMd(cell, vm),
                                    fontSize = (fontSize - 1).sp,
                                    fontWeight = if (ri == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.width(160.dp).padding(8.dp),
                                )
                            }
                        }
                        if (ri < b.rows.lastIndex) HorizontalDivider(color = cs.outlineVariant)
                    }
                }
            }
        }
    }
}
