package com.gozdor.slatenotes.ui.workspace

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.screens.ConfirmDialog
import com.gozdor.slatenotes.ui.theme.Lucide
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(vm: AppViewModel) {
    val imeVisible = WindowInsets.isImeVisible
    val cs = MaterialTheme.colorScheme
    var ribbonOpen by remember { mutableStateOf(false) }

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
                // Fade between notes when switching tabs or following links.
                Crossfade(
                    targetState = vm.activeTabId to vm.activePath,
                    animationSpec = tween(180),
                    label = "note",
                ) { (tabId, path) ->
                    if (path == null) EmptyTab(vm) else key(tabId, path) { NoteView(vm, tabId, path) }
                }
            }
        }
        // The navigation bar floats over the content, like Obsidian's.
        if (!imeVisible) {
            NavPill(vm, onMenu = { ribbonOpen = true }, modifier = Modifier.align(Alignment.BottomCenter))
        }
        SidePanels(vm)
    }

    if (ribbonOpen) RibbonSheet(vm) { ribbonOpen = false }
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

    Row(
        Modifier
            .fillMaxWidth()
            .background(cs.background)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Lucide.PanelLeft, "Open left sidebar") { vm.openLeft() }
        Text(
            text = path?.let { File(it).nameWithoutExtension } ?: "New tab",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Box {
            CircleIconButton(Lucide.EllipsisVertical, "More options") { menu = true }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = cs.surface,
            ) {
                if (path != null) {
                    MenuRow(if (reading) Lucide.Pencil else Lucide.BookOpen, if (reading) "Editing view" else "Reading view") {
                        menu = false
                        vm.setReading(tabId, !reading)
                    }
                    MenuRow(Lucide.Bookmark, if (vm.isBookmarked(path)) "Remove bookmark" else "Bookmark") {
                        menu = false
                        vm.toggleBookmark(path)
                    }
                    MenuRow(Lucide.Pencil, "Rename") { menu = false; vm.requestRename() }
                    MenuRow(Lucide.FolderInput, "Move file to…") { menu = false; vm.moveTarget = File(path) }
                    MenuRow(Lucide.ArrowDownLeft, "Backlinks") { menu = false; vm.openRight(0) }
                    MenuRow(Lucide.List, "Outline") { menu = false; vm.openRight(3) }
                    MenuRow(Lucide.X, "Close tab") { menu = false; vm.closeTab(vm.activeTab) }
                    HorizontalDivider(color = cs.outlineVariant)
                    DropdownMenuItem(
                        text = { Text("Delete file", color = cs.error) },
                        leadingIcon = { Icon(Lucide.Trash, null, tint = cs.error, modifier = Modifier.size(20.dp)) },
                        onClick = {
                            menu = false
                            if (vm.settings.confirmDelete) confirmDelete = true else vm.delete(File(path))
                        },
                    )
                } else {
                    MenuRow(Lucide.SquarePen, "New note") { menu = false; vm.createNote() }
                    MenuRow(Lucide.X, "Close tab") { menu = false; vm.closeTab(vm.activeTab) }
                }
            }
        }
    }

    if (confirmDelete && path != null) {
        DeleteDialog(vm, File(path)) { confirmDelete = false }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
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

// ------------------------------------------------------------------ floating navigation bar

@Composable
private fun NavPill(vm: AppViewModel, onMenu: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = cs.surface,
            shadowElevation = 10.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavIcon(Lucide.ChevronLeft, "Back", vm.canGoBack) { vm.goBack() }
                NavIcon(Lucide.ChevronRight, "Forward", vm.canGoForward) { vm.goForward() }
                NavIcon(Lucide.Search, "Find or create note") { vm.show(Overlay.QuickSwitcher) }
                NavIcon(Lucide.Plus, "New note") { vm.createNote() }
                TabCount(vm)
                NavIcon(Lucide.Menu, "Open menu", onClick = onMenu)
            }
        }
    }
}

@Composable
private fun TabCount(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(52.dp)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null) { vm.show(Overlay.Tabs) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 30.dp, height = 26.dp)
                .border(2.dp, cs.onSurface, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                vm.tabs.size.coerceAtMost(99).toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
        }
    }
}

