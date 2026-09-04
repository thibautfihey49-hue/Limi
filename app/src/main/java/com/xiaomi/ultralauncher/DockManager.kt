package com.xiaomi.ultralauncher

import android.content.Context

class DockManager(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("dock_prefs", Context.MODE_PRIVATE)
    private val defaults = listOf(
        "com.android.dialer", "com.google.android.gm", "com.android.chrome",
        "com.google.android.apps.photos", "com.whatsapp", "com.spotify.music", "com.android.camera2"
    )

    fun getDockApps(): MutableList<String> =
        (0 until 7).map { i -> prefs.getString("dock_$i", defaults[i])!! }.toMutableList()

    fun setDockApp(pos: Int, pkg: String) = prefs.edit().putString("dock_$pos", pkg).apply()
}
