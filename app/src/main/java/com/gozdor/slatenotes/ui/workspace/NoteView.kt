package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.editor.Formatting
import com.gozdor.slatenotes.ui.editor.HighlightColors
import com.gozdor.slatenotes.ui.editor.MarkdownHighlighter
import com.gozdor.slatenotes.ui.editor.ReadingView
import com.gozdor.slatenotes.ui.theme.Lucide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteView(vm: AppViewModel, tabId: Long, path: String) {
    val file = remember(path) { File(path) }
    val cs = MaterialTheme.colorScheme
    val settings = vm.settings
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
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
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()
    val pull = rememberPullDownToCommand { vm.show(Overlay.Commands) }

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
        val r = withContext(Dispatchers.IO) { runCatching { file.readText() } }
        val t = r.getOrNull()
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
        }
    }

    // Autosave shortly after typing stops.
    LaunchedEffect(value.text) {
        if (!loaded || value.text == lastSaved) return@LaunchedEffect
        delay(600)
        vm.saveNote(path, value.text)
        lastSaved = value.text
    }

    DisposableEffect(path) {
        onDispose {
            if (loaded && value.text != lastSaved) vm.saveNote(path, value.text)
        }
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

    // "Rename" from the menu or command palette focuses the inline title.
    val startRename = remember { vm.renameRequest }
    LaunchedEffect(vm.renameRequest) {
        if (vm.renameRequest == startRename) return@LaunchedEffect
        delay(50)
        title = title.copy(selection = TextRange(0, title.text.length))
        titleFocus.requestFocus()
    }

    // Outline taps jump to a heading.
    LaunchedEffect(vm.jump, loaded) {
        val j = vm.jump ?: return@LaunchedEffect
        if (j.path != path || !loaded) return@LaunchedEffect
        vm.jump = null
        vm.setReading(tabId, false)
        val off = j.offset.coerceIn(0, value.text.length)
        value = value.copy(selection = TextRange(off))
        delay(120)
        layout?.let { l ->
            val top = l.getLineTop(l.getLineForOffset(off))
            scroll.animateScrollTo((bodyTop + top).toInt().coerceAtLeast(0))
        }
    }

    // Text inserted by commands (e.g. current date).
    LaunchedEffect(vm.insertRequest) {
        val ins = vm.insertRequest ?: return@LaunchedEffect
        vm.insertRequest = null
        if (reading || !loaded) return@LaunchedEffect
        val s = value.selection.min
        val e = value.selection.max
        value = TextFieldValue(value.text.replaceRange(s, e, ins), TextRange(s + ins.length))
    }

    if (missing) {
        EmptyState(
            title = "This file can't be opened",
            actions = listOf("Close tab" to { vm.closeTab(vm.activeTab) }),
            modifier = Modifier.nestedScroll(pull),
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pull),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp),
            ) {
                if (settings.showInlineTitle) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it.copy(text = it.text.replace("\n", "")) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = cs.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            commitTitle()
                            if (!reading) bodyFocus.requestFocus() else focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp)
                            .focusRequester(titleFocus)
                            .onFocusChanged {
                                if (titleFocused && !it.isFocused) commitTitle()
                                titleFocused = it.isFocused
                            },
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                if (reading) {
                    ReadingView(
                        text = value.text,
                        fontSize = settings.fontSize,
                        resolveEmbed = { vm.resolveAttachment(it) },
                        onOpenLink = { vm.openLink(it) },
                        onOpenTag = { vm.openTag(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = { nv ->
                            value = if (settings.continueLists) Formatting.continueList(value, nv) else nv
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = cs.onSurface,
                            fontSize = settings.fontSize.sp,
                            lineHeight = (settings.fontSize * 1.5f).sp,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        visualTransformation = highlighter,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        onTextLayout = { layout = it },
                        decorationBox = { inner ->
                            Box {
                                if (value.text.isEmpty()) {
                                    Text(
                                        "Start writing…",
                                        color = cs.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = settings.fontSize.sp,
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(bodyFocus)
                            .onGloballyPositioned { bodyTop = it.positionInParent().y },
                    )
                }
                Spacer(Modifier.height(240.dp))
            }
        }

        if (!reading && imeVisible) {
            HorizontalDivider(color = cs.outlineVariant)
            MobileToolbar(
                apply = { op -> value = op(value) },
                onCommand = { vm.show(Overlay.Commands) },
                onHideKeyboard = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            )
        }
    }
}

/** The toolbar above the keyboard while editing, like Obsidian's mobile toolbar. Swipe for more. */
@Composable
private fun MobileToolbar(
    apply: ((TextFieldValue) -> TextFieldValue) -> Unit,
    onCommand: () -> Unit,
    onHideKeyboard: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
        ) {
            ToolButton(Lucide.Link, "Add internal link") { apply { Formatting.wrap(it, "[[", "]]") } }
            ToolButton(Lucide.Hash, "Add tag") { apply { Formatting.wrap(it, "#", "") } }
            ToolButton(Lucide.SquareCheck, "Toggle checklist") {
                apply { Formatting.toggleLinePrefix(it, "- [ ] ", listOf("- [x] ", "- [X] ", "- ", "* ")) }
            }
            ToolButton(Lucide.List, "Toggle bulleted list") {
                apply { Formatting.toggleLinePrefix(it, "- ", listOf("- [ ] ", "- [x] ", "- [X] ", "* ")) }
            }
            ToolButton(Lucide.ListOrdered, "Toggle numbered list") {
                apply { Formatting.toggleLinePrefix(it, "1. ", listOf("- [ ] ", "- [x] ", "- ", "* ")) }
            }
            ToolButton(Lucide.ListIndentIncrease, "Indent") { apply { Formatting.indent(it, true) } }
            ToolButton(Lucide.ListIndentDecrease, "Unindent") { apply { Formatting.indent(it, false) } }
            ToolButton(Lucide.Heading, "Cycle heading") { apply { Formatting.cycleHeading(it) } }
            ToolButton(Lucide.Bold, "Bold") { apply { Formatting.wrap(it, "**") } }
            ToolButton(Lucide.Italic, "Italic") { apply { Formatting.wrap(it, "*") } }
            ToolButton(Lucide.Strikethrough, "Strikethrough") { apply { Formatting.wrap(it, "~~") } }
            ToolButton(Lucide.TextQuote, "Toggle blockquote") { apply { Formatting.toggleLinePrefix(it, "> ") } }
            ToolButton(Lucide.Code, "Code") { apply { Formatting.wrap(it, "`") } }
            ToolButton(Lucide.Terminal, "Command palette", onCommand)
            ToolButton(Lucide.KeyboardOff, "Hide keyboard", onHideKeyboard)
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}
