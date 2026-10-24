package com.gozdor.slatenotes.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Overlay
import com.gozdor.slatenotes.ui.theme.SlateTheme
import kotlinx.coroutines.delay
import java.io.File
import javax.swing.UIManager

/** The app icon, drawn the same way as the Android launcher icon. */
private val SlateIcon: ImageVector by lazy {
    ImageVector.Builder("slate", 108.dp, 108.dp, 108f, 108f).apply {
        addPath(addPathNodes("M24,6h60a18,18 0,0 1,18 18v60a18,18 0,0 1,-18 18h-60a18,18 0,0 1,-18 -18v-60a18,18 0,0 1,18 -18z"), fill = SolidColor(Color(0xFF22262E)))
        group(rotate = -8f, pivotX = 54f, pivotY = 54f) {
            addPath(addPathNodes("M36,30h36a6,6 0,0 1,6 6v36a6,6 0,0 1,-6 6h-36a6,6 0,0 1,-6 -6v-36a6,6 0,0 1,6 -6z"), fill = SolidColor(Color(0xFF8FA8FF)))
            addPath(addPathNodes("M38,34h32a4,4 0,0 1,4 4v32a4,4 0,0 1,-4 4h-32a4,4 0,0 1,-4 -4v-32a4,4 0,0 1,4 -4z"), fill = SolidColor(Color(0xFF22262E)))
            addPath(addPathNodes("M41,43h26v3.5h-26z M41,52h20v3.5h-20z M41,61h23v3.5h-23z"), fill = SolidColor(Color(0xFFDCE3FF)))
        }
    }.build()
}

fun main() {
    // Native Windows look for the folder picker.
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

    application {
        var toast by remember { mutableStateOf<String?>(null) }
        val platform = remember { DesktopPlatform { toast = it } }
        val scope = rememberCoroutineScope()
        val vm = remember {
            AppViewModel(platform, scope).apply {
                dockedSidebars = true
                leftOpen = true
                rightOpen = true
            }
        }
        LaunchedEffect(Unit) { vm.start() }
        LaunchedEffect(toast) {
            if (toast != null) {
                delay(2500)
                toast = null
            }
        }

        val title = listOfNotNull(
            vm.activePath?.let { File(it).nameWithoutExtension },
            vm.vault?.name,
            "Slate Notes",
        ).joinToString(" - ")

        Window(
            onCloseRequest = ::exitApplication,
            title = title,
            icon = rememberVectorPainter(SlateIcon),
            state = rememberWindowState(width = 1280.dp, height = 820.dp),
            onPreviewKeyEvent = { handleHotkey(vm, it) },
        ) {
            // Re-read the vault whenever the window comes back into focus, like the phone app does on resume.
            val focused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(focused) { if (focused && vm.vault != null) vm.refresh() }
            SlateTheme(vm.settings) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        DesktopApp(vm, platform)
                        toast?.let {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.inverseSurface,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 48.dp),
                            ) {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun showOnce(vm: AppViewModel, o: Overlay) {
    if (vm.overlay != o) vm.show(o)
}

/** Obsidian's default Windows hotkeys. Returns true when the key was used. */
private fun handleHotkey(vm: AppViewModel, e: KeyEvent): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    if (e.key == Key.Escape) {
        when {
            vm.moveTarget != null -> vm.moveTarget = null
            vm.helpTopic != null -> vm.helpTopic = null
            vm.overlay != null && vm.vault != null -> vm.closeOverlay()
            else -> return false
        }
        return true
    }
    if (!e.isCtrlPressed || vm.vault == null) return false
    val shift = e.isShiftPressed
    when {
        e.isAltPressed && e.key == Key.DirectionLeft -> vm.goBack()
        e.isAltPressed && e.key == Key.DirectionRight -> vm.goForward()
        e.key == Key.O -> showOnce(vm, Overlay.QuickSwitcher)
        e.key == Key.P -> showOnce(vm, Overlay.Commands)
        e.key == Key.N && !shift -> vm.createNote()
        e.key == Key.T && shift -> vm.reopenClosedTab()
        e.key == Key.T -> vm.newTab()
        e.key == Key.W -> vm.closeTab(vm.activeTab)
        e.key == Key.F && shift -> vm.openLeft(1)
        e.key == Key.Comma -> showOnce(vm, Overlay.Settings)
        e.key == Key.E -> toggleReading(vm)
        e.key == Key.Tab && vm.tabs.isNotEmpty() -> {
            val n = vm.tabs.size
            vm.switchTab((vm.activeTab + if (shift) n - 1 else 1) % n)
        }
        else -> return false
    }
    return true
}

fun toggleReading(vm: AppViewModel) {
    val p = vm.activePath ?: return
    if (p.endsWith(".canvas", ignoreCase = true)) return
    vm.setReading(vm.activeTabId, !vm.isReading(vm.activeTabId))
}
