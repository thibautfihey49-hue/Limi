package com.xiaomi.ultralauncher

import android.app.WallpaperManager
import android.content.Intent
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
    private var apps: List<App> = emptyList()

    private val dockPkgs = arrayOf(
        "com.android.dialer",
        "com.google.android.gm",
        "com.android.chrome",
        "com.google.android.apps.photos",
        "com.whatsapp",
        "com.spotify.music",
        "com.android.camera2"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION, Intent.FLAG_ACTIVITY_NO_ANIMATION)
        setContentView(R.layout.activity_launcher)

        // ✅ Récupération SÉCURE des vues
        root = findViewById(R.id.root)
        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        toggleBar = findViewById(R.id.toggleBar)
        dockRecycler = findViewById(R.id.dockRecycler)
        drawer = findViewById(R.id.drawer)

        // ✅ Fond d'écran SÉCURE
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            window.setBackgroundDrawable(wallpaperManager.drawable)
        } catch (e: Exception) {
            window.setBackgroundColor(0xFFF5F5F5.toInt())
        }

        // ✅ Horloge
        updateClock()
        clock?.postDelayed(object : Runnable {
            override fun run() {
                updateClock()
                clock?.postDelayed(this, 60000)
            }
        }, 60000 - System.currentTimeMillis() % 60000)

        // ✅ Chargement apps
        loadApps()

        // ✅ Dock
        dockRecycler?.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = DockAdapter()
        }

        // ✅ Tiroir
        drawer?.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = AppAdapter()
        }

        // ✅ Clic barre
        toggleBar?.setOnClickListener { toggle() }

        // ✅ Appui long = fond d'écran
        root?.setOnLongClickListener {
            try {
                startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
            } catch (_: Exception) {}
            true
        }

        // ✅ Glisser
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

    private fun updateClock() {
        try {
            clock?.text = fmtTime.format(Date())
            date?.text = fmtDate.format(Date())
        } catch (_: Exception) {}
    }

    private fun loadApps() {
        try {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            apps = resolveInfos
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
            apps = emptyList()
        }
    }

    private fun toggle() {
        if (open) closeDrawer() else openDrawer()
    }

    private fun openDrawer() {
        if (open) return
        open = true
        drawer?.visibility = View.VISIBLE
    }

    private fun closeDrawer() {
        if (!open) return
        open = false
        drawer?.visibility = View.GONE
    }

    private fun launch(pkg: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (_: Exception) {}
        if (open) closeDrawer()
    }

    // ✅ Adapter Apps
    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.appIcon)
            val name: TextView = itemView.findViewById(R.id.appName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            try {
                val app = apps[position]
                holder.icon.setImageDrawable(app.icon)
                holder.name.text = app.name
                holder.itemView.setOnClickListener { launch(app.pkg) }
            } catch (_: Exception) {}
        }

        override fun getItemCount(): Int = apps.size
    }

    // ✅ Adapter Dock
    inner class DockAdapter : RecyclerView.Adapter<DockAdapter.VH>() {
        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.dockIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dock, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            try {
                val pkg = dockPkgs[position]
                holder.icon.setImageDrawable(packageManager.getApplicationIcon(pkg))
                holder.itemView.setOnClickListener { launch(pkg) }
            } catch (_: Exception) {}
        }

        override fun getItemCount(): Int = 7
    }

    override fun onBackPressed() {
        if (open) closeDrawer() else super.onBackPressed()
    }
}
