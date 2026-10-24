package com.gozdor.slatenotes.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.gozdor.slatenotes.data.KeyValueStore
import java.io.File

/** The few things the Android and Windows apps do differently. Everything else is shared. */
interface Platform {
    /** Where vault browsing and vault detection start. */
    val storageRoot: File
    val store: KeyValueStore
    val appVersion: String
    fun toast(message: String)

    /** Opens a web link in the default browser; false if nothing could open it. */
    fun openUrl(url: String): Boolean

    /** Decodes an image file, scaled down so neither side exceeds [maxSize] pixels. */
    fun loadImage(file: File, maxSize: Int): ImageBitmap?
}
