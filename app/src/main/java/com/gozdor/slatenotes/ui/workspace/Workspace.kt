package com.gozdor.slatenotes.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.screens.ConfirmDialog
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(vm: AppViewModel) {
    val imeVisible = WindowInsets.isImeVisible
    val cs = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            ViewHeader(vm)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding(),
            ) {
                val path = vm.activePath
                val tabId = vm.activeTabId
                if (path == null) {
                    EmptyTab(vm)
                } else {
                    androidx.compose.runtime.key(tabId, path) { NoteView(vm, tabId, path) }
                }
            }
            if (!imeVisible) NavBar(vm)
        }
        SidePanels(vm)
    }
}

// ------------------------------------------------------------------ header

@Composable
private fun ViewHeader(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val path = vm.activePath
    val tabId = vm.activeTabId
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val reading = vm.isReading(tabId)

    Surface(color = cs.background) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.openLeft() }) {
                    Icon(Icons.Outlined.ViewSidebar, contentDescription = "Open left sidebar", tint = cs.onSurfaceVariant)
                }
                Text(
                    text = path?.let { File(it).nameWithoutExtension } ?: "New tab",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (path != null) {
                    IconButton(onClick = { vm.setReading(tabId, !reading) }) {
                        if (reading) Icon(Icons.Default.Edit, contentDescription = "Switch to editing view", tint = cs.onSurfaceVariant)
                        else Icon(Icons.Default.MenuBook, contentDescription = "Switch to reading view", tint = cs.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = cs.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (vm.isBookmarked(path)) "Remove bookmark" else "Bookmark") },
                                onClick = { menu = false; vm.toggleBookmark(path) },
                            )
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; vm.requestRename() })
                            DropdownMenuItem(text = { Text("Move file to…") }, onClick = { menu = false; vm.moveTarget = File(path) })
                            DropdownMenuItem(
                                text = { Text("Open backlinks") },
                                onClick = { menu = false; vm.openRight(0) },
                            )
                            DropdownMenuItem(
                                text = { Text("Open outline") },
                                onClick = { menu = false; vm.openRight(3) },
                            )
                            DropdownMenuItem(
                                text = { Text("Close tab") },
                                onClick = { menu = false; vm.closeTab(vm.activeTab) },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete file", color = cs.error) },
                                onClick = {
                                    menu = false
                                    if (vm.settings.confirmDelete) confirmDelete = true else vm.delete(File(path))
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { vm.openRight() }) {
                    Icon(
                        Icons.Outlined.ViewSidebar,
                        contentDescription = "Open right sidebar",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                    )
                }
            }
        }
    }

    if (confirmDelete && path != null) {
        DeleteDialog(vm, File(path)) { confirmDelete = false }
    }
}

@Composable
fun DeleteDialog(vm: AppViewModel, file: File, onDone: () -> Unit) {
    ConfirmDialog(
        title = "Delete \"${if (file.isDirectory) file.name else file.nameWithoutExtension}\"?",
        message = if (vm.settings.deletePermanently) "This can't be undone."
        else "It will be moved to the vault's .trash folder.",
        confirm = "Delete",
        onDismiss = onDone,
    ) {
        onDone()
        vm.delete(file)
    }
}

