package com.gozdor.slatenotes.data

data class SlateSettings(
    /** "system", "light" or "dark" */
    val theme: String = "system",
    val accent: Long = 0xFF5B6EE1,
    /** New tabs open in reading view instead of editing view. */
    val defaultReading: Boolean = false,
    val fontSize: Int = 16,
    val showInlineTitle: Boolean = true,
    val continueLists: Boolean = true,
    /** false = move to the vault's .trash folder, true = delete for good */
    val deletePermanently: Boolean = false,
    /** false = vault root, true = same folder as the open note */
    val newNotesInCurrentFolder: Boolean = false,
    val confirmDelete: Boolean = true,
)

/** Where each platform keeps small settings: SharedPreferences on Android, a properties file on Windows. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class Prefs(private val store: KeyValueStore) {

    // ---- settings ----

    fun settings(): SlateSettings {
        val d = SlateSettings()
        fun bool(k: String, def: Boolean) = store.get(k)?.toBooleanStrictOrNull() ?: def
        return SlateSettings(
            theme = store.get("s.theme") ?: d.theme,
            accent = store.get("s.accent")?.toLongOrNull() ?: d.accent,
            defaultReading = bool("s.defaultReading", d.defaultReading),
            fontSize = store.get("s.fontSize")?.toIntOrNull() ?: d.fontSize,
            showInlineTitle = bool("s.inlineTitle", d.showInlineTitle),
            continueLists = bool("s.continueLists", d.continueLists),
            deletePermanently = bool("s.deletePermanently", d.deletePermanently),
            newNotesInCurrentFolder = bool("s.newInCurrent", d.newNotesInCurrentFolder),
            confirmDelete = bool("s.confirmDelete", d.confirmDelete),
        )
    }

    fun saveSettings(s: SlateSettings) {
        store.put("s.theme", s.theme)
        store.put("s.accent", s.accent.toString())
        store.put("s.defaultReading", s.defaultReading.toString())
        store.put("s.fontSize", s.fontSize.toString())
        store.put("s.inlineTitle", s.showInlineTitle.toString())
        store.put("s.continueLists", s.continueLists.toString())
        store.put("s.deletePermanently", s.deletePermanently.toString())
        store.put("s.newInCurrent", s.newNotesInCurrentFolder.toString())
        store.put("s.confirmDelete", s.confirmDelete.toString())
    }

    // ---- vaults ----

    fun vaults(): List<String> = list("vaults")

    fun addVault(path: String) = putList("vaults", listOf(path) + vaults().filter { it != path })

    fun removeVault(path: String) {
        putList("vaults", vaults().filter { it != path })
        if (lastVault() == path) setLastVault(null)
    }

    fun lastVault(): String? = store.get("last")

    fun setLastVault(path: String?) = store.put("last", path)

    // ---- per-vault lists (paths relative to the vault root) ----

    fun recents(vault: String): List<String> = list("recent:$vault")

    fun addRecent(vault: String, rel: String) =
        putList("recent:$vault", (listOf(rel) + recents(vault).filter { it != rel }).take(30))

    fun remapRecents(vault: String, map: (String) -> String?) =
        putList("recent:$vault", recents(vault).mapNotNull(map).distinct())

    fun bookmarks(vault: String): List<String> = list("bookmarks:$vault")

    fun setBookmarks(vault: String, items: List<String>) = putList("bookmarks:$vault", items)

    /** Open tabs (absolute paths, "" for an empty tab) so the workspace comes back after a restart. */
    fun tabs(vault: String): List<String> {
        val raw = store.get("tabs:$vault") ?: return emptyList()
        return raw.split('\n')
    }

    fun saveTabs(vault: String, paths: List<String?>, active: Int) {
        store.put("tabs:$vault", paths.joinToString("\n") { it ?: "" })
        store.put("activeTab:$vault", active.toString())
    }

    fun activeTab(vault: String): Int = store.get("activeTab:$vault")?.toIntOrNull() ?: 0

    private fun list(key: String): List<String> =
        (store.get(key) ?: "").split('\n').filter { it.isNotBlank() }

    private fun putList(key: String, items: List<String>) = store.put(key, items.joinToString("\n"))
}
