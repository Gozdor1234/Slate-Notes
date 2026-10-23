package com.gozdor.slatenotes.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gozdor.slatenotes.ui.screens.BrowserScreen
import com.gozdor.slatenotes.ui.screens.EditorScreen
import com.gozdor.slatenotes.ui.screens.HomeScreen
import com.gozdor.slatenotes.ui.screens.PermissionScreen
import com.gozdor.slatenotes.ui.screens.SearchScreen
import com.gozdor.slatenotes.ui.screens.VaultsScreen

object StoragePermission {
    fun granted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
}

@Composable
fun SlateApp(vm: AppViewModel = viewModel()) {
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
        BackHandler(enabled = vm.canGoBack) { vm.back() }

        when (val s = vm.screen) {
            Screen.Vaults -> VaultsScreen(vm)
            is Screen.Browser -> BrowserScreen(vm, s.path)
            Screen.Home -> HomeScreen(vm)
            is Screen.Editor -> key(s.path) { EditorScreen(vm, s.path) }
            is Screen.Search -> SearchScreen(vm, s.initial)
        }
    }
}