// ------------------------------------------------------------------ bottom navigation bar

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavBar(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    var ribbon by remember { mutableStateOf(false) }
    Surface(color = cs.surfaceContainer) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = cs.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavButton(Icons.Default.ChevronLeft, "Back", vm.canGoBack) { vm.goBack() }
                NavButton(Icons.Default.ChevronRight, "Forward", vm.canGoForward) { vm.goForward() }
                NavButton(Icons.Default.AddCircleOutline, "Create or find note", true) { vm.show(Overlay.QuickSwitcher) }
                Box(
                    Modifier
                        .size(48.dp)
                        .combinedClickable(
                            onClick = { vm.show(Overlay.Tabs) },
                            onLongClick = { vm.newTab() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .border(1.8.dp, cs.onSurfaceVariant, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            vm.tabs.size.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
                Box {
                    NavButton(Icons.Default.Menu, "Open menu", true) { ribbon = true }
                    RibbonMenu(vm, ribbon) { ribbon = false }
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = label, tint = if (enabled) cs.onSurfaceVariant else cs.onSurfaceVariant.copy(alpha = 0.3f))
    }
}

/** On mobile, Obsidian's ribbon actions live behind the menu button. Same here. */
@Composable
private fun RibbonMenu(vm: AppViewModel, open: Boolean, onDismiss: () -> Unit) {
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Command palette") }, onClick = { onDismiss(); vm.show(Overlay.Commands) })
        DropdownMenuItem(text = { Text("Search") }, onClick = { onDismiss(); vm.openLeft(1) })
        DropdownMenuItem(text = { Text("Bookmarks") }, onClick = { onDismiss(); vm.openLeft(2) })
        DropdownMenuItem(text = { Text("New note") }, onClick = { onDismiss(); vm.createNote() })
        HorizontalDivider()
        DropdownMenuItem(text = { Text("Open another vault") }, onClick = { onDismiss(); vm.show(Overlay.Vaults) })
        DropdownMenuItem(text = { Text("Help") }, onClick = { onDismiss(); vm.openHelp() })
        DropdownMenuItem(text = { Text("Settings") }, onClick = { onDismiss(); vm.show(Overlay.Settings) })
    }
}

// ------------------------------------------------------------------ empty tab

@Composable
private fun EmptyTab(vm: AppViewModel) {
    val pull = rememberPullDownToCommand { vm.show(Overlay.Commands) }
    EmptyState(
        title = "No file is open",
        actions = listOf(
            "Create new note" to { vm.createNote() },
            "Go to file" to { vm.show(Overlay.QuickSwitcher) },
            "Close" to { vm.closeTab(vm.activeTab) },
        ),
        modifier = Modifier.nestedScroll(pull),
    )
}

@Composable
fun EmptyState(title: String, actions: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            actions.forEach { (label, action) ->
                TextButton(onClick = action) { Text(label, color = cs.primary) }
            }
            Spacer(Modifier.height(400.dp))
        }
    }
}

/**
 * Pull down from the top of a note to open the command palette (Obsidian's default mobile Quick Action).
 * Fires once per drag when the content is already scrolled to the top.
 */
@Composable
fun rememberPullDownToCommand(onPull: () -> Unit): NestedScrollConnection {
    val currentOnPull = androidx.compose.runtime.rememberUpdatedState(onPull)
    return remember {
        object : NestedScrollConnection {
            var pulled = 0f
            var fired = false

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > 260f && !fired) {
                        fired = true
                        currentOnPull.value()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pulled = 0f
                fired = false
                return Velocity.Zero
            }
        }
    }
}

// ------------------------------------------------------------------ sidebars

@Composable
private fun BoxScope.SidePanels(vm: AppViewModel) {
    val anyOpen = vm.leftOpen || vm.rightOpen
    val scrim by animateFloatAsState(if (anyOpen) 0.45f else 0f, label = "scrim")
    if (scrim > 0.01f) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .pointerInputTap { vm.closeSidebars() },
        )
    }

    // Edge swipe zones. Android reserves edge swipes for "back" when gesture navigation is on,
    // so these zones ask the system to exclude them (Android limits this to a 200dp tall strip).
    if (!anyOpen) {
        EdgeZone(Modifier.align(Alignment.CenterStart), opensFromLeft = true) { vm.openLeft() }
        EdgeZone(Modifier.align(Alignment.CenterEnd), opensFromLeft = false) { vm.openRight() }
    }

    AnimatedVisibility(
        visible = vm.leftOpen,
        enter = slideInHorizontally { -it },
        exit = slideOutHorizontally { -it },
        modifier = Modifier.align(Alignment.CenterStart),
    ) {
        PanelFrame(onSwipeClose = { vm.closeSidebars() }, closeDirection = -1) { LeftSidebar(vm) }
    }
    AnimatedVisibility(
        visible = vm.rightOpen,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        PanelFrame(onSwipeClose = { vm.closeSidebars() }, closeDirection = 1) { RightSidebar(vm) }
    }
}

@Composable
private fun EdgeZone(modifier: Modifier, opensFromLeft: Boolean, onOpen: () -> Unit) {
    Box(
        modifier
            .width(18.dp)
            .height(200.dp)
            .systemGestureExclusion()
            .pointerInputDrag { amount ->
                if ((opensFromLeft && amount > 6f) || (!opensFromLeft && amount < -6f)) onOpen()
            },
    )
}

@Composable
private fun PanelFrame(onSwipeClose: () -> Unit, closeDirection: Int, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.86f)
            .widthIn(max = 420.dp)
            .pointerInputDrag { amount -> if (amount * closeDirection > 8f) onSwipeClose() },
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) { content() }
    }
}

private fun Modifier.pointerInputTap(onTap: () -> Unit): Modifier =
    pointerInput("tap") { detectTapGestures { onTap() } }

private fun Modifier.pointerInputDrag(onDrag: (Float) -> Unit): Modifier =
    pointerInput("drag") { detectHorizontalDragGestures { _, amount -> onDrag(amount) } }
