package com.gozdor.slatenotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsTest {
    private class MapStore(val map: MutableMap<String, String> = mutableMapOf()) : KeyValueStore {
        override fun get(key: String) = map[key]
        override fun put(key: String, value: String?) {
            if (value == null) map.remove(key) else map[key] = value
        }
    }

    @Test
    fun settingsRoundTrip() {
        val p = Prefs(MapStore())
        val s = SlateSettings(theme = "dark", accent = 0xFF3E9B4F, fontSize = 20, deletePermanently = true)
        p.saveSettings(s)
        assertEquals(s, p.settings())
    }

    @Test
    fun readsValuesSavedByOlderAndroidBuilds() {
        // Android used to store typed values; its store hands them back as strings like these.
        val store = MapStore(mutableMapOf("s.accent" to 4284182241L.toString(), "s.fontSize" to "18", "s.confirmDelete" to "false"))
        val s = Prefs(store).settings()
        assertEquals(4284182241L, s.accent)
        assertEquals(18, s.fontSize)
        assertEquals(false, s.confirmDelete)
    }

    @Test
    fun badValuesFallBackToDefaults() {
        val s = Prefs(MapStore(mutableMapOf("s.fontSize" to "huge", "s.continueLists" to "maybe"))).settings()
        assertEquals(SlateSettings().fontSize, s.fontSize)
        assertEquals(SlateSettings().continueLists, s.continueLists)
    }

    @Test
    fun tabsAndVaultLists() {
        val p = Prefs(MapStore())
        p.addVault("/a")
        p.addVault("/b")
        p.addVault("/a")
        assertEquals(listOf("/a", "/b"), p.vaults())
        p.saveTabs("/a", listOf("/a/x.md", null), 1)
        assertEquals(listOf("/a/x.md", ""), p.tabs("/a"))
        assertEquals(1, p.activeTab("/a"))
        p.setLastVault("/a")
        p.removeVault("/a")
        assertEquals(null, p.lastVault())
    }
}
