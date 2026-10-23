package com.gozdor.slatenotes.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Screen
import java.io.File
import java.text.DateFormat
import java.util.Date

private data class TreeRow(val node: TreeNode, val depth: Int)

private sealed interface TreeAction {
    data class NewFolder(val parent: File) : TreeAction
    data class Rename(val file: File) : TreeAction
    data class Delete(val file: File) : TreeAction
}

private fun flatten(nodes: List<TreeNode>, expanded: Map<String, Boolean>, depth: Int, out: MutableList<TreeRow>) {
    for (n in nodes) {
        out += TreeRow(n, depth)
        if (n.isDir && expanded[n.file.absolutePath] == true) flatten(n.children, expanded, depth + 1, out)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val vault = vm.vault ?: return
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var action by remember { mutableStateOf<TreeAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        Modifier.clickable { vm.navigate(Screen.Vaults) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(vault.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch vault")
                    }
                },
                actions = {
                    if (vm.indexing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { action = TreeAction.NewFolder(vault.root) }) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder")
                    }
                    IconButton(onClick = { vm.navigate(Screen.Search()) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.createNote(vault.root) }) {
                Icon(Icons.Default.Edit, contentDescription = "New note")
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            TabRow(selectedTabIndex = tab) {
                listOf("Files", "Recent", "Tags").forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> FileTree(vm) { action = it }
                1 -> RecentList(vm)
                else -> TagList(vm)
            }
        }
    }

    when (val a = action) {
        is TreeAction.NewFolder -> NameDialog("New folder", "New folder", "Create", { action = null }) {
            action = null
            vm.createFolder(a.parent, it)
        }
        is TreeAction.Rename -> NameDialog(
            "Rename",
            if (a.file.isDirectory) a.file.name else a.file.nameWithoutExtension,
            "Rename",
            { action = null },
        ) {
            action = null
            vm.renameFile(a.file, it)
        }
        is TreeAction.Delete -> ConfirmDialog(
            title = "Delete \"${a.file.name}\"?",
            message = if (a.file.isDirectory) "The folder and everything in it will be moved to the vault's .trash folder."
            else "It will be moved to the vault's .trash folder.",
            confirm = "Delete",
            onDismiss = { action = null },
        ) {
            action = null
            vm.delete(a.file)
        }
        null -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTree(vm: AppViewModel, onAction: (TreeAction) -> Unit) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val expandedSnapshot = expanded.toMap()
    val rows = remember(vm.tree, expandedSnapshot) {
        mutableListOf<TreeRow>().also { flatten(vm.tree, expandedSnapshot, 0, it) }
    }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val cs = MaterialTheme.colorScheme

    if (rows.isEmpty() && !vm.indexing) {
        EmptyHint("No notes yet. Tap the pencil to create one.")
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(rows, key = { it.node.file.absolutePath }) { row ->
            val f = row.node.file
            val path = f.absolutePath
            val isMd = !row.node.isDir && f.extension.equals("md", ignoreCase = true)
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                when {
                                    row.node.isDir -> expanded[path] = expanded[path] != true
                                    isMd -> vm.openNote(f)
                                    f.extension.equals("canvas", ignoreCase = true) ->
                                        vm.toast("Canvas files open in a later version")
                                    else -> vm.toast("Can't open .${f.extension} files yet")
                                }
                            },
                            onLongClick = { menuFor = path },
                        )
                        .padding(start = (12 + row.depth * 18).dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (row.node.isDir) {
                        Icon(
                            if (expanded[path] == true) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.Folder, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                    } else {
                        Spacer(Modifier.width(24.dp))
                        Icon(
                            if (isMd) Icons.Outlined.Description else Icons.Outlined.InsertDriveFile,
                            null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isMd) f.nameWithoutExtension else f.name,
                        color = if (row.node.isDir || isMd) cs.onSurface else cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(expanded = menuFor == path, onDismissRequest = { menuFor = null }) {
                    if (row.node.isDir) {
                        DropdownMenuItem(text = { Text("New note here") }, onClick = {
                            menuFor = null
                            vm.createNote(f)
                        })
                        DropdownMenuItem(text = { Text("New folder here") }, onClick = {
                            menuFor = null
                            onAction(TreeAction.NewFolder(f))
                        })
                    }
                    DropdownMenuItem(text = { Text("Rename") }, onClick = {
                        menuFor = null
                        onAction(TreeAction.Rename(f))
                    })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = {
                        menuFor = null
                        onAction(TreeAction.Delete(f))
                    })
                }
            }
        }
    }
}

@Composable
private fun RecentList(vm: AppViewModel) {
    val notes = vm.recentNotes()
    if (notes.isEmpty()) {
        EmptyHint("Notes you open will show up here.")
        return
    }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(notes, key = { it.file.absolutePath }) { n ->
            NoteRow(
                title = n.name,
                subtitle = n.rel.substringBeforeLast('/', "").ifEmpty { "/" } + "  ·  " + fmt.format(Date(n.modified)),
                onClick = { vm.openNote(n.file) },
            )
        }
    }
}

@Composable
private fun TagList(vm: AppViewModel) {
    val tags = vm.index.tagCounts
    if (tags.isEmpty()) {
        EmptyHint("No tags yet. Add #tags in a note or in its properties.")
        return
    }
    val cs = MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(tags, key = { it.first.lowercase() }) { (tag, count) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.openTag(tag) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#$tag", color = cs.primary, modifier = Modifier.weight(1f))
                Text(count.toString(), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun NoteRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(title, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
