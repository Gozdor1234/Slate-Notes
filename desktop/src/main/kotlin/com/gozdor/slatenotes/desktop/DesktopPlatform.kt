package com.gozdor.slatenotes.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.gozdor.slatenotes.data.KeyValueStore
import com.gozdor.slatenotes.ui.Platform
import org.json.JSONObject
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.Properties
import javax.swing.JFileChooser
import org.jetbrains.skia.Image as SkiaImage

class DesktopPlatform(private val onToast: (String) -> Unit) : Platform {
    /** Settings live in %APPDATA%\Slate Notes\settings.properties. */
    private val dir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Slate Notes").apply { mkdirs() }
    private val file = File(dir, "settings.properties")
    private val props = Properties().apply {
        if (file.isFile) runCatching { file.inputStream().use { load(it) } }
    }

    override val store = object : KeyValueStore {
        override fun get(key: String): String? = props.getProperty(key)
        override fun put(key: String, value: String?) {
            if (value == null) props.remove(key) else props.setProperty(key, value)
            runCatching { file.outputStream().use { props.store(it, "Slate Notes settings") } }
        }
    }

    override val storageRoot: File = File(System.getProperty("user.home"))

    // The Windows installer's launcher sets this; running from Gradle it's absent.
    override val appVersion: String = System.getProperty("jpackage.app-version") ?: "dev"

    override fun toast(message: String) = onToast(message)

    override fun openUrl(url: String): Boolean = runCatching {
        Desktop.getDesktop().browse(URI(url))
        true
    }.getOrDefault(false)

    override fun loadImage(file: File, maxSize: Int): ImageBitmap? =
        runCatching { SkiaImage.makeFromEncoded(file.readBytes()).toComposeImageBitmap() }.getOrNull()

    /** Vaults Obsidian knows about on this PC, from its own list in %APPDATA%\obsidian\obsidian.json. */
    fun obsidianVaults(): List<File> = runCatching {
        val appData = System.getenv("APPDATA") ?: return emptyList()
        val f = File(appData, "obsidian/obsidian.json")
        if (!f.isFile) return emptyList()
        val vaults = JSONObject(f.readText()).optJSONObject("vaults") ?: return emptyList()
        vaults.keys().asSequence()
            .mapNotNull { vaults.optJSONObject(it)?.optString("path")?.takeIf { p -> p.isNotBlank() } }
            .map { File(it) }
            .filter { it.isDirectory }
            .toList()
    }.getOrDefault(emptyList())

    fun chooseFolder(title: String): File? {
        val fc = JFileChooser(storageRoot).apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        return if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) fc.selectedFile else null
    }
}
