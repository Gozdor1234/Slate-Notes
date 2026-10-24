package com.gozdor.slatenotes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gozdor.slatenotes.data.IndexedNote
import com.gozdor.slatenotes.data.Prefs
import com.gozdor.slatenotes.data.SlateSettings
import com.gozdor.slatenotes.data.TreeNode
import com.gozdor.slatenotes.data.Vault
import com.gozdor.slatenotes.data.VaultIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Full-screen layers drawn above the workspace. */
sealed interface Overlay {
    data object Vaults : Overlay
    data class Browser(val path: String) : Overlay
    data object Settings : Overlay
    data object QuickSwitcher : Overlay
    data object Commands : Overlay
    data object Tabs : Overlay
}

/** A tab with its own back/forward history. A null entry is an empty "new tab". */
data class Tab(val id: Long, val history: List<String?>, val pos: Int) {
    val path: String? get() = history.getOrNull(pos)
}

data class JumpRequest(val path: String, val offset: Int)

/** All app state and actions, shared by the Android and Windows apps. */
class AppViewModel(val platform: Platform, private val scope: CoroutineScope) {
    private val prefs = Prefs(platform.store)

    var settings by mutableStateOf(prefs.settings()); private set

    var vaultPaths by mutableStateOf(prefs.vaults()); private set
    var vault by mutableStateOf<Vault?>(null); private set
    var tree by mutableStateOf<List<TreeNode>>(emptyList()); private set
    var index by mutableStateOf(VaultIndex.EMPTY); private set
    var indexing by mutableStateOf(false); private set
    var recents by mutableStateOf<List<String>>(emptyList()); private set
    var bookmarks by mutableStateOf<List<String>>(emptyList()); private set

    val tabs = mutableStateListOf<Tab>()
    var activeTab by mutableIntStateOf(0); private set
    private val readingByTab = mutableStateMapOf<Long, Boolean>()
    private var nextTabId = 1L

    val overlays = mutableStateListOf<Overlay>()
    val overlay: Overlay? get() = overlays.lastOrNull()

    var leftOpen by mutableStateOf(false)
    var rightOpen by mutableStateOf(false)
    var leftPanel by mutableIntStateOf(0)
    var rightPanel by mutableIntStateOf(0)
    var searchQuery by mutableStateOf("")

    /** Help sheet: null = closed, -1 = topic list, n = topic n. */
    var helpTopic by mutableStateOf<Int?>(null)

    // One-shot requests the open note view picks up.
    var jump by mutableStateOf<JumpRequest?>(null)
    var renameRequest by mutableLongStateOf(0L); private set
    var insertRequest by mutableStateOf<String?>(null)
    var justCreated: String? = null

    private var started = false

    val storageRoot: File get() = platform.storageRoot

    // ---------------------------------------------------------------- startup

    fun start() {
        if (started) return
        started = true
        val last = prefs.lastVault()
        if (last != null && File(last).isDirectory) openVault(File(last))
    }

    fun updateSettings(transform: (SlateSettings) -> SlateSettings) {
        settings = transform(settings)
        prefs.saveSettings(settings)
    }

    // ---------------------------------------------------------------- overlays, sidebars, back

    /**
     * Phone: sidebars are drawers that close when something else opens, and only one shows at a time.
     * Windows: sidebars are docked panels that stay put until toggled.
     */
    var dockedSidebars = false

    fun closeDrawers() {
        if (!dockedSidebars) closeSidebars()
    }

    fun show(o: Overlay) {
        closeDrawers()
        overlays.add(o)
    }

