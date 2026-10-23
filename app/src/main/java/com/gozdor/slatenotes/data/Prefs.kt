package com.gozdor.slatenotes.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("slate", Context.MODE_PRIVATE)

    fun vaults(): List<String> = list("vaults")

    fun addVault(path: String) = putList("vaults", listOf(path) + vaults().filter { it != path })

    fun removeVault(path: String) {
        putList("vaults", vaults().filter { it != path })
        if (lastVault() == path) setLastVault(null)
    }

    fun lastVault(): String? = sp.getString("last", null)

    fun setLastVault(path: String?) = sp.edit().putString("last", path).apply()

    /** Recently opened notes per vault, stored as paths relative to the vault root. */
    fun recents(vault: String): List<String> = list("recent:$vault")

    fun addRecent(vault: String, rel: String) =
        putList("recent:$vault", (listOf(rel) + recents(vault).filter { it != rel }).take(30))

    fun replaceRecent(vault: String, oldRel: String, newRel: String?) =
        putList("recent:$vault", recents(vault).mapNotNull { if (it == oldRel) newRel else it }.distinct())

    private fun list(key: String): List<String> =
        (sp.getString(key, "") ?: "").split('\n').filter { it.isNotBlank() }

    private fun putList(key: String, items: List<String>) =
        sp.edit().putString(key, items.joinToString("\n")).apply()
}
