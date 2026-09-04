package com.xiaomi.ultralauncher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaomi.ultralauncher.databinding.ItemAppBinding
import com.xiaomi.ultralauncher.databinding.ItemDockBinding

class AppDrawerAdapter(
    private var apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.VH>() {

    private var filtered = apps
    inner class VH(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = filtered[position]
        holder.binding.appIcon.setImageDrawable(app.icon)
        holder.binding.appLabel.text = app.label
        holder.binding.root.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = filtered.size

    fun filter(query: String) {
        filtered = if (query.isEmpty()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
        notifyDataSetChanged()
    }

    fun updateList(newList: List<AppInfo>) {
        apps = newList
        filtered = newList
        notifyDataSetChanged()
    }
}

class DockAdapter(
    private var packages: MutableList<String>,
    private val onClick: (String) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<DockAdapter.VH>() {

    inner class VH(val binding: ItemDockBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDockBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = packages[position]
        try {
            holder.binding.dockIcon.setImageDrawable(
                holder.binding.root.context.packageManager.getApplicationIcon(pkg)
            )
        } catch (_: Exception) {}
        holder.binding.root.setOnClickListener { onClick(pkg) }
        holder.binding.root.setOnLongClickListener { onLongClick(position); true }
    }

    override fun getItemCount() = 7

    fun update(position: Int, pkg: String) {
        packages[position] = pkg
        notifyItemChanged(position)
    }
}
