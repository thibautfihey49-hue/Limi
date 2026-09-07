package com.xiaomi.ultralauncher

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class App(val pkg: String, val name: String, val icon: Drawable)

class MainActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var toggleBar: View
    private lateinit var dockRecycler: RecyclerView
    private lateinit var drawer: RecyclerView
    private var open = false
    private val fmtTime = SimpleDateFormat("HH:mm", Locale.FRANCE)
    private val fmtDate = SimpleDateFormat("EEEE d MMM", Locale.FRANCE)
    private var apps: List<App> = emptyList()
    private val dockPkgs = arrayOf(
        "com.android.dialer", "com.google.android.gm", "com.android.chrome",
        "com.google.android.apps.photos", "com.whatsapp", "com.spotify.music", "com.android.camera2"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION, Intent.FLAG_ACTIVITY_NO_ANIMATION)
        setContentView(R.layout.activity_launcher)

        root = findViewById(R.id.root)
        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)
        toggleBar = findViewById(R.id.toggleBar)
        dockRecycler = findViewById(R.id.dockRecycler)
        drawer = findViewById(R.id.drawer)

        // ✅ Fond d'écran
        window.setBackgroundDrawable(WallpaperManager.getInstance(this).drawable)

        // ✅ Horloge
        updateClock()
        clock.postDelayed(object : Runnable {
            override fun run() { updateClock(); clock.postDelayed(this, 60000) }
        }, 60000 - System.currentTimeMillis() % 60000)

        // ✅ Chargement apps
        loadApps()

        // ✅ Dock
        dockRecycler.layoutManager = GridLayoutManager(this, 7)
        dockRecycler.adapter = DockAdapter()

        // ✅ Tiroir
        drawer.layoutManager = GridLayoutManager(this, 4)
        drawer.adapter = AppAdapter()

        // ✅ CLIC — INSTANTANÉ, SANS ANIMATION
        toggleBar.setOnClickListener { toggle() }

        // ✅ Appui long = fond d'écran
        root.setOnLongClickListener {
            startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
            true
        }

        // ✅ Glisser
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                if (dy > 120 && kotlin.math.abs(vy) > 350) { open(); return true }
                if (dy < -120 && kotlin.math.abs(vy) > 350) { close(); return true }
                return false
            }
        })
        root.setOnTouchListener { _, e -> gd.onTouchEvent(e); false }
    }

    private fun updateClock() {
        clock.text = fmtTime.format(Date())
        date.text = fmtDate.format(Date())
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = pm.queryIntentActivities(intent, 0).map {
            App(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm))
        }.sortedBy { it.name.lowercase() }
    }

    // ✅ SANS ANIMATION — AFFICHAGE INSTANTANÉ
    private fun toggle() { if (open) close() else open() }

    private fun open() {
        if (open) return
        open = true
        drawer.visibility = View.VISIBLE
    }

    private fun close() {
        if (!open) return
        open = false
        drawer.visibility = View.GONE
    }

    private fun launch(pkg: String) {
        try {
            startActivity(packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
        if (open) close()
    }

    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.appIcon)
            val name: TextView = v.findViewById(R.id.appName)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val v = layoutInflater.inflate(R.layout.item_app, p, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, i: Int) {
            val a = apps[i]
            h.icon.setImageDrawable(a.icon)
            h.name.text = a.name
            h.itemView.setOnClickListener { launch(a.pkg) }
        }
        override fun getItemCount() = apps.size
    }

    inner class DockAdapter : RecyclerView.Adapter<DockAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.dockIcon)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val v = layoutInflater.inflate(R.layout.item_dock, p, false)
            return VH(v)
        }
        override fun onBindViewHolder(h: VH, i: Int) {
            val pkg = dockPkgs[i]
            try { h.icon.setImageDrawable(packageManager.getApplicationIcon(pkg)) }
            catch (_: Exception) {}
            h.itemView.setOnClickListener { launch(pkg) }
        }
        override fun getItemCount() = 7
    }

    override fun onBackPressed() { if (open) close() else super.onBackPressed() }
}
