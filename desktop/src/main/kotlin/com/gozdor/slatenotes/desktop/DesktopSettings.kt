package com.gozdor.slatenotes.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.data.SlateSettings
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.theme.AccentPresets
import com.gozdor.slatenotes.ui.theme.Lucide
import kotlin.math.roundToInt

private val pages = listOf(
    "Editor" to Lucide.Pencil,
    "Files and links" to Lucide.Folder,
    "Appearance" to Lucide.Palette,
    "Hotkeys" to Lucide.Keyboard,
    "Help & tips" to Lucide.CircleQuestionMark,
    "About" to Lucide.Info,
)

@Composable
fun SettingsPanel(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    var page by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(cs.surfaceContainer)
                .padding(10.dp),
        ) {
            Text("Options", fontSize = 12.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            pages.forEachIndexed { i, (label, icon) ->
                NavRow(label, i == page, icon) {
                    if (label == "Help & tips") vm.openHelp() else page = i
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pages[page].first, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                HoverIcon(Lucide.X, "Close (Esc)") { vm.closeOverlay() }
            }
            Spacer(Modifier.height(12.dp))
            val s = vm.settings
            fun set(t: (SlateSettings) -> SlateSettings) = vm.updateSettings(t)
            when (pages[page].first) {
                "Editor" -> {
                    Setting("Default view for new tabs", "Open new tabs in editing or reading view.") {
                        Choice(listOf("Editing view", "Reading view"), if (s.defaultReading) 1 else 0) { i -> set { it.copy(defaultReading = i == 1) } }
                    }
                    Setting("Font size", "Text size in the editor and reading view: ${s.fontSize}") {
                        Slider(
                            value = s.fontSize.toFloat(),
                            onValueChange = { v -> set { it.copy(fontSize = v.roundToInt()) } },
                            valueRange = 12f..24f,
                            steps = 11,
                            modifier = Modifier.width(220.dp),
                        )
                    }
                    Setting("Show inline title", "Show the file name as an editable title at the top of the note.") {
                        Switch(s.showInlineTitle, { v -> set { it.copy(showInlineTitle = v) } })
                    }
                    Setting("Smart lists", "Continue bullets, numbers and checkboxes when you press Enter.") {
                        Switch(s.continueLists, { v -> set { it.copy(continueLists = v) } })
                    }
                }
                "Files and links" -> {
                    Setting("Default location for new notes", "Where newly created notes are placed.") {
                        Choice(listOf("Vault folder", "Same folder as current file"), if (s.newNotesInCurrentFolder) 1 else 0) { i ->
                            set { it.copy(newNotesInCurrentFolder = i == 1) }
                        }
                    }
                    Setting("Deleted files", "What happens to a file after you delete it.") {
                        Choice(listOf("Move to vault .trash folder", "Permanently delete"), if (s.deletePermanently) 1 else 0) { i ->
                            set { it.copy(deletePermanently = i == 1) }
                        }
                    }
                    Setting("Confirm file deletion", "Ask before deleting a note or folder.") {
                        Switch(s.confirmDelete, { v -> set { it.copy(confirmDelete = v) } })
                    }
                    Setting("Reload vault", "Re-read every file from disk.") {
                        OutlinedButton(onClick = { vm.refresh(); vm.toast("Reloaded vault from disk") }) { Text("Reload") }
                    }
                }
                "Appearance" -> {
                    Setting("Base color scheme", "Light, dark, or follow Windows.") {
                        Choice(listOf("Adapt to system", "Light", "Dark"), when (s.theme) { "light" -> 1; "dark" -> 2; else -> 0 }) { i ->
                            set { it.copy(theme = listOf("system", "light", "dark")[i]) }
                        }
                    }
                    Setting("Accent color", AccentPresets.firstOrNull { it.second == s.accent }?.first ?: "Custom") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccentPresets.forEach { (name, color) ->
                                val selected = color == s.accent
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(if (selected) 2.dp else 0.dp, if (selected) cs.onSurface else Color.Transparent, CircleShape)
                                        .clickable { set { it.copy(accent = color) } },
                                ) {}
                            }
                        }
                    }
                }
                "Hotkeys" -> {
                    WindowsHelp.first { it.first == "Keyboard shortcuts" }.second.forEach { line ->
                        Text(line, fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))
                        HorizontalDivider(color = cs.outlineVariant)
                    }
                }
                "About" -> {
                    Setting("Slate Notes", "Version ${vm.platform.appVersion}") {}
                    Setting("Current vault", vm.vault?.root?.absolutePath.orEmpty()) {
                        OutlinedButton(onClick = { vm.show(Overlay.Vaults) }) { Text("Switch vault") }
                    }
                    Text(
                        "A personal Markdown notes app inspired by Obsidian's workflow, built from its public docs and open file formats.",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

/** One settings row: name and description on the left, the control on the right. */
@Composable
private fun Setting(name: String, desc: String, control: @Composable () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 24.dp)) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            control()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** A dropdown, like Obsidian's settings selects. */
@Composable
private fun Choice(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(options[selected], fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Lucide.ChevronDown, null, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o, fontSize = 13.sp) }, onClick = { open = false; onSelect(i) })
            }
        }
    }
}
