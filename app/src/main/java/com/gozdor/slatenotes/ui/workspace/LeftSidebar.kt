package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.ui.theme.Lucide

@Composable
fun ColumnScope.LeftSidebar(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = vm.leftPanel,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "leftPanel",
        ) { panel ->
            when (panel) {
                0 -> FilesPanel(vm)
                1 -> SearchPanel(vm)
                else -> BookmarksPanel(vm)
            }
        }
    }
    ViewSwitcherPill(
        options = listOf(Lucide.Folder to "Files", Lucide.Search to "Search", Lucide.Bookmark to "Bookmarks"),
        selected = vm.leftPanel,
        onSelect = { vm.leftPanel = it },
    )
    // Vault name and counts (tap to switch vault), with settings beside it.
    val (files, folders) = remember(vm.tree) { countTree(vm.tree) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { vm.show(Overlay.Vaults) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    vm.vault?.name.orEmpty(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Icon(Lucide.ChevronDown, contentDescription = "Switch vault", tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Text(
                "$files ${if (files == 1) "file" else "files"}, $folders ${if (folders == 1) "folder" else "folders"}",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
        }
        CircleIconButton(Lucide.Settings, "Settings") { vm.show(Overlay.Settings) }
    }
}

private fun countTree(nodes: List<TreeNode>): Pair<Int, Int> {
    var files = 0
    var folders = 0
    fun walk(list: List<TreeNode>) {
        for (n in list) {
            if (n.isDir) {
                folders++
                walk(n.children)
            } else {
                files++
            }
        }
    }
    walk(nodes)
    return files to folders
}

/** Rounded "Files ⇕" button at the bottom of a sidebar that switches between its views. */
@Composable
fun ViewSwitcherPill(options: List<Pair<ImageVector, String>>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val (icon, label) = options[selected.coerceIn(options.indices)]
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(50),
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = cs.onSurface, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Lucide.ChevronsUpDown, contentDescription = "Switch view", tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = cs.surface,
        ) {
            options.forEachIndexed { i, (optIcon, optLabel) ->
                DropdownMenuItem(
                    text = { Text(optLabel, fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = { Icon(optIcon, null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        open = false
                        onSelect(i)
                    },
                )
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

/** State of an in-progress drag in the file explorer. */
private class DragState {
    var item by mutableStateOf<File?>(null)
    var label by mutableStateOf("")

    /** Finger position in root coordinates. */
    var pointer by mutableStateOf(Offset.Zero)

    /** Folder the item would be dropped into; null = not a valid drop spot. */
    var dest by mutableStateOf<File?>(null)
    var travelled = 0f
}

@Composable
private fun FilesPanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val vault = vm.vault ?: return
    val expanded = vm.expandedFolders
    val snapshot = expanded.toMap()
    val rows = remember(vm.tree, snapshot) { mutableListOf<TreeRow>().also { flatten(vm.tree, snapshot, 0, it) } }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var newFolderIn by remember { mutableStateOf<File?>(null) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var deleting by remember { mutableStateOf<File?>(null) }
    val active = vm.activePath
    val haptic = LocalHapticFeedback.current

    val drag = remember { DragState() }
    val rowBounds = remember { mutableMapOf<String, Rect>() }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var listBounds by remember { mutableStateOf(Rect.Zero) }
    val listState = rememberLazyListState()
    val currentRows by rememberUpdatedState(rows)

    // Works out which folder the finger is over. Over a note = that note's folder. Empty space = vault folder.
    fun computeDest(): File? {
        val item = drag.item ?: return null
        val p = drag.pointer
        if (!panelBounds.contains(p)) return null
        val hit = currentRows.firstOrNull { r -> rowBounds[r.node.file.absolutePath]?.contains(p) == true }
        val dest = when {
            hit == null -> vault.root
            hit.node.isDir -> hit.node.file
            else -> hit.node.file.parentFile ?: vault.root
        }
        if (item.isDirectory && vault.isSelfOrInside(item, dest)) return null
        return dest
    }

    // While dragging: scroll when near the top or bottom edge, and open a folder you hover over.
    LaunchedEffect(drag.item) {
        if (drag.item == null) return@LaunchedEffect
        var hoverDir: String? = null
        var hoverSince = 0L
        while (drag.item != null) {
            val y = drag.pointer.y
            val edge = 140f
            when {
                y < listBounds.top + edge -> listState.scrollBy(-18f)
                y > listBounds.bottom - edge -> listState.scrollBy(18f)
            }
            drag.dest = computeDest()
            val d = drag.dest
            val overDir = d?.takeIf { it != vault.root && it.absolutePath != drag.item?.parentFile?.absolutePath }?.absolutePath
            if (overDir != hoverDir) {
                hoverDir = overDir
                hoverSince = System.currentTimeMillis()
            } else if (overDir != null && expanded[overDir] != true && System.currentTimeMillis() - hoverSince > 700) {
                expanded[overDir] = true
            }
            delay(16)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { panelBounds = it.boundsInRoot() },
    ) {
        Column {
            if (drag.item != null) {
                val d = drag.dest
                Text(
                    when {
                        d == null -> "Can't drop here"
                        d.absolutePath == drag.item?.parentFile?.absolutePath -> "Already in ${if (d == vault.root) "the vault folder" else d.name}"
                        d == vault.root -> "Release to move to the vault folder"
                        else -> "Release to move into ${d.name}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(cs.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            } else if (rows.isEmpty() && !vm.indexing) {
                EmptyHint("No notes yet. Tap the new note button above.")
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { listBounds = it.boundsInRoot() },
            ) {
                items(rows, key = { it.node.file.absolutePath }) { row ->
                    val f = row.node.file
                    val path = f.absolutePath
                    val isMd = !row.node.isDir && f.extension.equals("md", ignoreCase = true)
                    val isCanvas = !row.node.isDir && f.extension.equals("canvas", ignoreCase = true)
                    val isDropTarget = drag.item != null && row.node.isDir && drag.dest?.absolutePath == path
                    val isDragged = drag.item?.absolutePath == path
                    val chevronAngle by animateFloatAsState(if (expanded[path] == true) 90f else 0f, label = "chevron")
                    val bg = when {
                        isDropTarget -> cs.primary.copy(alpha = 0.22f)
                        path == active -> cs.surfaceContainerHighest
                        else -> Color.Transparent
                    }
                    Box(Modifier.animateItem()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .onGloballyPositioned { rowBounds[path] = it.boundsInRoot() }
                                .background(bg, RoundedCornerShape(6.dp))
                                // Long-press: release without moving opens the menu; move to drag.
                                .pointerInput(path) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { local ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            drag.item = f
                                            drag.label = if (f.isDirectory) f.name else f.nameWithoutExtension
                                            drag.travelled = 0f
                                            drag.pointer = (rowBounds[path]?.topLeft ?: Offset.Zero) + local
                                            drag.dest = null
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            drag.travelled += amount.getDistance()
                                            drag.pointer += amount
                                        },
                                        onDragEnd = {
                                            val item = drag.item
                                            val dest = computeDest()
                                            val moved = drag.travelled > 24f
                                            drag.item = null
                                            drag.dest = null
                                            if (!moved) {
                                                menuFor = path
                                            } else if (item != null && dest != null) {
                                                vm.moveFile(item, dest)
                                            }
                                        },
                                        onDragCancel = {
                                            drag.item = null
                                            drag.dest = null
                                        },
                                    )
                                }
                                .clickable {
                                    when {
                                        row.node.isDir -> expanded[path] = expanded[path] != true
                                        isMd -> vm.openNote(f)
                                        isCanvas -> vm.openNote(f)
                                        else -> vm.toast("Can't open .${f.extension} files yet")
                                    }
                                }
                                .padding(start = (8 + row.depth * 16).dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                                .alpha(if (isDragged) 0.4f else 1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (row.node.isDir) {
                                Icon(
                                    Lucide.ChevronRight,
                                    null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp).rotate(chevronAngle),
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Lucide.Folder, null, tint = if (isDropTarget) cs.primary else cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            } else {
                                Spacer(Modifier.width(22.dp))
                                Icon(
                                    when {
                                        isMd -> Lucide.FileText
                                        isCanvas -> Lucide.LayoutDashboard
                                        else -> Lucide.File
                                    },
                                    null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isMd || isCanvas) f.nameWithoutExtension else f.name,
                                color = if (row.node.isDir || isMd || isCanvas) cs.onSurface else cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        DropdownMenu(expanded = menuFor == path, onDismissRequest = { menuFor = null }) {
                            if (row.node.isDir) {
                                DropdownMenuItem(text = { Text("New note") }, onClick = { menuFor = null; vm.createNote(f) })
                                DropdownMenuItem(text = { Text("New canvas") }, onClick = { menuFor = null; vm.createCanvas(f) })
                                DropdownMenuItem(text = { Text("New folder") }, onClick = { menuFor = null; newFolderIn = f })
                            } else if (isMd || isCanvas) {
                                DropdownMenuItem(text = { Text("Open in new tab") }, onClick = { menuFor = null; vm.openNote(f, newTab = true) })
                                DropdownMenuItem(
                                    text = { Text(if (vm.isBookmarked(path)) "Remove bookmark" else "Bookmark") },
                                    onClick = { menuFor = null; vm.toggleBookmark(path) },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (row.node.isDir) "Move folder to…" else "Move file to…") },
                                onClick = { menuFor = null; vm.moveTarget = f },
                            )
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { menuFor = null; renaming = f })
                            DropdownMenuItem(text = { Text("Delete", color = cs.error) }, onClick = {
                                menuFor = null
                                if (vm.settings.confirmDelete) deleting = f else vm.delete(f)
                            })
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.createNote(vault.root) }) {
                    Icon(Lucide.SquarePen, contentDescription = "New note", tint = cs.onSurfaceVariant)
                }
                IconButton(onClick = { newFolderIn = vault.root }) {
                    Icon(Lucide.FolderPlus, contentDescription = "New folder", tint = cs.onSurfaceVariant)
                }
                IconButton(onClick = { expanded.clear() }) {
                    Icon(Lucide.ChevronsDownUp, contentDescription = "Collapse all", tint = cs.onSurfaceVariant)
                }
                if (vm.indexing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        // The item following your finger while dragging.
        if (drag.item != null) {
            val p = drag.pointer - panelBounds.topLeft
            Row(
                Modifier
                    .offset { IntOffset((p.x + 24).roundToInt(), (p.y - 56).roundToInt()) }
                    .shadow(6.dp, RoundedCornerShape(8.dp))
                    .background(cs.surfaceContainerHighest, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (drag.item?.isDirectory == true) Lucide.Folder else Lucide.FileText,
                    null, tint = cs.primary, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(drag.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    newFolderIn?.let { parent ->
        NameDialog("New folder", "Untitled", "Create", { newFolderIn = null }) {
            newFolderIn = null
            vm.createFolder(parent, it)
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
            leadingIcon = { Icon(Lucide.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { vm.searchQuery = "" }) { Icon(Lucide.X, "Clear") }
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
                    Icon(Lucide.Bookmark, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(f.nameWithoutExtension, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.toggleBookmark(f.absolutePath) }) {
                        Icon(Lucide.X, contentDescription = "Remove bookmark", tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
