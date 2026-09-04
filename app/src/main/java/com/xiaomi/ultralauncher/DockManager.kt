package com.xiaomi.ultralauncher

import android.content.Context

class DockManager(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("dock_prefs", Context.MODE_PRIVATE)

    fun getDockApps(): MutableList<String> {
        val defaults = listOf(
            "com.android.dialer",
            "com.google.android.gm",
            "com.android.chrome",
            "com.google.android.apps.photos",
            "com.whatsapp",
            "com.spotify.music",
            "com.android.camera2"
        )
        return (0 until 7).map { i ->
            prefs.getString("dock_$i", defaults[i]) ?: defaults[i]
        }.toMutableList()
    }

    fun setDockApp(pos: Int, pkg: String) {
        prefs.edit().putString("dock_$pos", pkg).apply()
    }
}
