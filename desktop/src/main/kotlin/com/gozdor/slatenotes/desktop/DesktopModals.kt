package com.gozdor.slatenotes.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.data.Fuzzy
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.screens.NameDialog
import com.gozdor.slatenotes.ui.settings.HelpTopics
import com.gozdor.slatenotes.ui.theme.Lucide
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dimmed backdrop with a centered card, like Obsidian's prompts. Clicking outside or Esc closes it. */
@Composable
fun Modal(vm: AppViewModel, wide: Boolean = false, onClose: () -> Unit = { vm.closeOverlay() }, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = if (wide) Alignment.Center else Alignment.TopCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp,
            modifier = Modifier
                .padding(top = if (wide) 0.dp else 80.dp)
                .then(if (wide) Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.82f).widthIn(max = 1000.dp) else Modifier.width(620.dp))
                // Swallow clicks so they don't reach the backdrop.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) { content() }
    }
}

/** A prompt with a search box and a keyboard-navigable list (↑ ↓ to move, Enter to choose). */
@Composable
private fun <T> PromptList(
    placeholder: String,
    query: String,
    onQuery: (String) -> Unit,
    items: List<T>,
    onChoose: (T) -> Unit,
    footer: String,
    row: @Composable (T, Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(items) { selected = 0 }
    LaunchedEffect(selected) { if (items.isNotEmpty()) listState.animateScrollToItem(selected.coerceIn(items.indices)) }

    Column {
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(cs.primary),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text(placeholder, color = cs.onSurfaceVariant, fontSize = 16.sp)
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focus)
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) {
                        Key.DirectionDown -> { if (items.isNotEmpty()) selected = (selected + 1) % items.size; true }
                        Key.DirectionUp -> { if (items.isNotEmpty()) selected = (selected - 1 + items.size) % items.size; true }
                        Key.Enter, Key.NumPadEnter -> { items.getOrNull(selected)?.let(onChoose); true }
                        else -> false
                    }
                },
        )
        HorizontalDivider(color = cs.outlineVariant)
        LazyColumn(state = listState, modifier = Modifier.heightIn(max = 420.dp).padding(6.dp)) {
            itemsIndexed(items) { i, item ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (i == selected) cs.surfaceContainerHigh else Color.Transparent)
                        .clickable { onChoose(item) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) { row(item, i == selected) }
            }
        }
        Text(
            footer,
            fontSize = 12.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// ------------------------------------------------------------------ quick switcher

private sealed interface SwitchItem {
    data class Note(val file: File, val name: String, val folder: String) : SwitchItem
    data class Create(val name: String) : SwitchItem
}

@Composable
fun QuickSwitcherPanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val items = remember(q, vm.index, vm.recents) {
        val notes = (if (q.isEmpty()) vm.recentNotes() else vm.index.switcher(q))
            .map { SwitchItem.Note(it.file, it.name, it.rel.substringBeforeLast('/', "")) }
        val exact = notes.any { it.name.equals(q, ignoreCase = true) }
        if (q.isNotEmpty() && !exact) notes + SwitchItem.Create(q) else notes
    }
    PromptList(
        placeholder = "Find or create a note…",
        query = query,
        onQuery = { query = it },
        items = items,
        onChoose = {
            when (it) {
                is SwitchItem.Note -> vm.openNote(it.file)
                is SwitchItem.Create -> {
                    vm.closeOverlay()
                    vm.createNamed(it.name)
                }
            }
        },
        footer = "↑↓ to navigate    ↵ to open    esc to dismiss",
    ) { item, _ ->
        when (item) {
            is SwitchItem.Note -> Column {
                Text(item.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.folder.isNotEmpty()) Text(item.folder, fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1)
            }
            is SwitchItem.Create -> Text("Create \"${item.name}\"", fontSize = 14.sp, color = cs.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ------------------------------------------------------------------ command palette

private class Cmd(val name: String, val hotkey: String = "", val needsFile: Boolean = false, val run: () -> Unit)

@Composable
fun CommandPalettePanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val path = vm.activePath
    val isNote = path?.endsWith(".md", ignoreCase = true) == true
    fun then(action: () -> Unit): () -> Unit = {
        vm.closeOverlay()
        action()
    }
    val all = listOf(
        Cmd("Create new note", "Ctrl + N", run = then { vm.createNote() }),
        Cmd("Canvas: Create new canvas", run = then { vm.createCanvas() }),
        Cmd("Open quick switcher", "Ctrl + O", run = then { vm.show(Overlay.QuickSwitcher) }),
        Cmd("Search: Search in all files", "Ctrl + Shift + F", run = then { vm.openLeft(1) }),
        Cmd("Toggle reading view", "Ctrl + E", needsFile = true, run = then { toggleReading(vm) }),
        Cmd("Rename file", needsFile = true, run = then { if (isNote) vm.requestRename() }),
        Cmd("Move current file to another folder", needsFile = true, run = then { path?.let { vm.moveTarget = File(it) } }),
        Cmd(
            if (path != null && vm.isBookmarked(path)) "Bookmarks: Remove bookmark" else "Bookmarks: Bookmark current file",
            needsFile = true,
            run = then { path?.let { vm.toggleBookmark(it) } },
        ),
        Cmd("Delete current file", needsFile = true, run = then { path?.let { vm.delete(File(it)) } }),
        Cmd("Insert current date", needsFile = true, run = then { vm.insertRequest = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }),
        Cmd("Insert current time", needsFile = true, run = then { vm.insertRequest = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }),
        Cmd("Navigate back", "Ctrl + Alt + ←", run = then { vm.goBack() }),
        Cmd("Navigate forward", "Ctrl + Alt + →", run = then { vm.goForward() }),
        Cmd("New tab", "Ctrl + T", run = then { vm.newTab() }),
        Cmd("Close current tab", "Ctrl + W", run = then { vm.closeTab(vm.activeTab) }),
        Cmd("Undo close tab", "Ctrl + Shift + T", run = then { vm.reopenClosedTab() }),
        Cmd("Close all tabs", run = then { vm.closeAllTabs() }),
        Cmd("Toggle left sidebar", run = then { vm.leftOpen = !vm.leftOpen }),
        Cmd("Toggle right sidebar", run = then { vm.rightOpen = !vm.rightOpen }),
        Cmd("Files: Show file explorer", run = then { vm.openLeft(0) }),
        Cmd("Bookmarks: Show bookmarks", run = then { vm.openLeft(2) }),
        Cmd("Backlinks: Show backlinks", run = then { vm.openRight(0) }),
        Cmd("Outgoing links: Show outgoing links", run = then { vm.openRight(1) }),
        Cmd("Tags: Show tags", run = then { vm.openRight(2) }),
        Cmd("Outline: Show outline", run = then { vm.openRight(3) }),
        Cmd("Reload vault from disk", run = then { vm.refresh() }),
        Cmd("Open another vault", run = then { vm.show(Overlay.Vaults) }),
        Cmd("Open settings", "Ctrl + ,", run = then { vm.show(Overlay.Settings) }),
        Cmd("Open help", run = then { vm.openHelp() }),
    ).filter { !it.needsFile || path != null }

    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) all
    else all.mapNotNull { c -> Fuzzy.score(q, c.name.lowercase())?.let { c to it } }.sortedBy { it.second }.map { it.first }

    PromptList(
        placeholder = "Select a command…",
        query = query,
        onQuery = { query = it },
        items = shown,
        onChoose = { it.run() },
        footer = "↑↓ to navigate    ↵ to use    esc to dismiss",
    ) { c, _ ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (c.hotkey.isNotEmpty()) {
                Text(
                    c.hotkey,
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(cs.surfaceContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ vault chooser

@Composable
fun VaultChooser(vm: AppViewModel, platform: DesktopPlatform, modal: Boolean) {
    val cs = MaterialTheme.colorScheme
    var naming by remember { mutableStateOf(false) }
    val obsidian = remember { platform.obsidianVaults() }
    val saved = vm.vaultPaths.map { File(it) }
    val extra = obsidian.filter { o -> saved.none { it.absolutePath == o.absolutePath } }

    Row(Modifier.fillMaxSize().background(cs.background)) {
        // Known vaults
        Column(
            Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(cs.surfaceContainer)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            if (saved.isEmpty() && extra.isEmpty()) {
                Text("No vaults yet", color = cs.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
            }
            saved.forEach { f -> VaultRow(f, current = vm.vault?.root?.absolutePath == f.absolutePath, onRemove = { vm.forgetVault(f.absolutePath) }) { open(vm, f) } }
            if (extra.isNotEmpty()) {
                Text("Obsidian vaults on this PC", fontSize = 12.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp))
                extra.forEach { f -> VaultRow(f, current = false, onRemove = null) { open(vm, f) } }
            }
        }
        VerticalDivider(color = cs.outlineVariant)
        // Actions
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Slate Notes", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Version ${platform.appVersion}", fontSize = 13.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(36.dp))
            Column(Modifier.widthIn(max = 520.dp)) {
                ActionRow("Create new vault", "Create a new vault in a folder you choose.", "Create", primary = true) { naming = true }
                HorizontalDivider(color = cs.outlineVariant)
                ActionRow("Open folder as vault", "Choose an existing folder of Markdown files, such as an Obsidian vault.", "Open") {
                    platform.chooseFolder("Open folder as vault")?.let { open(vm, it) }
                }
            }
            if (modal) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = { vm.closeOverlay() }) { Text("Cancel") }
            }
        }
    }

    if (naming) {
        NameDialog("Vault name", "My Vault", "Choose location…", { naming = false }) { name ->
            naming = false
            val parent = platform.chooseFolder("Where should \"$name\" be created?") ?: return@NameDialog
            val dir = File(parent, name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim())
            if (dir.exists() && !dir.isDirectory) {
                vm.toast("A file named \"${dir.name}\" is already there")
            } else if (dir.isDirectory || dir.mkdirs()) {
                open(vm, dir)
            } else {
                vm.toast("Couldn't create the vault folder")
            }
        }
    }
}

private fun open(vm: AppViewModel, dir: File) {
    if (!dir.isDirectory) {
        vm.toast("That folder no longer exists")
        return
    }
    vm.openVault(dir)
    vm.leftOpen = true
    vm.rightOpen = true
}

@Composable
private fun VaultRow(f: File, current: Boolean, onRemove: (() -> Unit)?, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (current) cs.surfaceContainerHighest else if (hovered) cs.surfaceContainerHigh else Color.Transparent)
            .hoverable(interaction)
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(f.absolutePath, fontSize = 11.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onRemove != null && hovered) HoverIcon(Lucide.X, "Remove from list", size = 24.dp, onClick = onRemove)
    }
}

@Composable
private fun ActionRow(title: String, desc: String, button: String, primary: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        if (primary) Button(onClick = onClick) { Text(button) } else OutlinedButton(onClick = onClick) { Text(button) }
    }
}

