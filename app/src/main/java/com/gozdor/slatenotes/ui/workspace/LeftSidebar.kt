package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.SearchHit
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.screens.EmptyHint
import com.gozdor.slatenotes.ui.screens.NameDialog
import com.gozdor.slatenotes.ui.screens.NoteRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ColumnScope.LeftSidebar(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    PanelTabs(
        icons = listOf(
            Icons.Outlined.Folder to "Files",
            Icons.Outlined.Search to "Search",
            Icons.Outlined.BookmarkBorder to "Bookmarks",
        ),
        selected = vm.leftPanel,
        onSelect = { vm.leftPanel = it },
    )
    HorizontalDivider(color = cs.outlineVariant)
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        when (vm.leftPanel) {
            0 -> FilesPanel(vm)
            1 -> SearchPanel(vm)
            else -> BookmarksPanel(vm)
        }
    }
    HorizontalDivider(color = cs.outlineVariant)
    // Vault switcher, help and settings at the bottom of the sidebar.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable { vm.show(Overlay.Vaults) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                vm.vault?.name.orEmpty(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Default.UnfoldMore, contentDescription = "Switch vault", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { vm.closeSidebars(); vm.openHelp() }) {
            Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = cs.onSurfaceVariant)
        }
        IconButton(onClick = { vm.show(Overlay.Settings) }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun PanelTabs(icons: List<Pair<ImageVector, String>>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEachIndexed { i, (icon, label) ->
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .background(if (i == selected) cs.surfaceContainerHighest else Color.Transparent, RoundedCornerShape(8.dp)),
            ) {
                IconButton(onClick = { onSelect(i) }) {
                    Icon(icon, contentDescription = label, tint = if (i == selected) cs.onSurface else cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

// ------------------------------------------------------------------ files

private data class TreeRow(val node: TreeNode, val depth: Int)

private fun flatten(nodes: List<TreeNode>, expanded: Map<String, Boolean>, depth: Int, out: MutableList<TreeRow>) {
    for (n in nodes) {
        out += TreeRow(n, depth)
        if (n.isDir && expanded[n.file.absolutePath] == true) flatten(n.children, expanded, depth + 1, out)
    }
}

/** Folder expansion survives closing the sidebar. */
private val expandedFolders = mutableStateMapOf<String, Boolean>()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesPanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val vault = vm.vault ?: return
    val snapshot = expandedFolders.toMap()
    val rows = remember(vm.tree, snapshot) { mutableListOf<TreeRow>().also { flatten(vm.tree, snapshot, 0, it) } }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var newFolderIn by remember { mutableStateOf<File?>(null) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var deleting by remember { mutableStateOf<File?>(null) }
    val active = vm.activePath

    Column {
        PanelHeader("Files") {
            if (vm.indexing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            IconButton(onClick = { vm.createNote(vault.root) }) {
                Icon(Icons.Outlined.Edit, contentDescription = "New note", tint = cs.onSurfaceVariant)
            }
            IconButton(onClick = { newFolderIn = vault.root }) {
                Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder", tint = cs.onSurfaceVariant)
            }
            IconButton(onClick = { expandedFolders.clear() }) {
                Icon(Icons.Default.UnfoldLess, contentDescription = "Collapse all", tint = cs.onSurfaceVariant)
            }
        }
        if (rows.isEmpty() && !vm.indexing) {
            EmptyHint("No notes yet. Tap the new note button above.")
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(rows, key = { it.node.file.absolutePath }) { row ->
                val f = row.node.file
                val path = f.absolutePath
                val isMd = !row.node.isDir && f.extension.equals("md", ignoreCase = true)
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .background(if (path == active) cs.surfaceContainerHighest else Color.Transparent, RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = {
                                    when {
                                        row.node.isDir -> expandedFolders[path] = expandedFolders[path] != true
                                        isMd -> vm.openNote(f)
                                        f.extension.equals("canvas", ignoreCase = true) -> vm.toast("Canvas files open in a later version")
                                        else -> vm.toast("Can't open .${f.extension} files yet")
                                    }
                                },
                                onLongClick = { menuFor = path },
                            )
                            .padding(start = (8 + row.depth * 16).dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (row.node.isDir) {
                            Icon(
                                if (expandedFolders[path] == true) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Outlined.Folder, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        } else {
                            Spacer(Modifier.width(22.dp))
                            Icon(
                                if (isMd) Icons.Outlined.Description else Icons.Outlined.InsertDriveFile,
                                null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isMd) f.nameWithoutExtension else f.name,
                            color = if (row.node.isDir || isMd) cs.onSurface else cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    DropdownMenu(expanded = menuFor == path, onDismissRequest = { menuFor = null }) {
                        if (row.node.isDir) {
                            DropdownMenuItem(text = { Text("New note") }, onClick = { menuFor = null; vm.createNote(f) })
                            DropdownMenuItem(text = { Text("New folder") }, onClick = { menuFor = null; newFolderIn = f })
                        } else if (isMd) {
                            DropdownMenuItem(text = { Text("Open in new tab") }, onClick = { menuFor = null; vm.openNote(f, newTab = true) })
                            DropdownMenuItem(
                                text = { Text(if (vm.isBookmarked(path)) "Remove bookmark" else "Bookmark") },
                                onClick = { menuFor = null; vm.toggleBookmark(path) },
                            )
                        }
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuFor = null; renaming = f })
                        DropdownMenuItem(text = { Text("Delete", color = cs.error) }, onClick = {
                            menuFor = null
                            if (vm.settings.confirmDelete) deleting = f else vm.delete(f)
                        })
                    }
                }
            }
        }
    }

    newFolderIn?.let { parent ->
        NameDialog("New folder", "Untitled", "Create", { newFolderIn = null }) {
            newFolderIn = null
            vm.createFolder(parent, it)
            expandedFolders[parent.absolutePath] = true
        }
    }
    renaming?.let { f ->
        NameDialog("Rename", if (f.isDirectory) f.name else f.nameWithoutExtension, "Rename", { renaming = null }) {
            renaming = null
            vm.renameFile(f, it)
        }
    }
    deleting?.let { f -> DeleteDialog(vm, f) { deleting = null } }
}

// ------------------------------------------------------------------ search

@Composable
private fun SearchPanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    var results by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    val index = vm.index
    val query = vm.searchQuery
    LaunchedEffect(query, index) {
        delay(120)
        results = withContext(Dispatchers.Default) { index.search(query) }
    }
    Column {
        TextField(
            value = query,
            onValueChange = { vm.searchQuery = it },
            placeholder = { Text("Search notes, or tag:#name") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.searchQuery = "" }) { Icon(Icons.Default.Close, "Clear") }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = cs.surfaceContainerHigh,
                unfocusedContainerColor = cs.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
        if (query.isNotBlank()) {
            Text(
                "${results.size} result${if (results.size == 1) "" else "s"}",
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(results, key = { it.note.file.absolutePath }) { hit ->
                NoteRow(hit.note.name, hit.snippet ?: hit.note.rel) { vm.openNote(hit.note.file) }
            }
        }
    }
}

// ------------------------------------------------------------------ bookmarks

@Composable
private fun BookmarksPanel(vm: AppViewModel) {
    val v = vm.vault ?: return
    val cs = MaterialTheme.colorScheme
    Column {
        PanelHeader("Bookmarks")
        if (vm.bookmarks.isEmpty()) {
            EmptyHint("No bookmarks yet. Use a note's ⋮ menu, or long-press it in Files, and choose Bookmark.")
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(vm.bookmarks, key = { it }) { rel ->
                val f = File(v.root, rel)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { if (f.isFile) vm.openNote(f) else vm.toast("That file no longer exists") }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Bookmark, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(f.nameWithoutExtension, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.toggleBookmark(f.absolutePath) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove bookmark", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
