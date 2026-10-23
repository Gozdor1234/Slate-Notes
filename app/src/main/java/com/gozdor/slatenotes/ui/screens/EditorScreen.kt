package com.gozdor.slatenotes.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.editor.Formatting
import com.gozdor.slatenotes.ui.editor.HighlightColors
import com.gozdor.slatenotes.ui.editor.MarkdownHighlighter
import com.gozdor.slatenotes.ui.editor.ReadingView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: AppViewModel, path: String) {
    val file = remember(path) { File(path) }
    val cs = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    var loaded by remember(path) { mutableStateOf(false) }
    var value by remember(path) { mutableStateOf(TextFieldValue("")) }
    var lastSaved by remember(path) { mutableStateOf("") }
    var reading by rememberSaveable(path) { mutableStateOf(false) }
    var title by remember(path) { mutableStateOf(TextFieldValue(file.nameWithoutExtension)) }
    var titleFocused by remember(path) { mutableStateOf(false) }
    var pendingName by remember(path) { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }

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
        val t = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrDefault("") }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val folder = file.parentFile?.let { vm.vault?.rel(it) }.orEmpty()
                    Text(
                        folder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        reading = !reading
                    }) {
                        if (reading) Icon(Icons.Default.Edit, contentDescription = "Edit")
                        else Icon(Icons.Default.Visibility, contentDescription = "Reading view")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuOpen = false
                                reading = false
                                title = title.copy(selection = TextRange(0, title.text.length))
                                titleFocus.requestFocus()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                // Inline title, like Obsidian: editing it renames the file.
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
                        .padding(top = 4.dp, bottom = 12.dp)
                        .focusRequester(titleFocus)
                        .onFocusChanged {
                            if (titleFocused && !it.isFocused) commitTitle()
                            titleFocused = it.isFocused
                        },
                )

                if (reading) {
                    ReadingView(
                        text = value.text,
                        resolveEmbed = { vm.resolveAttachment(it) },
                        onOpenLink = { vm.openLink(it) },
                        onOpenTag = { vm.openTag(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = { nv -> value = Formatting.continueList(value, nv) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = cs.onSurface,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        visualTransformation = highlighter,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(bodyFocus),
                    )
                }
                Spacer(Modifier.height(240.dp))
            }

            if (!reading) {
                HorizontalDivider(color = cs.outlineVariant)
                FormatBar { op -> value = op(value) }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete \"${file.nameWithoutExtension}\"?",
            message = "It will be moved to the vault's .trash folder.",
            confirm = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                lastSaved = value.text
                vm.delete(file) { vm.back() }
            },
        )
    }
}

@Composable
private fun FormatBar(apply: ((TextFieldValue) -> TextFieldValue) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
        ) {
            FmtButton(Icons.Default.Title, "Heading") { apply { Formatting.cycleHeading(it) } }
            FmtButton(Icons.Default.FormatBold, "Bold") { apply { Formatting.wrap(it, "**") } }
            FmtButton(Icons.Default.FormatItalic, "Italic") { apply { Formatting.wrap(it, "*") } }
            FmtButton(Icons.Default.StrikethroughS, "Strikethrough") { apply { Formatting.wrap(it, "~~") } }
            FmtButton(Icons.Default.Link, "Wikilink") { apply { Formatting.wrap(it, "[[", "]]") } }
            FmtButton(Icons.Default.CheckBox, "Checklist") {
                apply { Formatting.toggleLinePrefix(it, "- [ ] ", listOf("- [x] ", "- [X] ", "- ", "* ")) }
            }
            FmtButton(Icons.Default.FormatListBulleted, "Bulleted list") {
                apply { Formatting.toggleLinePrefix(it, "- ", listOf("- [ ] ", "- [x] ", "- [X] ", "* ")) }
            }
            FmtButton(Icons.Default.FormatQuote, "Quote") { apply { Formatting.toggleLinePrefix(it, "> ") } }
            FmtButton(Icons.Default.Code, "Code") { apply { Formatting.wrap(it, "`") } }
            FmtButton(Icons.Default.Tag, "Tag") { apply { Formatting.wrap(it, "#", "") } }
        }
    }
}

@Composable
private fun FmtButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
