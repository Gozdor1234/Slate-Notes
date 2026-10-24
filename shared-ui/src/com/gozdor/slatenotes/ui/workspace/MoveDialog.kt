package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.Fuzzy
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.ui.AppViewModel
import java.io.File

private data class FolderOption(val dir: File, val label: String, val depth: Int)

private fun collectFolders(nodes: List<TreeNode>, depth: Int, out: MutableList<FolderOption>, root: File) {
    for (n in nodes) {
        if (!n.isDir) continue
        out += FolderOption(n.file, n.file.absolutePath.removePrefix(root.absolutePath).trimStart('/'), depth)
        collectFolders(n.children, depth + 1, out, root)
    }
}

/** Folder picker for "Move file to…". Type to filter folders by path. */
@Composable
fun MoveDialog(vm: AppViewModel) {
    val item = vm.moveTarget ?: return
    val vault = vm.vault ?: return
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }

    val all = remember(vm.tree, item) {
        val out = mutableListOf(FolderOption(vault.root, "Vault folder (top level)", 0))
        collectFolders(vm.tree, 0, out, vault.root)
        out.filter { opt ->
            // Can't move a folder into itself or its subfolders, and skip where it already is.
            !(item.isDirectory && vault.isSelfOrInside(item, opt.dir)) &&
                opt.dir.absolutePath != item.parentFile?.absolutePath
        }
    }
    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) all
    else all.mapNotNull { o -> Fuzzy.score(q, o.label.lowercase())?.let { o to it } }.sortedBy { it.second }.map { it.first }

    AlertDialog(
        onDismissRequest = { vm.moveTarget = null },
        title = { Text("Move \"${if (item.isDirectory) item.name else item.nameWithoutExtension}\" to…") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Filter folders") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.width(8.dp))
                if (shown.isEmpty()) {
                    Text(
                        "No other folders. Create one first from the Files panel.",
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    items(shown, key = { it.dir.absolutePath }) { o ->
                        val isRoot = o.dir == vault.root
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.moveTarget = null
                                    vm.moveFile(item, o.dir)
                                }
                                .padding(start = if (q.isEmpty() && !isRoot) (o.depth * 16).dp else 0.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (isRoot) Icons.Outlined.Home else Icons.Outlined.Folder,
                                null, tint = cs.primary, modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (q.isEmpty() && !isRoot) o.dir.name else o.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { vm.moveTarget = null }) { Text("Cancel") } },
    )
}
