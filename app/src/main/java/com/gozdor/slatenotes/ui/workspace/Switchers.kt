package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.Fuzzy
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun OverlayFrame(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) { content() }
    }
}

@Composable
private fun PromptField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    onBack: () -> Unit,
    onGo: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close") }
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = cs.surfaceContainerHigh,
                unfocusedContainerColor = cs.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus),
        )
    }
}

// ------------------------------------------------------------------ quick switcher

@Composable
fun QuickSwitcher(vm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    val cs = MaterialTheme.colorScheme
    val q = query.trim()
    val results = remember(q, vm.index, vm.recents) {
        if (q.isEmpty()) vm.recentNotes() else vm.index.switcher(q)
    }
    val exact = results.any { it.name.equals(q, ignoreCase = true) }

    OverlayFrame {
        PromptField(
            value = query,
            onChange = { query = it },
            placeholder = "Find or create a note…",
            onBack = { vm.closeOverlay() },
            onGo = {
                when {
                    results.isNotEmpty() && (exact || q.isEmpty()) -> vm.openNote(results.first().file)
                    q.isNotEmpty() -> vm.createNamed(q)
                    results.isNotEmpty() -> vm.openNote(results.first().file)
                }
            },
        )
        if (q.isEmpty()) {
            Text(
                if (results.isEmpty()) "Type to find a note, or a new name to create one." else "Recently opened",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(results, key = { it.file.absolutePath }) { n ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.openNote(n.file) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Description, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(n.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val folder = n.rel.substringBeforeLast('/', "")
                        if (folder.isNotEmpty()) {
                            Text(folder, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
            if (q.isNotEmpty() && !exact) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.createNamed(q) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.NoteAdd, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Create \"$q\"", color = cs.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ command palette

private class Command(val name: String, val needsNote: Boolean = false, val run: () -> Unit)

@Composable
fun CommandPalette(vm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val path = vm.activePath
    val tabId = vm.activeTabId

    fun then(action: () -> Unit): () -> Unit = {
        vm.closeOverlay()
        action()
    }

    val all = listOf(
        Command("Create new note", run = then { vm.createNote() }),
        Command("Canvas: Create new canvas", run = then { vm.createCanvas() }),
        Command("Open quick switcher", run = then { vm.show(Overlay.QuickSwitcher) }),
        Command("Search: Search in all files", run = then { vm.openLeft(1) }),
        Command("Files: Show file explorer", run = then { vm.openLeft(0) }),
        Command("Bookmarks: Show bookmarks", run = then { vm.openLeft(2) }),
        Command("Toggle reading view", needsNote = true, run = then { vm.setReading(tabId, !vm.isReading(tabId)) }),
        Command("Rename file", needsNote = true, run = then { vm.requestRename() }),
        Command("Move current file to another folder", needsNote = true, run = then { path?.let { vm.moveTarget = File(it) } }),
        Command(
            if (path != null && vm.isBookmarked(path)) "Bookmarks: Remove bookmark" else "Bookmarks: Bookmark current file",
            needsNote = true,
            run = then { path?.let { vm.toggleBookmark(it) } },
        ),
        Command("Backlinks: Show backlinks", run = then { vm.openRight(0) }),
        Command("Outgoing links: Show outgoing links", run = then { vm.openRight(1) }),
        Command("Tags: Show tags", run = then { vm.openRight(2) }),
        Command("Outline: Show outline", run = then { vm.openRight(3) }),
        Command("Insert current date", needsNote = true, run = then {
            vm.setReading(tabId, false)
            vm.insertRequest = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }),
        Command("Insert current time", needsNote = true, run = then {
            vm.setReading(tabId, false)
            vm.insertRequest = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }),
        Command("Navigate back", run = then { vm.goBack() }),
        Command("Navigate forward", run = then { vm.goForward() }),
        Command("New tab", run = then { vm.newTab() }),
        Command("Close current tab", run = then { vm.closeTab(vm.activeTab) }),
        Command("Close all tabs", run = then { vm.closeAllTabs() }),
        Command("Delete current file", needsNote = true, run = {
            if (vm.settings.confirmDelete) confirmDelete = true
            else then { path?.let { vm.delete(File(it)) } }.invoke()
        }),
        Command("Reload vault from disk", run = then { vm.refresh() }),
        Command("Open another vault", run = then { vm.show(Overlay.Vaults) }),
        Command("Open settings", run = then { vm.show(Overlay.Settings) }),
        Command("Open help", run = then { vm.openHelp() }),
    ).filter { !it.needsNote || path != null }

    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) all
    else all.mapNotNull { c -> Fuzzy.score(q, c.name.lowercase())?.let { c to it } }.sortedBy { it.second }.map { it.first }

    OverlayFrame {
        PromptField(
            value = query,
            onChange = { query = it },
            placeholder = "Type a command…",
            onBack = { vm.closeOverlay() },
            onGo = { shown.firstOrNull()?.run?.invoke() },
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(shown, key = { it.name }) { c ->
                Text(
                    c.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { c.run() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }

    if (confirmDelete && path != null) {
        DeleteDialog(vm, File(path)) {
            confirmDelete = false
            vm.closeOverlay()
        }
    }
}

// ------------------------------------------------------------------ tab switcher

@Composable
fun TabSwitcher(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    OverlayFrame {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.closeOverlay() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close") }
            Text("Tabs (${vm.tabs.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.closeAllTabs(); vm.closeOverlay() }) { Text("Close all") }
            IconButton(onClick = { vm.newTab() }) { Icon(Icons.Default.Add, contentDescription = "New tab") }
        }
        HorizontalDivider(color = cs.outlineVariant)
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            itemsIndexed(vm.tabs, key = { _, t -> t.id }) { i, t ->
                val active = i == vm.activeTab
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (active) cs.primaryContainer else cs.surfaceContainer,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { vm.switchTab(i) }
                        .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.path?.let { File(it).nameWithoutExtension } ?: "New tab",
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) cs.onPrimaryContainer else cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val note = t.path?.let { vm.index.notes[it] }
                        val preview = note?.content
                            ?.let { c -> c.substring(com.gozdor.slatenotes.data.Md.frontmatter(c)?.end ?: 0) }
                            ?.replace(Regex("\\s+"), " ")?.trim()?.take(90)
                        if (!preview.isNullOrEmpty()) {
                            Text(preview, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2)
                        }
                    }
                    IconButton(onClick = { vm.closeTab(i) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close tab", tint = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
