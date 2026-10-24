package com.gozdor.slatenotes.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.screens.ConfirmDialog
import com.gozdor.slatenotes.ui.screens.NameDialog
import com.gozdor.slatenotes.ui.theme.Lucide
import java.io.File

private data class TreeRow(val node: TreeNode, val depth: Int)

private fun flatten(nodes: List<TreeNode>, expanded: Map<String, Boolean>, depth: Int, out: MutableList<TreeRow>) {
    for (n in nodes) {
        out += TreeRow(n, depth)
        if (n.isDir && expanded[n.file.absolutePath] == true) flatten(n.children, expanded, depth + 1, out)
    }
}

@Composable
fun FileExplorer(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val vault = vm.vault ?: return
    val expanded = vm.expandedFolders
    val snapshot = expanded.toMap()
    val rows = remember(vm.tree, snapshot) { mutableListOf<TreeRow>().also { flatten(vm.tree, snapshot, 0, it) } }
    val currentRows by rememberUpdatedState(rows)
    val bounds = remember { mutableMapOf<String, Rect>() }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var newFolderIn by remember { mutableStateOf<File?>(null) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var deleting by remember { mutableStateOf<File?>(null) }
    var dragging by remember { mutableStateOf<File?>(null) }
    var dropDir by remember { mutableStateOf<File?>(null) }

    // Folder under the pointer: a folder row = that folder, a file row = its folder, empty space = vault root.
    fun destAt(p: Offset): File? {
        val item = dragging ?: return null
        if (!panelBounds.contains(p)) return null
        val hit = currentRows.firstOrNull { bounds[it.node.file.absolutePath]?.contains(p) == true }
        val dest = when {
            hit == null -> vault.root
            hit.node.isDir -> hit.node.file
            else -> hit.node.file.parentFile ?: vault.root
        }
        return if (item.isDirectory && vault.isSelfOrInside(item, dest)) null else dest
    }

    Column(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { panelBounds = it.boundsInRoot() },
    ) {
        HeaderRow {
            Spacer(Modifier.weight(1f))
            HoverIcon(Lucide.SquarePen, "New note") { vm.createNote(vault.root) }
            HoverIcon(Lucide.FolderPlus, "New folder") { newFolderIn = vault.root }
            HoverIcon(Lucide.ChevronsDownUp, "Collapse all") { expanded.clear() }
            Spacer(Modifier.weight(1f))
        }
        LazyColumn(
            Modifier
                .weight(1f)
                .background(if (dragging != null && dropDir == vault.root) cs.primary.copy(alpha = 0.08f) else Color.Transparent),
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 24.dp),
        ) {
            items(rows, key = { it.node.file.absolutePath }) { row ->
                val f = row.node.file
                val path = f.absolutePath
                val isMd = !row.node.isDir && f.extension.equals("md", true)
                val isCanvas = !row.node.isDir && f.extension.equals("canvas", true)
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val angle by animateFloatAsState(if (expanded[path] == true) 90f else 0f, label = "chevron")
                val isDrop = dragging != null && row.node.isDir && dropDir?.absolutePath == path
                Box(Modifier.animateItem()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { bounds[path] = it.boundsInRoot() }
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    isDrop -> cs.primary.copy(alpha = 0.2f)
                                    path == vm.activePath -> cs.surfaceContainerHighest
                                    hovered -> cs.surfaceContainerHigh
                                    else -> Color.Transparent
                                },
                            )
                            .hoverable(interaction)
                            .pointerInput(path) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (currentEvent.buttons.isSecondaryPressed) {
                                        menuFor = path
                                        return@awaitEachGesture
                                    }
                                    val origin = bounds[path]?.topLeft ?: Offset.Zero
                                    var moved = false
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!ch.pressed) break
                                        if (!moved && (ch.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                            moved = true
                                            dragging = f
                                        }
                                        if (moved) {
                                            ch.consume()
                                            dropDir = destAt(origin + ch.position)
                                        }
                                    }
                                    if (moved) {
                                        val dest = dropDir
                                        dragging = null
                                        dropDir = null
                                        if (dest != null) vm.moveFile(f, dest)
                                    } else {
                                        when {
                                            row.node.isDir -> expanded[path] = expanded[path] != true
                                            isMd || isCanvas -> vm.openNote(f)
                                            else -> vm.toast("Can't open .${f.extension} files yet")
                                        }
                                    }
                                }
                            }
                            .padding(start = (6 + row.depth * 16).dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (row.node.isDir) {
                            Icon(Lucide.ChevronRight, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp).rotate(angle))
                            Spacer(Modifier.width(4.dp))
                        } else {
                            Spacer(Modifier.width(18.dp))
                        }
                        if (isCanvas) {
                            Icon(Lucide.LayoutDashboard, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (isMd || isCanvas) f.nameWithoutExtension else f.name,
                            fontSize = 13.sp,
                            color = if (row.node.isDir || isMd || isCanvas) cs.onSurface else cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    DropdownMenu(
                        expanded = menuFor == path,
                        onDismissRequest = { menuFor = null },
                        shape = RoundedCornerShape(10.dp),
                        containerColor = cs.surface,
                    ) {
                        val close = { menuFor = null }
                        if (row.node.isDir) {
                            MenuItem("New note") { close(); vm.createNote(f) }
                            MenuItem("New canvas") { close(); vm.createCanvas(f) }
                            MenuItem("New folder") { close(); newFolderIn = f }
                            HorizontalDivider(color = cs.outlineVariant)
                        } else if (isMd || isCanvas) {
                            MenuItem("Open in new tab") { close(); vm.openNote(f, newTab = true) }
                            MenuItem(if (vm.isBookmarked(path)) "Remove bookmark" else "Bookmark") { close(); vm.toggleBookmark(path) }
                            HorizontalDivider(color = cs.outlineVariant)
                        }
                        MenuItem(if (row.node.isDir) "Move folder to…" else "Move file to…") { close(); vm.moveTarget = f }
                        MenuItem("Rename…") { close(); renaming = f }
                        DropdownMenuItem(
                            text = { Text("Delete", fontSize = 13.sp, color = cs.error) },
                            onClick = {
                                menuFor = null
                                if (vm.settings.confirmDelete) deleting = f else vm.delete(f)
                            },
                        )
                    }
                }
            }
        }
        dragging?.let { d ->
            val dest = dropDir
            Text(
                when {
                    dest == null -> "Can't drop here"
                    dest.absolutePath == d.parentFile?.absolutePath -> "Already here"
                    dest == vault.root -> "Move \"${d.name}\" to the vault folder"
                    else -> "Move \"${d.name}\" into ${dest.name}"
                },
                fontSize = 12.sp,
                color = cs.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(cs.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
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
    deleting?.let { f ->
        ConfirmDialog(
            title = "Delete \"${if (f.isDirectory) f.name else f.nameWithoutExtension}\"?",
            message = if (vm.settings.deletePermanently) "This can't be undone." else "It will be moved to the vault's .trash folder.",
            confirm = "Delete",
            onDismiss = { deleting = null },
        ) {
            deleting = null
            vm.delete(f)
        }
    }
}

@Composable
fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = onClick)
}
