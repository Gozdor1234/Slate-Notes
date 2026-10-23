package com.gozdor.slatenotes.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultsScreen(vm: AppViewModel) {
    var detected by remember { mutableStateOf<List<File>?>(null) }
    var forget by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { detected = runCatching { vm.detectVaults() }.getOrDefault(emptyList()) }
    val cs = MaterialTheme.colorScheme
    val saved = vm.vaultPaths
    val extra = detected.orEmpty().filter { it.absolutePath !in saved }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vaults") },
                navigationIcon = {
                    if (vm.canGoBack) {
                        IconButton(onClick = { vm.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "A vault is any folder of Markdown notes. You can open an existing Obsidian vault, " +
                            "and both apps will see the same files.",
                        color = cs.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.navigate(Screen.Browser(vm.storageRoot.absolutePath)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open or create a vault folder")
                    }
                }
            }
            if (saved.isNotEmpty()) {
                item { SectionLabel("Your vaults") }
                items(saved, key = { "s:$it" }) { p ->
                    VaultRow(
                        File(p),
                        current = vm.vault?.root?.absolutePath == p,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (File(p).isDirectory) vm.openVault(File(p)) else vm.toast("That folder no longer exists")
                            },
                            onLongClick = { forget = p },
                        ),
                    )
                }
            }
            item { SectionLabel("Obsidian vaults found on this phone") }
            when {
                detected == null -> item { HintRow("Looking…") }
                extra.isEmpty() -> item { HintRow("None found (besides the ones above).") }
                else -> items(extra, key = { "d:${it.absolutePath}" }) { f ->
                    VaultRow(f, current = false, modifier = Modifier.clickable { vm.openVault(f) })
                }
            }
        }
    }

    forget?.let { p ->
        ConfirmDialog(
            title = "Remove from list?",
            message = "This only removes \"${File(p).name}\" from Slate's vault list. No files are deleted.",
            confirm = "Remove",
            onDismiss = { forget = null },
        ) {
            forget = null
            vm.forgetVault(p)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun HintRow(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun VaultRow(dir: File, current: Boolean, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Folder, null, tint = cs.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                dir.name + if (current) "  (open)" else "",
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                dir.absolutePath,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Simple folder browser for picking or creating a vault folder anywhere in phone storage. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: AppViewModel, path: String) {
    var current by remember(path) { mutableStateOf(File(path)) }
    var dirs by remember { mutableStateOf<List<File>>(emptyList()) }
    var newFolder by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val root = vm.storageRoot
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(current, reload) {
        dirs = withContext(Dispatchers.IO) {
            (current.listFiles() ?: emptyArray())
                .filter { it.isDirectory && !it.name.startsWith(".") }
                .sortedBy { it.name.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Choose vault folder")
                        Text(
                            current.absolutePath.removePrefix(root.absolutePath).ifEmpty { "/" },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { newFolder = true }) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = cs.surfaceContainer) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { current.parentFile?.let { current = it } },
                        enabled = current.absolutePath != root.absolutePath,
                    ) {
                        Icon(Icons.Default.ArrowUpward, null)
                    }
                    Button(
                        onClick = { vm.openVault(current) },
                        enabled = current.absolutePath != root.absolutePath,
                        modifier = Modifier.weight(1f),
                    ) { Text("Use \"${current.name}\" as vault", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (dirs.isEmpty()) item { HintRow("No folders here. Tap the folder+ icon to create one.") }
            items(dirs, key = { it.absolutePath }) { d ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { current = d }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Folder, null, tint = cs.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(d.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (File(d, ".obsidian").isDirectory) {
                        Spacer(Modifier.width(8.dp))
                        Text("vault", color = cs.primary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                HorizontalDivider(color = cs.outlineVariant)
            }
        }
    }

    if (newFolder) {
        NameDialog("New folder", "My Vault", "Create", { newFolder = false }) { name ->
            newFolder = false
            val f = File(current, name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim())
            if (f.exists() || f.mkdirs()) {
                current = f
                reload++
            } else {
                vm.toast("Couldn't create folder")
            }
        }
    }
}

@Composable
fun PermissionScreen(onCheck: () -> Unit) {
    val context = LocalContext.current
    val legacy = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onCheck() }
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.FolderOpen, null, tint = cs.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Storage access", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Slate keeps your notes as plain Markdown files in a folder you choose, so other apps " +
                "(Obsidian, Syncthing, a file manager) can use the same files. Android needs " +
                "\"All files access\" for that.",
            textAlign = TextAlign.Center,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            } else {
                legacy.launch(
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    )
                )
            }
        }) { Text("Grant access") }
    }
}
