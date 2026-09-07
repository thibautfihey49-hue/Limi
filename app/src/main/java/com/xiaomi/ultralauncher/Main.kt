package com.xiaomi.ultralauncher

import android.app.Dialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class App(
    val pkg: String,
    val name: String,
    val icon: Drawable
)

data class DockSlot(
    var pkg: String? = null
)

class MainActivity : AppCompatActivity() {

    private var root: FrameLayout? = null
    private var clock: TextView? = null
    private var date: TextView? = null
    private var toggleBar: View? = null
    private var dockRecycler: RecyclerView? = null
    private var drawer: RecyclerView? = null

    private var open = false
    private val fmtTime = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val fmtDate = SimpleDateFormat("EEEE d MMM", Locale.FRANCE)
    private var allApps: List<App> = emptyList()
    private lateinit var dockSlots: MutableList<DockSlot>
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "LimiDockPrefs"
        private const val DOCK_SLOTS_KEY = "dock_slots"
        private const val DOCK_SIZE = 7
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION, Intent.FLAG_ACTIVITY_NO_ANIMATION)
        setContentView(R.layout.activity_launcher)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        root = findViewById(R.id.root)
        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        toggleBar = findViewById(R.id.toggleBar)
        dockRecycler = findViewById(R.id.dockRecycler)
        drawer = findViewById(R.id.drawer)

        loadDockSlots()

        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            window.setBackgroundDrawable(wallpaperManager.drawable)
        } catch (e: Exception) {
            window.decorView.setBackgroundColor(0xFFF5F5F5.toInt())
        }

        updateClock()
        clock?.postDelayed(object : Runnable {
            override fun run() {
                updateClock()
                clock?.postDelayed(this, 60000)
            }
        }, 60000 - System.currentTimeMillis() % 60000)

        loadAllApps()

        dockRecycler?.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = DockAdapter()
        }

        drawer?.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = AppAdapter()
        }

        toggleBar?.setOnClickListener { toggle() }

        root?.setOnLongClickListener {
            try {
                startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
            } catch (_: Exception) {}
            true
        }

        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                vx: Float,
                vy: Float
            ): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                if (dy > 120 && kotlin.math.abs(vy) > 350) {
                    openDrawer()
                    return true
                }
                if (dy < -120 && kotlin.math.abs(vy) > 350) {
                    closeDrawer()
                    return true
                }
                return false
            }
        })

        root?.setOnTouchListener { _, e ->
            gd.onTouchEvent(e)
            false
        }
    }

    private fun loadDockSlots() {
        dockSlots = mutableListOf()
        val saved = prefs.getStringSet(DOCK_SLOTS_KEY, null)
        if (saved != null && saved.size == DOCK_SIZE) {
            saved.forEachIndexed { i, pkg ->
                dockSlots.add(DockSlot(pkg.ifEmpty { null }))
            }
        } else {
            val defaultPkgs = listOf(
                "com.android.dialer",
                "com.google.android.gm",
                "com.android.chrome",
                "com.google.android.apps.photos",
                "com.whatsapp",
                "com.spotify.music",
                "com.android.camera2"
            )
            repeat(DOCK_SIZE) { i ->
                dockSlots.add(DockSlot(defaultPkgs.getOrNull(i)))
            }
        }
    }

    private fun saveDockSlots() {
        val toSave = dockSlots.map { it.pkg ?: "" }.toSet()
        prefs.edit().putStringSet(DOCK_SLOTS_KEY, toSave).apply()
    }

    private fun updateClock() {
        try {
            clock?.text = fmtTime.format(Date())
            date?.text = fmtDate.format(Date())
        } catch (_: Exception) {}
    }

    private fun loadAllApps() {
        try {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            allApps = resolveInfos
                .mapNotNull { info ->
                    try {
                        App(
                            pkg = info.activityInfo.packageName,
                            name = info.loadLabel(pm).toString(),
                            icon = info.loadIcon(pm)
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                .sortedBy { it.name.lowercase() }
        } catch (_: Exception) {
            allApps = emptyList()
        }
    }

    private fun toggle() { if (open) closeDrawer() else openDrawer() }
    private fun openDrawer() { if (open) return; open = true; drawer?.visibility = View.VISIBLE }
    private fun closeDrawer() { if (!open) return; open = false; drawer?.visibility = View.GONE }

    private fun launch(pkg: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (_: Exception) {}
        if (open) closeDrawer()
    }

    private fun showAppPicker(slotIndex: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_app_picker)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val pickerRecycler = dialog.findViewById<RecyclerView>(R.id.pickerRecycler)
        pickerRecycler?.layoutManager = GridLayoutManager(this, 4)
        pickerRecycler?.adapter = AppPickerAdapter(allApps) { selectedApp ->
            dockSlots[slotIndex].pkg = selectedApp.pkg
            saveDockSlots()
            dockRecycler?.adapter?.notifyItemChanged(slotIndex)
            dialog.dismiss()
        }

        dialog.show()
    }

    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.appIcon)
            val name: TextView = itemView.findViewById(R.id.appName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            try {
                val app = allApps[position]
                holder.icon.setImageDrawable(app.icon)
                holder.name.text = app.name
                holder.itemView.setOnClickListener { launch(app.pkg) }
            } catch (_: Exception) {}
        }
        override fun getItemCount() = allApps.size
    }

    inner class DockAdapter : RecyclerView.Adapter<DockAdapter.VH>() {
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.dockIcon)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_dock, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            try {
                val slot = dockSlots[position]
                if (slot.pkg != null) {
                    holder.icon.setImageDrawable(packageManager.getApplicationIcon(slot.pkg!!))
                } else {
                    holder.icon.setImageResource(R.drawable.ic_empty_slot)
                }
                holder.itemView.setOnClickListener { slot.pkg?.let { launch(it) } }
                holder.itemView.setOnLongClickListener { showAppPicker(position); true }
            } catch (_: Exception) {}
        }
        override fun getItemCount() = DOCK_SIZE
    }

    inner class AppPickerAdapter(
        private val apps: List<App>,
        private val onAppSelected: (App) -> Unit
    ) : RecyclerView.Adapter<AppPickerAdapter.VH>() {
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.appIcon)
            val name: TextView = itemView.findViewById(R.id.appName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.name
            holder.itemView.setOnClickListener { onAppSelected(app) }
        }
        override fun getItemCount() = apps.size
    }

    override fun onBackPressed() { if (open) closeDrawer() else super.onBackPressed() }
}
