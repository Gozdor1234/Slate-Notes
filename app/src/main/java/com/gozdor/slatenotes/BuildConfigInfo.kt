package com.gozdor.slatenotes

import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel

object BuildConfigInfo {
    fun versionName(vm: AndroidViewModel): String {
        val ctx = vm.getApplication<android.app.Application>()
        return try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