/** The menu button's sheet. On mobile, Obsidian puts its ribbon actions here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RibbonSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Slide the sheet away first, then run the action.
    fun pick(action: () -> Unit) {
        scope.launch { state.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    val items = listOf(
        Triple(Lucide.FileSearch, "Open quick switcher") { vm.show(Overlay.QuickSwitcher) },
        Triple(Lucide.SquarePen, "Create new note") { vm.createNote() },
        Triple(Lucide.Search, "Search") { vm.openLeft(1) },
        Triple(Lucide.Bookmark, "Bookmarks") { vm.openLeft(2) },
        Triple(Lucide.Terminal, "Open command palette") { vm.show(Overlay.Commands) },
        Triple(Lucide.CircleQuestionMark, "Help & tips") { vm.openHelp() },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = cs.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cs.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Column {
                items.forEachIndexed { i, (icon, label, action) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { pick(action) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(label, fontSize = 17.sp, color = cs.onSurface)
                    }
                    if (i < items.lastIndex) {
                        HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ empty tab

@Composable
private fun EmptyTab(vm: AppViewModel) {
    val pull = rememberPullDownToCommand { vm.show(Overlay.Commands) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .nestedScroll(pull),
    ) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PillButton("New note", Lucide.SquarePen) { vm.createNote() }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, actions: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport)
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                actions.forEach { (label, action) -> PillButton(label, onClick = action) }
            }
        }
    }
}

/**
 * Pull down from the top of a note to open the command palette (Obsidian's default mobile Quick Action).
 * Fires once per drag when the content is already scrolled to the top.
 */
@Composable
fun rememberPullDownToCommand(onPull: () -> Unit): NestedScrollConnection {
    val currentOnPull = rememberUpdatedState(onPull)
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

/**
 * Both sidebars follow your finger: drag from the edge to pull one out, drag it back (or tap the dimmed
 * area) to put it away. Letting go settles it open or closed with a spring, based on position and speed.
 */
@Composable
private fun SidePanels(vm: AppViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = min(maxWidth * 0.86f, 420.dp)
        val widthPx = with(LocalDensity.current) { panelWidth.toPx() }
        val scope = rememberCoroutineScope()
        val spec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f)

        val left = remember { Animatable(0f) }
        val right = remember { Animatable(0f) }
        var leftDragTo by remember { mutableFloatStateOf(0f) }
        var rightDragTo by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(vm.leftOpen) { left.animateTo(if (vm.leftOpen) 1f else 0f, spec) }
        LaunchedEffect(vm.rightOpen) { right.animateTo(if (vm.rightOpen) 1f else 0f, spec) }

        // Velocity is in px/s; convert to "panel widths per second" for the spring.
        fun settle(anim: Animatable<Float, *>, velocityPx: Float, isLeft: Boolean) {
            val v = velocityPx / widthPx * (if (isLeft) 1f else -1f)
            val open = when {
                v > 1.2f -> true
                v < -1.2f -> false
                else -> anim.value > 0.5f
            }
            if (open) {
                if (isLeft) vm.openLeft() else vm.openRight()
            } else if (if (isLeft) vm.leftOpen else vm.rightOpen) {
                vm.closeSidebars()
            }
            scope.launch { anim.animateTo(if (open) 1f else 0f, spec, initialVelocity = v) }
        }

        val leftDrag = rememberDraggableState { d ->
            leftDragTo = (leftDragTo + d / widthPx).coerceIn(0f, 1f)
            scope.launch { left.snapTo(leftDragTo) }
        }
        val rightDrag = rememberDraggableState { d ->
            rightDragTo = (rightDragTo - d / widthPx).coerceIn(0f, 1f)
            scope.launch { right.snapTo(rightDragTo) }
        }
        val leftDragMod = Modifier.draggable(
            state = leftDrag,
            orientation = Orientation.Horizontal,
            onDragStarted = { leftDragTo = left.value },
            onDragStopped = { v -> settle(left, v, isLeft = true) },
        )
        val rightDragMod = Modifier.draggable(
            state = rightDrag,
            orientation = Orientation.Horizontal,
            onDragStarted = { rightDragTo = right.value },
            onDragStopped = { v -> settle(right, v, isLeft = false) },
        )

        val progress = maxOf(left.value, right.value)
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress }
                    .background(Color.Black.copy(alpha = 0.3f))
                    .then(if (left.value >= right.value) leftDragMod else rightDragMod)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        vm.closeSidebars()
                    },
            )
        } else {
            // Edge zones. Android reserves edge swipes for Back with gesture navigation, so these
            // ask the system to exclude them (Android allows at most a 200dp tall strip per edge).
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
                    .height(200.dp)
                    .systemGestureExclusion()
                    .then(leftDragMod),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
                    .height(200.dp)
                    .systemGestureExclusion()
                    .then(rightDragMod),
            )
        }

        if (left.value > 0f) {
            Panel(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(panelWidth)
                    .graphicsLayer { translationX = -(1f - left.value) * widthPx }
                    .then(leftDragMod),
            ) { LeftSidebar(vm) }
        }
        if (right.value > 0f) {
            Panel(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(panelWidth)
                    .graphicsLayer { translationX = (1f - right.value) * widthPx }
                    .then(rightDragMod),
            ) { RightSidebar(vm) }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxHeight(),
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) { content() }
    }
}
