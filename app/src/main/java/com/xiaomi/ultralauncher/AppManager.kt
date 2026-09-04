package com.xiaomi.ultralauncher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo) =
        label.lowercase().compareTo(other.label.lowercase())
}

object AppManager {
    private var cache: List<AppInfo>? = null
    private var lastRefresh = 0L

    fun getInstalledApps(ctx: Context, force: Boolean = false): List<AppInfo> {
        val now = System.currentTimeMillis()
        if (!force && cache != null && now - lastRefresh < 30000) return cache!!

        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = pm.queryIntentActivities(intent, 0).map {
            AppInfo(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm)
            )
        }.sorted()
        cache = apps
        lastRefresh = now
        return apps
    }

    fun launchApp(ctx: Context, pkg: String) {
        try {
            val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
            i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (_: Exception) {}
    }
}
