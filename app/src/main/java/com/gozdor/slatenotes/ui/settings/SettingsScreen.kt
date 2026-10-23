package com.gozdor.slatenotes.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.BuildConfigInfo
import com.gozdor.slatenotes.data.SlateSettings
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.theme.AccentPresets
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val s = vm.settings
    fun set(t: (SlateSettings) -> SlateSettings) = vm.updateSettings(t)

    Surface(color = cs.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.closeOverlay() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                // ---- Help: one row that opens the topic list
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.openHelp() }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Help & tips")
                            Text("How everything in Slate works", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant)
                    }
                }

                // ---- Editor
                item { Group("Editor") }
                item {
                    Choice(
                        title = "Default view for new tabs",
                        options = listOf("Editing", "Reading"),
                        selected = if (s.defaultReading) 1 else 0,
                    ) { i -> set { it.copy(defaultReading = i == 1) } }
                }
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Font size: ${s.fontSize}")
                        Text("Text size in the editor and reading view.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        Slider(
                            value = s.fontSize.toFloat(),
                            onValueChange = { v -> set { it.copy(fontSize = v.roundToInt()) } },
                            valueRange = 12f..24f,
                            steps = 11,
                        )
                    }
                }
                item {
                    Toggle("Show inline title", "Show the note's file name as an editable title at the top.", s.showInlineTitle) { v ->
                        set { it.copy(showInlineTitle = v) }
                    }
                }
                item {
                    Toggle("Continue lists on Enter", "Add the next bullet, number or checkbox automatically.", s.continueLists) { v ->
                        set { it.copy(continueLists = v) }
                    }
                }

                // ---- Files and links
                item { Group("Files and links") }
                item {
                    Choice(
                        title = "Default location for new notes",
                        options = listOf("Vault folder", "Current folder"),
                        selected = if (s.newNotesInCurrentFolder) 1 else 0,
                    ) { i -> set { it.copy(newNotesInCurrentFolder = i == 1) } }
                }
                item {
                    Choice(
                        title = "Deleted files",
                        options = listOf("Vault .trash", "Delete forever"),
                        selected = if (s.deletePermanently) 1 else 0,
                    ) { i -> set { it.copy(deletePermanently = i == 1) } }
                }
                item {
                    Toggle("Confirm file deletion", "Ask before deleting a note or folder.", s.confirmDelete) { v ->
                        set { it.copy(confirmDelete = v) }
                    }
                }

                // ---- Appearance
                item { Group("Appearance") }
                item {
                    Choice(
                        title = "Base color scheme",
                        options = listOf("Adapt to system", "Light", "Dark"),
                        selected = when (s.theme) { "light" -> 1; "dark" -> 2; else -> 0 },
                    ) { i -> set { it.copy(theme = listOf("system", "light", "dark")[i]) } }
                }
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Accent color")
                        Text(
                            AccentPresets.firstOrNull { it.second == s.accent }?.first ?: "Custom",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AccentPresets.forEach { (name, color) ->
                                val selected = color == s.accent
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .background(Color(color), CircleShape)
                                        .border(if (selected) 3.dp else 0.dp, if (selected) cs.onSurface else Color.Transparent, CircleShape)
                                        .clickable { set { it.copy(accent = color) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // ---- Vault
                item { Group("Vault") }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.show(Overlay.Vaults) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Current vault: ${vm.vault?.name.orEmpty()}")
                            Text(vm.vault?.root?.absolutePath.orEmpty(), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Text("Switch", color = cs.primary)
                    }
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.refresh(); vm.toast("Reloaded vault from disk") }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) { Text("Reload vault from disk") }
                }

                // ---- About
                item { Group("About") }
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Slate Notes ${BuildConfigInfo.versionName(vm)}")
                        Text(
                            "A personal Markdown notes app inspired by Obsidian's workflow, built from its public docs and open file formats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Group(title: String) {
    Column {
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun Toggle(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                ) { Text(label, maxLines = 1) }
            }
        }
    }
}

// ------------------------------------------------------------------ help sheet

/** Pops up over whatever is on screen. Shows the topic list, or one topic. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(vm: AppViewModel) {
    val topic = vm.helpTopic ?: return
    val cs = MaterialTheme.colorScheme
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { vm.helpTopic = null },
        sheetState = state,
        containerColor = cs.surfaceContainerLow,
    ) {
        if (topic < 0 || topic >= HelpTopics.size) {
            Text(
                "Help & tips",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(HelpTopics) { i, t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.helpFromSettings = false; vm.helpTopic = i }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title)
                            Text(t.summary, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant)
                    }
                }
            }
        } else {
            val t = HelpTopics[topic]
            LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!vm.helpFromSettings) {
                            IconButton(onClick = { vm.helpTopic = -1 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All topics")
                            }
                        }
                        Text(t.title, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                t.blocks.forEach { b ->
                    item {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            b.heading?.let {
                                Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cs.primary)
                                Spacer(Modifier.height(4.dp))
                            }
                            b.text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            b.bullets.forEach { line ->
                                Row(Modifier.padding(vertical = 3.dp)) {
                                    Text("•", color = cs.primary, modifier = Modifier.width(18.dp))
                                    Text(line, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(cs.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            "Tip: every action is also in the command palette. Pull down from the top of a note to open it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
