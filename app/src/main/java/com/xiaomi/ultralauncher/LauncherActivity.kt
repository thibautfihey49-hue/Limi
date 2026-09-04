package com.xiaomi.ultralauncher

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.xiaomi.ultralauncher.databinding.ActivityLauncherBinding
import kotlin.math.abs

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding
    private lateinit var dockManager: DockManager
    private lateinit var drawerAdapter: AppDrawerAdapter
    private lateinit var dockAdapter: DockAdapter
    private var isDrawerOpen = false
    private val interpolator = DecelerateInterpolator(1.5f)

    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            applyWallpaper()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dockManager = DockManager(this)

        // ✅ Fond d'écran transparent
        window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
        )

        applyWallpaper()
        registerReceiver(wallpaperReceiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))

        // ✅ Horloge instantanée — MAJ chaque minute SANS retard
        updateClock()
        val clockRunnable = object : Runnable {
            override fun run() {
                updateClock()
                binding.clockText.postDelayed(this, 60000)
            }
        }
        binding.clockText.postDelayed(clockRunnable, 60000 - System.currentTimeMillis() % 60000)

        // ✅ Dock — SANS animation, chargement IMMÉDIAT
        dockAdapter = DockAdapter(
            dockManager.getDockApps(),
            { pkg -> AppManager.launchApp(this, pkg) },
            { pos -> AppPickerDialog(this) { sel ->
                dockManager.setDockApp(pos, sel)
                dockAdapter.update(pos, sel)
            }.show() }
        )
        binding.dockRecycler.layoutManager = GridLayoutManager(this, 7)
        binding.dockRecycler.adapter = dockAdapter
        binding.dockRecycler.isNestedScrollingEnabled = false

        // ✅ Tiroir — chargement DIFFÉRÉ pour affichage instantané
        binding.drawerContainer.visibility = View.GONE
        binding.root.viewTreeObserver.addOnPreDrawListener {
            if (!::drawerAdapter.isInitialized) {
                val apps = AppManager.getInstalledApps(this)
                drawerAdapter = AppDrawerAdapter(apps) { app ->
                    AppManager.launchApp(this, app.packageName)
                    closeDrawer()
                }
                binding.drawerRecycler.layoutManager = GridLayoutManager(this, 4)
                binding.drawerRecycler.adapter = drawerAdapter
                binding.drawerRecycler.isNestedScrollingEnabled = false
            }
            true
        }

        // ✅ Recherche — filtrée sans délai
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(t: String?): Boolean {
                if (::drawerAdapter.isInitialized) drawerAdapter.filter(t ?: "")
                return true
            }
        })

        // ✅ BARRE DE TOGGLE — CLIC INSTANTANÉ, SANS DÉLAI
        binding.toggleBar.setOnClickListener { toggleDrawer() }

        // ✅ APPUI LONG — CHANGER FOND D'ÉCRAN
        binding.root.setOnLongClickListener {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            startActivity(Intent.createChooser(intent, "Choisir un fond"))
            Toast.makeText(this, "Choisissez votre fond", Toast.LENGTH_SHORT).show()
            true
        }

        // ✅ GESTURES — INSTANTANÉES, SANS DÉLAI
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                if (dy > 150 && abs(vy) > 400) { openDrawer(); return true }
                if (dy < -150 && abs(vy) > 400) { closeDrawer(); return true }
                return false
            }
        })
        binding.root.setOnTouchListener { _, e -> detector.onTouchEvent(e); false }
    }

    private fun updateClock() {
        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE)
        binding.clockText.text = sdfTime.format(java.util.Date())
        val sdfDate = java.text.SimpleDateFormat("EEEE d MMM", java.util.Locale.FRANCE)
        binding.dateText.text = sdfDate.format(java.util.Date())
    }

    private fun applyWallpaper() {
        try {
            binding.root.background = WallpaperManager.getInstance(this).drawable
        } catch (_: Exception) {}
    }

    // ✅ TOGGLE — INSTANTANÉ, ANIMATION RAPIDE
    private fun toggleDrawer() {
        if (isDrawerOpen) closeDrawer() else openDrawer()
    }

    fun openDrawer() {
        if (isDrawerOpen) return
        isDrawerOpen = true
        binding.drawerContainer.visibility = View.VISIBLE
        binding.drawerContainer.animate()
            .translationY(0f)
            .setDuration(150)
            .setInterpolator(interpolator)
            .start()
        if (::drawerAdapter.isInitialized) {
            drawerAdapter.updateList(AppManager.getInstalledApps(this, true))
        }
    }

    fun closeDrawer() {
        if (!isDrawerOpen) return
        isDrawerOpen = false
        binding.drawerContainer.animate()
            .translationY(1000f)
            .setDuration(120)
            .setInterpolator(interpolator)
            .withEndAction { binding.drawerContainer.visibility = View.GONE }
            .start()
    }

    override fun onBackPressed() {
        if (isDrawerOpen) closeDrawer() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wallpaperReceiver)
    }
}
