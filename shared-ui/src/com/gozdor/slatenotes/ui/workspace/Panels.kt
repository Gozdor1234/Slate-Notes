package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.Md
import com.gozdor.slatenotes.data.SearchHit
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.JumpRequest
import com.gozdor.slatenotes.ui.screens.EmptyHint
import com.gozdor.slatenotes.ui.screens.NoteRow
import com.gozdor.slatenotes.ui.theme.Lucide
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Sidebar panels shared by the Android and Windows apps. */

@Composable
fun PanelHeader(title: String, actions: @Composable () -> Unit = {}) {
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

// ------------------------------------------------------------------ search

@Composable
fun SearchPanel(vm: AppViewModel) {
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
fun BookmarksPanel(vm: AppViewModel) {
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

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun BacklinksPanel(vm: AppViewModel) {
    val path = vm.activePath
    if (path == null) {
        EmptyHint("Open a note to see which notes link to it.")
        return
    }
    val links = remember(vm.index, path) { vm.index.backlinks(path) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { SectionTitle("Linked mentions (${links.size})") }
        if (links.isEmpty()) item { EmptyHint("No notes link here yet.") }
        items(links, key = { it.file.absolutePath }) { n ->
            NoteRow(n.name, n.rel) { vm.openNote(n.file) }
        }
    }
}

@Composable
fun OutgoingPanel(vm: AppViewModel) {
    val path = vm.activePath
    if (path == null) {
        EmptyHint("Open a note to see the links it contains.")
        return
    }
    val links = remember(vm.index, path) { vm.index.outgoing(path) }
    val resolved = links.filter { it.second != null }
    val unresolved = links.filter { it.second == null }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { SectionTitle("Links (${resolved.size})") }
        items(resolved, key = { "r:" + it.first }) { (_, n) ->
            NoteRow(n!!.name, n.rel) { vm.openNote(n.file) }
        }
        item { SectionTitle("Not created yet (${unresolved.size})") }
        items(unresolved, key = { "u:" + it.first }) { (target, _) ->
            NoteRow(target, "Tap to create") { vm.openLink(target) }
        }
    }
}

@Composable
fun TagsPanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val tags = vm.index.tagCounts
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { SectionTitle("Tags in this vault") }
        if (tags.isEmpty()) item { EmptyHint("No tags yet. Add #tags in a note or in its tags property.") }
        items(tags, key = { it.first.lowercase() }) { (tag, count) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.openTag(tag) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#$tag", color = cs.primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(count.toString(), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun OutlinePanel(vm: AppViewModel) {
    val note = vm.activeNote
    val path = vm.activePath
    if (note == null || path == null) {
        EmptyHint("Open a note to see its headings.")
        return
    }
    val headings = remember(note.content) { Md.headings(note.content) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { SectionTitle("Outline") }
        if (headings.isEmpty()) item { EmptyHint("This note has no headings. Start a line with # to add one.") }
        items(headings, key = { it.offset }) { h ->
            Text(
                h.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (h.level <= 2) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vm.jump = JumpRequest(path, h.offset)
                        vm.closeDrawers()
                    }
                    .padding(start = (16 + (h.level - 1) * 14).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )
        }
    }
}
