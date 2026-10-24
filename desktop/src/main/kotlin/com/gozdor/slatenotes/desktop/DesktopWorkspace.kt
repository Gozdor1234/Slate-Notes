package com.gozdor.slatenotes.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.canvas.CanvasView
import com.gozdor.slatenotes.ui.theme.Lucide
import com.gozdor.slatenotes.ui.workspace.BacklinksPanel
import com.gozdor.slatenotes.ui.workspace.BookmarksPanel
import com.gozdor.slatenotes.ui.workspace.MoveDialog
import com.gozdor.slatenotes.ui.workspace.OutgoingPanel
import com.gozdor.slatenotes.ui.workspace.OutlinePanel
import com.gozdor.slatenotes.ui.workspace.PillButton
import com.gozdor.slatenotes.ui.workspace.SearchPanel
import com.gozdor.slatenotes.ui.workspace.TagsPanel
import java.awt.Cursor
import java.io.File

const val HEADER_HEIGHT = 40

@Composable
fun DesktopApp(vm: AppViewModel, platform: DesktopPlatform) {
    Box(Modifier.fillMaxSize()) {
        if (vm.vault == null) {
            VaultChooser(vm, platform, modal = false)
        } else {
            Workspace(vm)
        }
        when (vm.overlay) {
            Overlay.QuickSwitcher -> Modal(vm) { QuickSwitcherPanel(vm) }
            Overlay.Commands -> Modal(vm) { CommandPalettePanel(vm) }
            Overlay.Settings -> Modal(vm, wide = true) { SettingsPanel(vm) }
            Overlay.Vaults -> if (vm.vault != null) Modal(vm, wide = true) { VaultChooser(vm, platform, modal = true) }
            else -> Unit
        }
        if (vm.helpTopic != null) HelpModal(vm)
        MoveDialog(vm)
    }
}

@Composable
private fun Workspace(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    var leftWidth by remember { mutableStateOf(280.dp) }
    var rightWidth by remember { mutableStateOf(300.dp) }

    Row(Modifier.fillMaxSize()) {
        Ribbon(vm)
        VerticalDivider(color = cs.outlineVariant)
        AnimatedVisibility(vm.leftOpen, enter = expandHorizontally(), exit = shrinkHorizontally()) {
            Row {
                Column(
                    Modifier
                        .width(leftWidth)
                        .fillMaxHeight()
                        .background(cs.surfaceContainer),
                ) { LeftSidebar(vm) }
                ResizeHandle { leftWidth = (leftWidth + it).coerceIn(200.dp, 520.dp) }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            TabBar(vm)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val path = vm.activePath
                val tabId = vm.activeTabId
                when {
                    path == null -> EmptyTab(vm)
                    path.endsWith(".canvas", ignoreCase = true) -> androidx.compose.runtime.key(tabId, path) {
                        Column {
                            ViewHeader(vm, path)
                            CanvasView(vm, path, controlsBottomPadding = 40.dp)
                        }
                    }
                    else -> androidx.compose.runtime.key(tabId, path) { DesktopNoteView(vm, tabId, path) }
                }
                StatusBar(vm, Modifier.align(Alignment.BottomEnd))
            }
        }
        AnimatedVisibility(vm.rightOpen, enter = expandHorizontally(), exit = shrinkHorizontally()) {
            Row {
                ResizeHandle { rightWidth = (rightWidth - it).coerceIn(200.dp, 520.dp) }
                Column(
                    Modifier
                        .width(rightWidth)
                        .fillMaxHeight()
                        .background(cs.surfaceContainer),
                ) { RightSidebar(vm) }
            }
        }
    }
}

/** Thin border you can drag to resize a sidebar. */
@Composable
private fun ResizeHandle(onDrag: (Dp) -> Unit) {
    val density = LocalDensity.current
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .width(5.dp)
            .fillMaxHeight()
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, amount ->
                    change.consume()
                    onDrag(with(density) { amount.toDp() })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(if (hovered) 3.dp else 1.dp)
                .fillMaxHeight()
                .background(if (hovered) cs.primary else cs.outlineVariant),
        )
    }
}

