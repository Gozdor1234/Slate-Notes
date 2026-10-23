package com.gozdor.slatenotes.data

import java.io.File
import java.io.IOException

data class TreeNode(val file: File, val isDir: Boolean, val children: List<TreeNode>)

/** A vault is just a folder of Markdown files, the same layout Obsidian uses. */
class Vault(val root: File) {
    val name: String get() = root.name
    private val trash: File get() = File(root, ".trash")

    fun rel(f: File): String = f.absolutePath.removePrefix(root.absolutePath).trimStart('/')

    fun buildTree(dir: File = root): List<TreeNode> =
        (dir.listFiles() ?: emptyArray())
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            .map { if (it.isDirectory) TreeNode(it, true, buildTree(it)) else TreeNode(it, false, emptyList()) }

    fun buildIndex(): VaultIndex {
        val notes = HashMap<String, IndexedNote>()
        val attachments = HashMap<String, File>()
        root.walkTopDown()
            .onEnter { it == root || !it.name.startsWith(".") }
            .filter { it.isFile && !it.name.startsWith(".") }
            .forEach { f ->
                if (f.extension.equals("md", ignoreCase = true)) {
                    val text = if (f.length() < 4_000_000) runCatching { f.readText() }.getOrDefault("") else ""
                    notes[f.absolutePath] = IndexedNote.of(f, rel(f), text)
                } else {
                    attachments.putIfAbsent(f.name.lowercase(), f)
                }
            }
        return VaultIndex(notes, attachments)
    }

    fun indexOne(f: File, text: String): IndexedNote = IndexedNote.of(f, rel(f), text)

    fun createNote(dir: File, base: String = "Untitled", content: String = ""): File {
        dir.mkdirs()
        val f = uniqueFile(dir, sanitize(base).ifBlank { "Untitled" }, ".md")
        f.writeText(content)
        return f
    }

    fun createFolder(parent: File, name: String): File {
        val f = uniqueFile(parent, sanitize(name).ifBlank { "New folder" }, "")
        if (!f.mkdirs()) throw IOException("Couldn't create folder")
        return f
    }

    fun write(f: File, text: String) {
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, ".${f.name}.slate-tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) {
            f.writeText(text)
            tmp.delete()
        }
    }

    fun rename(f: File, newBase: String): File {
        val clean = sanitize(newBase)
        if (clean.isBlank()) throw IOException("Name can't be empty")
        val ext = if (f.isDirectory || f.extension.isEmpty()) "" else "." + f.extension
        val target = File(f.parentFile, clean + ext)
        if (target.absolutePath == f.absolutePath) return f
        val caseOnly = target.absolutePath.equals(f.absolutePath, ignoreCase = true)
        if (!caseOnly && target.exists()) throw IOException("\"${target.name}\" already exists")
        if (caseOnly) {
            // Phone storage is case-insensitive, so go through a temporary name.
            val tmp = File(f.parentFile, ".slate-rename-${System.nanoTime()}")
            if (!f.renameTo(tmp) || !tmp.renameTo(target)) throw IOException("Rename failed")
        } else if (!f.renameTo(target)) {
            throw IOException("Rename failed")
        }
        return target
    }

    /** Matches Obsidian's "move to vault .trash folder" delete option, so nothing is lost by accident. */
    fun moveToTrash(f: File) {
        trash.mkdirs()
        val target = if (f.isDirectory) uniqueFile(trash, f.name, "")
        else uniqueFile(trash, f.nameWithoutExtension, if (f.extension.isEmpty()) "" else "." + f.extension)
        if (!f.renameTo(target)) throw IOException("Couldn't delete \"${f.name}\"")
    }

    fun deletePermanently(f: File) {
        if (!f.deleteRecursively()) throw IOException("Couldn't delete \"${f.name}\"")
    }

    private fun uniqueFile(dir: File, base: String, ext: String): File {
        var f = File(dir, "$base$ext")
        var i = 1
        while (f.exists()) {
            f = File(dir, "$base $i$ext")
            i++
        }
        return f
    }

    companion object {
        /** Characters Obsidian doesn't allow in file names (they break links or the filesystem). */
        fun sanitize(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|#^\\[\\]]"), " ").replace(Regex("\\s+"), " ").trim()
    }
}
