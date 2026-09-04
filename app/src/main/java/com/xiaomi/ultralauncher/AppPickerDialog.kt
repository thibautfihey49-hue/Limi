package com.xiaomi.ultralauncher

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.xiaomi.ultralauncher.databinding.DialogAppPickerBinding

class AppPickerDialog(
    context: Context,
    private val onSelected: (String) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DialogAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setLayout((context.resources.displayMetrics.widthPixels * 0.9).toInt(), (context.resources.displayMetrics.heightPixels * 0.7).toInt())
        binding.pickerRecycler.layoutManager = GridLayoutManager(context, 4)
        binding.pickerRecycler.adapter = AppDrawerAdapter(AppManager.getInstalledApps(context)) {
            onSelected(it.packageName); dismiss()
        }
    }
}
