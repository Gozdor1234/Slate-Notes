package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.Md
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.JumpRequest
import com.gozdor.slatenotes.ui.screens.EmptyHint
import com.gozdor.slatenotes.ui.screens.NoteRow
import com.gozdor.slatenotes.ui.theme.Lucide
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun ColumnScope.RightSidebar(vm: AppViewModel) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = vm.rightPanel,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "rightPanel",
        ) { panel ->
            when (panel) {
                0 -> BacklinksPanel(vm)
                1 -> OutgoingPanel(vm)
                2 -> TagsPanel(vm)
                else -> OutlinePanel(vm)
            }
        }
    }
    ViewSwitcherPill(
        options = listOf(
            Lucide.ArrowDownLeft to "Backlinks",
            Lucide.ArrowUpRight to "Outgoing links",
            Lucide.Tags to "Tags",
            Lucide.List to "Outline",
        ),
        selected = vm.rightPanel,
        onSelect = { vm.rightPanel = it },
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun BacklinksPanel(vm: AppViewModel) {
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
private fun OutgoingPanel(vm: AppViewModel) {
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
private fun TagsPanel(vm: AppViewModel) {
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
private fun OutlinePanel(vm: AppViewModel) {
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
                        vm.closeSidebars()
                    }
                    .padding(start = (16 + (h.level - 1) * 14).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            )
        }
    }
}
