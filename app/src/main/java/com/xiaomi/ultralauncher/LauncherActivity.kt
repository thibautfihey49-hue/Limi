package com.xiaomi.ultralauncher

import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dockManager = DockManager(this)

        try {
            window.setBackgroundDrawable(WallpaperManager.getInstance(this).drawable)
        } catch (_: Exception) {}

        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE)
        binding.clockText.text = sdfTime.format(java.util.Date())
        val sdfDate = java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.FRANCE)
        binding.dateText.text = sdfDate.format(java.util.Date())

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
        binding.dockRecycler.itemAnimator = null

        val apps = AppManager.getInstalledApps(this)
        drawerAdapter = AppDrawerAdapter(apps) { app ->
            AppManager.launchApp(this, app.packageName)
            closeDrawer()
        }
        binding.drawerRecycler.layoutManager = GridLayoutManager(this, 4)
        binding.drawerRecycler.adapter = drawerAdapter
        binding.drawerRecycler.itemAnimator = null

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(t: String?): Boolean {
                drawerAdapter.filter(t ?: "")
                return true
            }
        })

        // ✅ BARRE DE TOGGLE — UN CLIC = OUVRE/FERME
        binding.toggleBar.setOnClickListener { toggleDrawer() }

        // ✅ APPUI LONG N'IMPORTE OU = CHANGER FOND D'ÉCRAN
        binding.root.setOnLongClickListener {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            startActivity(Intent.createChooser(intent, "Choisir un fond d'écran"))
            Toast.makeText(this, "Choisissez votre fond d'écran", Toast.LENGTH_SHORT).show()
            true
        }

        // ✅ GLISSER VERS LE HAUT/BAS TOUJOURS FONCTIONNE
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                if (dy < -200 && abs(vy) > 500) { openDrawer(); return true }
                if (dy > 200 && abs(vy) > 500) { closeDrawer(); return true }
                return false
            }
        })
        binding.root.setOnTouchListener { _, e -> detector.onTouchEvent(e); false }
    }

    // ✅ FONCTION TOGGLE — SIMPLE
    private fun toggleDrawer() {
        if (isDrawerOpen) closeDrawer() else openDrawer()
    }

    fun openDrawer() {
        if (isDrawerOpen) return
        isDrawerOpen = true
        binding.drawerContainer.visibility = View.VISIBLE
        binding.drawerContainer.animate().translationY(0f).setDuration(220).start()
        drawerAdapter.updateList(AppManager.getInstalledApps(this, true))
    }

    fun closeDrawer() {
        if (!isDrawerOpen) return
        isDrawerOpen = false
        binding.drawerContainer.animate().translationY(1000f).setDuration(180)
            .withEndAction { binding.drawerContainer.visibility = View.GONE }.start()
    }

    override fun onBackPressed() {
        if (isDrawerOpen) closeDrawer() else super.onBackPressed()
    }
}