// ------------------------------------------------------------------ help

/** Topics for the Windows app, shown before the shared ones. */
val WindowsHelp: List<Pair<String, List<String>>> = listOf(
    "Slate on Windows" to listOf(
        "Left edge: the ribbon. Quick switcher, new canvas, command palette and new note. The top button shows or hides the left sidebar.",
        "Left sidebar: Files, Search and Bookmarks (icons at the top), with your vault, help and settings at the bottom.",
        "Middle: your tabs. Each tab has back and forward arrows, the note's folder and name, a reading/editing toggle and a ⋮ menu.",
        "Right sidebar: Backlinks, Outgoing links, Tags and Outline. The button at the far right of the tab bar shows or hides it.",
        "Drag the thin line beside either sidebar to resize it.",
        "Bottom right: backlink count plus word and character count for the open note.",
        "Some help topics below describe the phone app's buttons and gestures. Everything they describe is also available here through the sidebars, menus and hotkeys.",
    ),
    "Keyboard shortcuts" to listOf(
        "Ctrl + O  Open quick switcher",
        "Ctrl + P  Open command palette",
        "Ctrl + N  Create new note",
        "Ctrl + E  Toggle reading / editing view",
        "Ctrl + T  New tab    Ctrl + W  Close tab    Ctrl + Shift + T  Reopen closed tab",
        "Ctrl + Tab / Ctrl + Shift + Tab  Next / previous tab",
        "Ctrl + Alt + ← / →  Navigate back / forward",
        "Ctrl + Shift + F  Search all files",
        "Ctrl + ,  Open settings",
        "While editing: Ctrl + B bold, Ctrl + I italic, Ctrl + K link, Tab / Shift + Tab indent and unindent",
        "Esc closes the open pop-up",
    ),
    "Files and the mouse" to listOf(
        "Click a note to open it; click a folder to expand or collapse it.",
        "Right-click a note or folder for New note, New canvas, New folder, Open in new tab, Bookmark, Move to, Rename and Delete.",
        "Drag a note or folder onto a folder to move it there, or onto empty space to move it to the top of the vault.",
        "On a canvas: drag empty space to pan, scroll to pan, Ctrl + scroll to zoom.",
    ),
)

