package com.gozdor.slatenotes.ui

import android.app.Application
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gozdor.slatenotes.data.IndexedNote
import com.gozdor.slatenotes.data.Prefs
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.data.Vault
import com.gozdor.slatenotes.data.VaultIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Screen {
    data object Vaults : Screen
    data class Browser(val path: String) : Screen
    data object Home : Screen
    data class Editor(val path: String) : Screen
    data class Search(val initial: String = "") : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var vaultPaths by mutableStateOf(prefs.vaults()); private set
    var vault by mutableStateOf<Vault?>(null); private set
    var tree by mutableStateOf<List<TreeNode>>(emptyList()); private set
    var index by mutableStateOf(VaultIndex.EMPTY); private set
    var indexing by mutableStateOf(false); private set
    var recents by mutableStateOf<List<String>>(emptyList()); private set

    /** A note just created from the UI: its editor opens with the title selected for naming. */
    var justCreated: String? = null

    private val stack = mutableStateListOf<Screen>()
    val screen: Screen get() = stack.lastOrNull() ?: Screen.Vaults
    val canGoBack: Boolean get() = stack.size > 1

    private var started = false

    val storageRoot: File get() = Environment.getExternalStorageDirectory()

    fun start() {
        if (started) return
        started = true
        val last = prefs.lastVault()
        if (last != null && File(last).isDirectory) openVault(File(last))
        else {
            stack.clear()
            stack.add(Screen.Vaults)
        }
    }

    // ---- navigation ----

    fun navigate(s: Screen) {
        stack.add(s)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    private fun replaceTop(s: Screen) {
        if (stack.isEmpty()) stack.add(s) else stack[stack.lastIndex] = s
    }

    // ---- vaults ----

    fun openVault(dir: File) {
        val v = Vault(dir)
        vault = v
        tree = emptyList()
        index = VaultIndex.EMPTY
        prefs.addVault(dir.absolutePath)
        prefs.setLastVault(dir.absolutePath)
        vaultPaths = prefs.vaults()
        recents = prefs.recents(dir.absolutePath)
        stack.clear()
        stack.add(Screen.Home)
        refresh()
    }

    fun forgetVault(path: String) {
        prefs.removeVault(path)
        vaultPaths = prefs.vaults()
    }

    suspend fun detectVaults(): List<File> = withContext(Dispatchers.IO) {
        val root = storageRoot
        root.walkTopDown()
            .maxDepth(4)
            .onEnter { it == root || (!it.name.startsWith(".") && it.name != "Android") }
            .filter { it.isDirectory && File(it, ".obsidian").isDirectory }
            .toList()
    }

    fun refresh() {
        val v = vault ?: return
        viewModelScope.launch {
            indexing = true
            val result = withContext(Dispatchers.IO) {
                runCatching { v.buildTree() to v.buildIndex() }
            }
            if (vault === v) {
                result.onSuccess { (t, i) ->
                    tree = t
                    index = i
                }.onFailure { toast("Couldn't read vault: ${it.message}") }
            }
            indexing = false
        }
    }

    fun onResume() {
        if (vault != null) refresh()
    }

    // ---- notes ----

    fun openNote(f: File) {
        val v = vault ?: return
        prefs.addRecent(v.root.absolutePath, v.rel(f))
        recents = prefs.recents(v.root.absolutePath)
        navigate(Screen.Editor(f.absolutePath))
    }

    /** Follows a [[wikilink]]. Like Obsidian, a link to a note that doesn't exist yet creates it. */
    fun openLink(target: String) {
        val v = vault ?: return
        val hit = index.resolve(target)
        if (hit != null && hit.file.exists()) {
            openNote(hit.file)
            return
        }
        viewModelScope.launch {
            val created = withContext(Dispatchers.IO) {
                runCatching {
                    val parts = target.trim().trimStart('/').split('/')
                    val dir = if (parts.size > 1) File(v.root, parts.dropLast(1).joinToString("/")) else v.root
                    v.createNote(dir, parts.last())
                }
            }
            created.onSuccess {
                index = index.withNote(v.indexOne(it, ""))
                refresh()
                openNote(it)
            }.onFailure { toast("Couldn't create note: ${it.message}") }
        }
    }

    fun openTag(tag: String) = navigate(Screen.Search("tag:#$tag"))

    fun resolveAttachment(target: String): File? {
        val v = vault ?: return null
        return index.resolveAttachment(v.root, target)
    }

    fun recentNotes(): List<IndexedNote> {
        val v = vault ?: return emptyList()
        return recents.mapNotNull { index.notes[File(v.root, it).absolutePath] }
    }

    fun saveNote(path: String, text: String) {
        val v = vault ?: return
        viewModelScope.launch {
            val f = File(path)
            val ok = withContext(Dispatchers.IO) { runCatching { v.write(f, text) } }
            ok.onSuccess { index = index.withNote(v.indexOne(f, text)) }
                .onFailure { toast("Save failed: ${it.message}") }
        }
    }

    fun createNote(dir: File) {
        val v = vault ?: return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.createNote(dir) } }
            r.onSuccess {
                justCreated = it.absolutePath
                index = index.withNote(v.indexOne(it, ""))
                refresh()
                openNote(it)
            }.onFailure { toast("Couldn't create note: ${it.message}") }
        }
    }

    fun createFolder(parent: File, name: String) {
        val v = vault ?: return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.createFolder(parent, name) } }
            r.onFailure { toast(it.message ?: "Couldn't create folder") }
            refresh()
        }
    }

    /** Saves the current text first, then renames, so the editor never writes to the old path. */
    fun renameNote(path: String, newName: String, text: String, onFail: () -> Unit) {
        val v = vault ?: return
        viewModelScope.launch {
            val old = File(path)
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    v.write(old, text)
                    v.rename(old, newName)
                }
            }
            r.onSuccess { nf ->
                prefs.replaceRecent(v.root.absolutePath, v.rel(old), v.rel(nf))
                recents = prefs.recents(v.root.absolutePath)
                if (screen == Screen.Editor(path)) replaceTop(Screen.Editor(nf.absolutePath))
                refresh()
            }.onFailure {
                toast(it.message ?: "Rename failed")
                onFail()
            }
        }
    }

    fun renameFile(f: File, newName: String) {
        val v = vault ?: return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.rename(f, newName) } }
            r.onSuccess { nf ->
                if (!f.isDirectory) {
                    prefs.replaceRecent(v.root.absolutePath, v.rel(f), v.rel(nf))
                    recents = prefs.recents(v.root.absolutePath)
                }
            }.onFailure { toast(it.message ?: "Rename failed") }
            refresh()
        }
    }

    fun delete(f: File, then: () -> Unit = {}) {
        val v = vault ?: return
        viewModelScope.launch {
            val rel = v.rel(f)
            val r = withContext(Dispatchers.IO) { runCatching { v.moveToTrash(f) } }
            r.onSuccess {
                prefs.replaceRecent(v.root.absolutePath, rel, null)
                recents = prefs.recents(v.root.absolutePath)
                toast("Moved \"${f.name}\" to the vault's .trash folder")
                then()
            }.onFailure { toast(it.message ?: "Delete failed") }
            refresh()
        }
    }

    fun toast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}