// ------------------------------------------------------------------ small building blocks

/** Square icon button with a hover highlight, the way Obsidian's desktop buttons behave. */
@Composable
fun HoverIcon(icon: ImageVector, description: String, size: Dp = 30.dp, active: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    active -> cs.surfaceContainerHighest
                    hovered && enabled -> cs.surfaceContainerHigh
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (!enabled) cs.onSurfaceVariant.copy(alpha = 0.35f) else if (active || hovered) cs.onSurface else cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun HeaderRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

// ------------------------------------------------------------------ ribbon

@Composable
private fun Ribbon(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(cs.surfaceContainer)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HoverIcon(Lucide.PanelLeft, if (vm.leftOpen) "Collapse left sidebar" else "Expand left sidebar") { vm.leftOpen = !vm.leftOpen }
        Spacer(Modifier.height(6.dp))
        HoverIcon(Lucide.FileSearch, "Open quick switcher") { vm.show(Overlay.QuickSwitcher) }
        HoverIcon(Lucide.LayoutDashboard, "Create new canvas") { vm.createCanvas() }
        HoverIcon(Lucide.Terminal, "Open command palette") { vm.show(Overlay.Commands) }
        HoverIcon(Lucide.SquarePen, "Create new note") { vm.createNote() }
        Spacer(Modifier.weight(1f))
        // With the left sidebar collapsed, its vault/help/settings buttons move here.
        if (!vm.leftOpen) {
            HoverIcon(Lucide.FolderOpen, "Open another vault") { vm.show(Overlay.Vaults) }
            HoverIcon(Lucide.CircleQuestionMark, "Help") { vm.openHelp() }
            HoverIcon(Lucide.Settings, "Settings") { vm.show(Overlay.Settings) }
        }
    }
}

// ------------------------------------------------------------------ left sidebar

@Composable
private fun ColumnScope.LeftSidebar(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    HeaderRow {
        HoverIcon(Lucide.Folder, "Files", active = vm.leftPanel == 0) { vm.leftPanel = 0 }
        HoverIcon(Lucide.Search, "Search", active = vm.leftPanel == 1) { vm.leftPanel = 1 }
        HoverIcon(Lucide.Bookmark, "Bookmarks", active = vm.leftPanel == 2) { vm.leftPanel = 2 }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        when (vm.leftPanel) {
            0 -> FileExplorer(vm)
            1 -> SearchPanel(vm)
            else -> BookmarksPanel(vm)
        }
    }
    // Vault switcher, help and settings at the bottom, like Obsidian.
    val (files, folders) = remember(vm.tree) { countTree(vm.tree) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable { vm.show(Overlay.Vaults) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    vm.vault?.name.orEmpty(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Lucide.ChevronsUpDown, contentDescription = "Switch vault", tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
            Text(
                "$files ${if (files == 1) "file" else "files"}, $folders ${if (folders == 1) "folder" else "folders"}",
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
            )
        }
        HoverIcon(Lucide.CircleQuestionMark, "Help") { vm.openHelp() }
        HoverIcon(Lucide.Settings, "Settings") { vm.show(Overlay.Settings) }
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

// ------------------------------------------------------------------ right sidebar

@Composable
private fun ColumnScope.RightSidebar(vm: AppViewModel) {
    HeaderRow {
        HoverIcon(Lucide.ArrowDownLeft, "Backlinks", active = vm.rightPanel == 0) { vm.rightPanel = 0 }
        HoverIcon(Lucide.ArrowUpRight, "Outgoing links", active = vm.rightPanel == 1) { vm.rightPanel = 1 }
        HoverIcon(Lucide.Tags, "Tags", active = vm.rightPanel == 2) { vm.rightPanel = 2 }
        HoverIcon(Lucide.List, "Outline", active = vm.rightPanel == 3) { vm.rightPanel = 3 }
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        when (vm.rightPanel) {
            0 -> BacklinksPanel(vm)
            1 -> OutgoingPanel(vm)
            2 -> TagsPanel(vm)
            else -> OutlinePanel(vm)
        }
    }
}

// ------------------------------------------------------------------ tabs

@Composable
private fun TabBar(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT.dp)
            .background(cs.surfaceContainer)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp),
    ) {
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(end = 40.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
        ) {
            vm.tabs.forEachIndexed { i, t ->
                TabItem(
                    title = t.path?.let { File(it).nameWithoutExtension } ?: "New tab",
                    isCanvas = t.path?.endsWith(".canvas", ignoreCase = true) == true,
                    active = i == vm.activeTab,
                    onClick = { vm.switchTab(i) },
                    onClose = { vm.closeTab(i) },
                )
            }
            Box(Modifier.padding(bottom = 3.dp, start = 2.dp)) {
                HoverIcon(Lucide.Plus, "New tab (Ctrl+T)") { vm.newTab() }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 3.dp),
        ) {
            HoverIcon(Lucide.PanelRight, if (vm.rightOpen) "Collapse right sidebar" else "Expand right sidebar") {
                vm.rightOpen = !vm.rightOpen
            }
        }
    }
}