@Composable
fun HelpModal(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val windowsCount = WindowsHelp.size
    var selected by remember { mutableIntStateOf((vm.helpTopic ?: -1).let { if (it >= 0) it + windowsCount else 0 }) }
    Modal(vm, wide = true, onClose = { vm.helpTopic = null }) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(cs.surfaceContainer)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text("Help & tips", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(8.dp))
                val titles = WindowsHelp.map { it.first } + HelpTopics.map { it.title }
                titles.forEachIndexed { i, t -> NavRow(t, i == selected) { selected = i } }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val title = if (selected < windowsCount) WindowsHelp[selected].first else HelpTopics[selected - windowsCount].title
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    HoverIcon(Lucide.X, "Close") { vm.helpTopic = null }
                }
                Spacer(Modifier.height(12.dp))
                if (selected < windowsCount) {
                    WindowsHelp[selected].second.forEach { Bullet(it) }
                } else {
                    HelpTopics[selected - windowsCount].blocks.forEach { b ->
                        b.heading?.let {
                            Text(it, fontWeight = FontWeight.Bold, color = cs.primary, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        }
                        b.text?.let { Text(it, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                        b.bullets.forEach { Bullet(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(18.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun NavRow(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) cs.surfaceContainerHighest else if (hovered) cs.surfaceContainerHigh else Color.Transparent)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
