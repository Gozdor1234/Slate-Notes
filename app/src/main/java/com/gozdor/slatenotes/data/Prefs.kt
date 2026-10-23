package com.gozdor.slatenotes.data

import android.content.Context

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

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("slate", Context.MODE_PRIVATE)

    // ---- settings ----

    fun settings(): SlateSettings {
        val d = SlateSettings()
        return SlateSettings(
            theme = sp.getString("s.theme", d.theme) ?: d.theme,
            accent = sp.getLong("s.accent", d.accent),
            defaultReading = sp.getBoolean("s.defaultReading", d.defaultReading),
            fontSize = sp.getInt("s.fontSize", d.fontSize),
            showInlineTitle = sp.getBoolean("s.inlineTitle", d.showInlineTitle),
            continueLists = sp.getBoolean("s.continueLists", d.continueLists),
            deletePermanently = sp.getBoolean("s.deletePermanently", d.deletePermanently),
            newNotesInCurrentFolder = sp.getBoolean("s.newInCurrent", d.newNotesInCurrentFolder),
            confirmDelete = sp.getBoolean("s.confirmDelete", d.confirmDelete),
        )
    }

    fun saveSettings(s: SlateSettings) {
        sp.edit()
            .putString("s.theme", s.theme)
            .putLong("s.accent", s.accent)
            .putBoolean("s.defaultReading", s.defaultReading)
            .putInt("s.fontSize", s.fontSize)
            .putBoolean("s.inlineTitle", s.showInlineTitle)
            .putBoolean("s.continueLists", s.continueLists)
            .putBoolean("s.deletePermanently", s.deletePermanently)
            .putBoolean("s.newInCurrent", s.newNotesInCurrentFolder)
            .putBoolean("s.confirmDelete", s.confirmDelete)
            .apply()
    }

    // ---- vaults ----

    fun vaults(): List<String> = list("vaults")

    fun addVault(path: String) = putList("vaults", listOf(path) + vaults().filter { it != path })

    fun removeVault(path: String) {
        putList("vaults", vaults().filter { it != path })
        if (lastVault() == path) setLastVault(null)
    }

    fun lastVault(): String? = sp.getString("last", null)

    fun setLastVault(path: String?) = sp.edit().putString("last", path).apply()

    // ---- per-vault lists (paths relative to the vault root) ----

    fun recents(vault: String): List<String> = list("recent:$vault")

    fun addRecent(vault: String, rel: String) =
        putList("recent:$vault", (listOf(rel) + recents(vault).filter { it != rel }).take(30))

    fun remapRecents(vault: String, map: (String) -> String?) =
        putList("recent:$vault", recents(vault).mapNotNull(map).distinct())

    fun bookmarks(vault: String): List<String> = list("bookmarks:$vault")

    fun setBookmarks(vault: String, items: List<String>) = putList("bookmarks:$vault", items)

    /** Open tabs (absolute paths, "" for an empty tab) so the workspace comes back after a restart. */
    fun tabs(vault: String): List<String> =
        (sp.getString("tabs:$vault", null) ?: return emptyList()).split('\n')

    fun saveTabs(vault: String, paths: List<String?>, active: Int) {
        sp.edit()
            .putString("tabs:$vault", paths.joinToString("\n") { it ?: "" })
            .putInt("activeTab:$vault", active)
            .apply()
    }

    fun activeTab(vault: String): Int = sp.getInt("activeTab:$vault", 0)

    private fun list(key: String): List<String> =
        (sp.getString(key, "") ?: "").split('\n').filter { it.isNotBlank() }

    private fun putList(key: String, items: List<String>) =
        sp.edit().putString(key, items.joinToString("\n")).apply()
}