@Composable
private fun TabItem(title: String, isCanvas: Boolean, active: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .height(34.dp)
            .widthIn(min = 120.dp, max = 220.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(
                when {
                    active -> cs.background
                    hovered -> cs.surfaceContainerHigh
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCanvas) {
            Icon(Lucide.LayoutDashboard, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            title,
            fontSize = 13.sp,
            color = if (active) cs.onSurface else cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (active || hovered) HoverIcon(Lucide.X, "Close tab (Ctrl+W)", size = 20.dp, onClick = onClose)
        }
    }
}

// ------------------------------------------------------------------ view header, empty tab, status bar

@Composable
fun ViewHeader(vm: AppViewModel, path: String, actions: @Composable () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val v = vm.vault
    val f = File(path)
    val folder = f.parentFile?.let { p -> v?.rel(p) }.orEmpty()
    HeaderRow {
        HoverIcon(Lucide.ChevronLeft, "Navigate back (Ctrl+Alt+Left)", enabled = vm.canGoBack) { vm.goBack() }
        HoverIcon(Lucide.ChevronRight, "Navigate forward (Ctrl+Alt+Right)", enabled = vm.canGoForward) { vm.goForward() }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (folder.isNotEmpty()) {
                Text(folder.replace("/", " / ") + " / ", fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 1)
            }
            Text(f.nameWithoutExtension, fontSize = 13.sp, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
private fun EmptyTab(vm: AppViewModel) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PillButton("New note", Lucide.SquarePen, width = 142.dp) { vm.createNote() }
        PillButton("New base", Lucide.LayoutList, width = 142.dp) { vm.toast("Bases are coming in a later version") }
        PillButton("New canvas", Lucide.LayoutDashboard, width = 142.dp) { vm.createCanvas() }
    }
}

@Composable
private fun StatusBar(vm: AppViewModel, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val path = vm.activePath ?: return
    if (path.endsWith(".canvas", ignoreCase = true)) return
    val note = vm.index.notes[path] ?: return
    val backlinks = remember(vm.index, path) { vm.index.backlinks(path).size }
    val (words, chars) = remember(note.content) { wordAndCharCount(note.content) }
    Row(
        modifier
            .clip(RoundedCornerShape(topStart = 8.dp))
            .background(cs.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "$backlinks ${if (backlinks == 1) "backlink" else "backlinks"}",
            fontSize = 12.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.clickable { vm.openRight(0) },
        )
        Text("$words ${if (words == 1) "word" else "words"}  $chars ${if (chars == 1) "character" else "characters"}", fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}

/** Word and character counts for the note body (frontmatter excluded). */
fun wordAndCharCount(text: String): Pair<Int, Int> {
    val body = text.substring(com.gozdor.slatenotes.data.Md.frontmatter(text)?.end ?: 0)
    val words = Regex("\\S+").findAll(body).count()
    return words to body.length
}
