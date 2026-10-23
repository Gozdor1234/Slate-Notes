package com.gozdor.slatenotes.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gozdor.slatenotes.ui.screens.BrowserScreen
import com.gozdor.slatenotes.ui.screens.PermissionScreen
import com.gozdor.slatenotes.ui.screens.VaultsScreen
import com.gozdor.slatenotes.ui.settings.HelpSheet
import com.gozdor.slatenotes.ui.settings.SettingsScreen
import com.gozdor.slatenotes.ui.workspace.CommandPalette
import com.gozdor.slatenotes.ui.workspace.MoveDialog
import com.gozdor.slatenotes.ui.workspace.QuickSwitcher
import com.gozdor.slatenotes.ui.workspace.TabSwitcher
import com.gozdor.slatenotes.ui.workspace.WorkspaceScreen

object StoragePermission {
    fun granted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
}

@Composable
fun SlateApp(vm: AppViewModel) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(StoragePermission.granted(context)) }
    var resumedOnce by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = StoragePermission.granted(context)
        // Pick up edits made by other apps (Obsidian, sync tools) while we were in the background.
        if (hasPermission && resumedOnce) vm.onResume()
        resumedOnce = true
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!hasPermission) {
            PermissionScreen(onCheck = { hasPermission = StoragePermission.granted(context) })
            return@Surface
        }
        LaunchedEffect(Unit) { vm.start() }
        BackHandler(enabled = vm.backHandled) { vm.handleBack() }

        // The workspace stays composed under overlays so open notes keep their state.
        if (vm.vault != null) WorkspaceScreen(vm)

        // Pages slide up and fade in, and fade back out when closed.
        AnimatedContent(
            targetState = vm.overlay,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(280)) { it / 14 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 20 })
            },
            modifier = Modifier.fillMaxSize(),
            label = "overlay",
        ) { o ->
            when (o) {
                Overlay.Vaults -> VaultsScreen(vm)
                is Overlay.Browser -> BrowserScreen(vm, o.path)
                Overlay.Settings -> SettingsScreen(vm)
                Overlay.QuickSwitcher -> QuickSwitcher(vm)
                Overlay.Commands -> CommandPalette(vm)
                Overlay.Tabs -> TabSwitcher(vm)
                null -> if (vm.vault == null) VaultsScreen(vm)
            }
        }

        MoveDialog(vm)
        HelpSheet(vm)
    }
}