    fun closeOverlay() {
        if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex)
    }

    fun closeSidebars() {
        leftOpen = false
        rightOpen = false
    }

    fun openLeft(panel: Int? = null) {
        panel?.let { leftPanel = it }
        if (!dockedSidebars) rightOpen = false
        leftOpen = true
    }

    fun openRight(panel: Int? = null) {
        panel?.let { rightPanel = it }
        if (!dockedSidebars) leftOpen = false
        rightOpen = true
    }

    /** True when the system back button has something to do inside the app. */
    val backHandled: Boolean
        get() = helpTopic != null || (!dockedSidebars && (leftOpen || rightOpen)) || overlays.isNotEmpty() || canGoBack

    fun handleBack() {
        when {
            helpTopic != null -> helpTopic = if (helpTopic == -1 || helpFromSettings) null else -1
            !dockedSidebars && (leftOpen || rightOpen) -> closeSidebars()
            overlays.isNotEmpty() -> closeOverlay()
            canGoBack -> goBack()
        }
    }

    /** When help was opened straight to a topic from Settings, back closes it instead of showing the list. */
    var helpFromSettings = false

    fun openHelp(topic: Int? = null) {
        helpFromSettings = topic != null
        helpTopic = topic ?: -1
    }

    // ---------------------------------------------------------------- vaults

    fun openVault(dir: File) {
        val v = Vault(dir)
        val key = dir.absolutePath
        vault = v
        tree = emptyList()
        index = VaultIndex.EMPTY
        prefs.addVault(key)
        prefs.setLastVault(key)
        vaultPaths = prefs.vaults()
        recents = prefs.recents(key)
        bookmarks = prefs.bookmarks(key)

        tabs.clear()
        readingByTab.clear()
        prefs.tabs(key).forEach { p ->
            val path = p.ifEmpty { null }?.takeIf { File(it).isFile }
            tabs.add(Tab(nextTabId++, listOf(path), 0))
        }
        if (tabs.isEmpty()) tabs.add(Tab(nextTabId++, listOf(null), 0))
        activeTab = prefs.activeTab(key).coerceIn(0, tabs.lastIndex)

        overlays.clear()
        closeDrawers()
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
        scope.launch {
            indexing = true
            val result = withContext(Dispatchers.IO) { runCatching { v.buildTree() to v.buildIndex() } }
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

    // ---------------------------------------------------------------- tabs and history

    val activeTabId: Long get() = tabs.getOrNull(activeTab)?.id ?: -1L
    val activePath: String? get() = tabs.getOrNull(activeTab)?.path
    val activeNote: IndexedNote? get() = activePath?.let { index.notes[it] }

    val canGoBack: Boolean get() = (tabs.getOrNull(activeTab)?.pos ?: 0) > 0
    val canGoForward: Boolean
        get() = tabs.getOrNull(activeTab)?.let { it.pos < it.history.lastIndex } ?: false

    fun isReading(tabId: Long): Boolean = readingByTab[tabId] ?: settings.defaultReading

    fun setReading(tabId: Long, reading: Boolean) {
        readingByTab[tabId] = reading
    }

    fun openNote(f: File, newTab: Boolean = false) {
        val v = vault ?: return
        val p = f.absolutePath
        prefs.addRecent(v.root.absolutePath, v.rel(f))
        recents = prefs.recents(v.root.absolutePath)

        if (newTab || tabs.isEmpty()) {
            tabs.add(Tab(nextTabId++, listOf(p), 0))
            activeTab = tabs.lastIndex
        } else {
            val t = tabs[activeTab]
            if (t.path != p) {
                // An empty tab is replaced rather than kept in history.
                val base = if (t.path == null) t.history.take(t.pos) else t.history.take(t.pos + 1)
                val h = (base + p).takeLast(50)
                tabs[activeTab] = t.copy(history = h, pos = h.lastIndex)
            }
        }
        overlays.clear()
        closeDrawers()
        saveTabs()
    }

    fun goBack() {
        val t = tabs.getOrNull(activeTab) ?: return
        if (t.pos > 0) tabs[activeTab] = t.copy(pos = t.pos - 1)
        saveTabs()
    }

    fun goForward() {
        val t = tabs.getOrNull(activeTab) ?: return
        if (t.pos < t.history.lastIndex) tabs[activeTab] = t.copy(pos = t.pos + 1)
        saveTabs()
    }

    fun newTab() {
        tabs.add(Tab(nextTabId++, listOf(null), 0))
        activeTab = tabs.lastIndex
        overlays.clear()
        closeDrawers()
        saveTabs()
    }

    fun switchTab(i: Int) {
        if (i in tabs.indices) activeTab = i
        overlays.clear()
        saveTabs()
    }

    private val closedTabs = ArrayDeque<String>()

    fun closeTab(i: Int) {
        if (i !in tabs.indices) return
        tabs[i].path?.let { closedTabs.addLast(it) }
        readingByTab.remove(tabs[i].id)
        tabs.removeAt(i)
        if (tabs.isEmpty()) tabs.add(Tab(nextTabId++, listOf(null), 0))
        activeTab = when {
            i < activeTab -> activeTab - 1
            else -> activeTab.coerceAtMost(tabs.lastIndex)
        }
        saveTabs()
    }

    /** Ctrl+Shift+T: reopen the most recently closed note that still exists. */
    fun reopenClosedTab() {
        while (closedTabs.isNotEmpty()) {
            val f = File(closedTabs.removeLast())
            if (f.isFile) {
                openNote(f, newTab = true)
                return
            }
        }
    }

    fun closeAllTabs() {
        tabs.clear()
        readingByTab.clear()
        tabs.add(Tab(nextTabId++, listOf(null), 0))
        activeTab = 0
        saveTabs()
    }

    private fun saveTabs() {
        val v = vault ?: return
        prefs.saveTabs(v.root.absolutePath, tabs.map { it.path }, activeTab)
    }

    /** After a rename or delete, point every tab, history entry, recent and bookmark at the new path (or nothing). */
    private fun remap(oldAbs: String, newAbs: String?) {
        val v = vault ?: return
        fun map(p: String?): String? {
            if (p == null) return null
            return when {
                p == oldAbs -> newAbs
                p.startsWith("$oldAbs/") -> newAbs?.let { it + p.removePrefix(oldAbs) }
                else -> p
            }
        }
        for (i in tabs.indices) {
            val t = tabs[i]
            val h = t.history.map { map(it) }
            if (h != t.history) tabs[i] = t.copy(history = h)
        }
        val root = v.root.absolutePath
        val oldRel = v.rel(File(oldAbs))
        val newRel = newAbs?.let { v.rel(File(it)) }
        fun mapRel(r: String): String? = when {
            r == oldRel -> newRel
            r.startsWith("$oldRel/") -> newRel?.let { it + r.removePrefix(oldRel) }
            else -> r
        }
        prefs.remapRecents(root) { mapRel(it) }
        recents = prefs.recents(root)
        prefs.setBookmarks(root, bookmarks.mapNotNull { mapRel(it) }.distinct())
        bookmarks = prefs.bookmarks(root)
        saveTabs()
    }

    // ---------------------------------------------------------------- notes

    /** Follows a [[wikilink]]. Like Obsidian, a link to a note that doesn't exist yet creates it. */
    fun openLink(target: String, newTab: Boolean = false) {
        val v = vault ?: return
        val hit = index.resolve(target)
        if (hit != null && hit.file.exists()) {
            openNote(hit.file, newTab)
            return
        }
        createNamed(target, newTab)
    }

    /** Creates a note from a typed name or link target ("Folder/Name" puts it in that folder). */
    fun createNamed(name: String, newTab: Boolean = false) {
        val v = vault ?: return
        scope.launch {
            val created = withContext(Dispatchers.IO) {
                runCatching {
                    val parts = name.trim().trimStart('/').split('/')
                    val dir = if (parts.size > 1) File(v.root, parts.dropLast(1).joinToString("/")) else defaultNewNoteDir(v)
                    v.createNote(dir, parts.last())
                }
            }
            created.onSuccess {
                index = index.withNote(v.indexOne(it, ""))
                refresh()
                openNote(it, newTab)
            }.onFailure { toast("Couldn't create note: ${it.message}") }
        }
    }

    private fun defaultNewNoteDir(v: Vault): File =
        if (settings.newNotesInCurrentFolder) activePath?.let { File(it).parentFile } ?: v.root else v.root

    fun createNote(dir: File? = null) {
        val v = vault ?: return
        scope.launch {
            val target = dir ?: defaultNewNoteDir(v)
            val r = withContext(Dispatchers.IO) { runCatching { v.createNote(target) } }
            if (target != v.root) expandedFolders[target.absolutePath] = true
            r.onSuccess {
                justCreated = it.absolutePath
                index = index.withNote(v.indexOne(it, ""))
                refresh()
                openNote(it)
                activeTabId.let { id -> setReading(id, false) }
            }.onFailure { toast("Couldn't create note: ${it.message}") }
        }
    }

    fun createCanvas(dir: File? = null) {
        val v = vault ?: return
        scope.launch {
            val target = dir ?: defaultNewNoteDir(v)
            val r = withContext(Dispatchers.IO) { runCatching { v.createCanvas(target) } }
            if (target != v.root) expandedFolders[target.absolutePath] = true
            r.onSuccess {
                refresh()
                openNote(it)
            }.onFailure { toast("Couldn't create canvas: ${it.message}") }
        }
    }

    /** Saves a non-note file (like a canvas) without adding it to the note index. */
    fun saveFile(path: String, text: String) {
        val v = vault ?: return
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.write(File(path), text) } }
            r.onFailure { toast("Save failed: ${it.message}") }
        }
    }

    fun createFolder(parent: File, name: String) {
        val v = vault ?: return
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.createFolder(parent, name) } }
            r.onFailure { toast(it.message ?: "Couldn't create folder") }
            if (parent != v.root) expandedFolders[parent.absolutePath] = true
            refresh()
        }
    }

    fun openTag(tag: String) {
        searchQuery = "tag:#$tag"
        openLeft(1)
    }

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
        scope.launch {
            val f = File(path)
            val ok = withContext(Dispatchers.IO) { runCatching { v.write(f, text) } }
            ok.onSuccess { index = index.withNote(v.indexOne(f, text)) }
                .onFailure { toast("Save failed: ${it.message}") }
        }
    }

    fun requestRename() {
        activeTabId.let { setReading(it, false) }
        renameRequest = System.nanoTime()
    }

    /** Saves the current text first, then renames, so the editor never writes to the old path. */
    fun renameNote(path: String, newName: String, text: String, onFail: () -> Unit) {
        val v = vault ?: return
        scope.launch {
            val old = File(path)
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    v.write(old, text)
                    v.rename(old, newName)
                }
            }
            r.onSuccess { nf ->
                remap(old.absolutePath, nf.absolutePath)
                refresh()
            }.onFailure {
                toast(it.message ?: "Rename failed")
                onFail()
            }
        }
    }

    fun renameFile(f: File, newName: String) {
        val v = vault ?: return
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.rename(f, newName) } }
            r.onSuccess { nf -> remap(f.absolutePath, nf.absolutePath) }
                .onFailure { toast(it.message ?: "Rename failed") }
            refresh()
        }
    }

    /** Folder expansion in the file explorer, kept here so it survives closing the sidebar. */
    val expandedFolders = mutableStateMapOf<String, Boolean>()

    /** File waiting for the "Move to…" folder picker; null = picker closed. */
    var moveTarget by mutableStateOf<File?>(null)

    fun moveFile(f: File, destDir: File) {
        val v = vault ?: return
        if (f.parentFile?.absolutePath == destDir.absolutePath) return
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { v.move(f, destDir) } }
            r.onSuccess { nf ->
                if (f.isDirectory || nf.isDirectory) {
                    expandedFolders.remove(f.absolutePath)?.let { expandedFolders[nf.absolutePath] = it }
                }
                if (destDir != v.root) expandedFolders[destDir.absolutePath] = true
                remap(f.absolutePath, nf.absolutePath)
                toast("Moved to ${if (destDir == v.root) "vault folder" else destDir.name}")
            }.onFailure { toast(it.message ?: "Move failed") }
            refresh()
        }
    }

    fun delete(f: File) {
        val v = vault ?: return
        val permanent = settings.deletePermanently
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { if (permanent) v.deletePermanently(f) else v.moveToTrash(f) }
            }
            r.onSuccess {
                remap(f.absolutePath, null)
                toast(if (permanent) "Deleted \"${f.name}\"" else "Moved \"${f.name}\" to the vault's .trash folder")
            }.onFailure { toast(it.message ?: "Delete failed") }
            refresh()
        }
    }

    // ---------------------------------------------------------------- bookmarks

    fun isBookmarked(path: String): Boolean {
        val v = vault ?: return false
        return v.rel(File(path)) in bookmarks
    }

    fun toggleBookmark(path: String) {
        val v = vault ?: return
        val rel = v.rel(File(path))
        val list = if (rel in bookmarks) bookmarks - rel else bookmarks + rel
        prefs.setBookmarks(v.root.absolutePath, list)
        bookmarks = list
        toast(if (rel in list) "Bookmarked" else "Bookmark removed")
    }

    fun toast(msg: String) = platform.toast(msg)
}
