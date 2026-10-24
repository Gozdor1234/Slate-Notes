package com.gozdor.slatenotes

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gozdor.slatenotes.data.KeyValueStore
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.Platform
import java.io.File

class AndroidPlatform(private val app: Application) : Platform {
    private val sp = app.getSharedPreferences("slate", Context.MODE_PRIVATE)

    override val store = object : KeyValueStore {
        // Older builds saved typed values (Long, Int, Boolean); toString() reads those too.
        override fun get(key: String): String? = sp.all[key]?.toString()
        override fun put(key: String, value: String?) {
            sp.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
        }
    }

    override val storageRoot: File get() = Environment.getExternalStorageDirectory()

    override val appVersion: String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    override fun toast(message: String) = Toast.makeText(app, message, Toast.LENGTH_SHORT).show()

    override fun openUrl(url: String): Boolean = try {
        app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    override fun loadImage(file: File, maxSize: Int): ImageBitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var sample = 1
        while (opts.outWidth / sample > maxSize || opts.outHeight / sample > maxSize) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }
}

/** Keeps the app state alive across screen rotation. */
class AppHolder(app: Application) : AndroidViewModel(app) {
    val vm = AppViewModel(AndroidPlatform(app), viewModelScope)
}
