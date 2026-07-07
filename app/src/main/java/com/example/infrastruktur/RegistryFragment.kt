package com.example.infrastruktur

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.ProductionTask
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistryFragment : Fragment() {

    private lateinit var etSearchSn: EditText
    private lateinit var filterToggleGroup: MaterialButtonToggleGroup
    private lateinit var rvRegistry: RecyclerView
    private lateinit var llEmptyRegistry: LinearLayout
    private lateinit var adapter: RegistryAdapter

    private var allTasks: List<ProductionTask> = emptyList()
    private var selectedFilter: String = "ALL"
    private var searchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearchSn = view.findViewById(R.id.et_search_sn)
        filterToggleGroup = view.findViewById(R.id.filterToggleGroup)
        rvRegistry = view.findViewById(R.id.rv_registry)
        llEmptyRegistry = view.findViewById(R.id.ll_empty_registry)

        adapter = RegistryAdapter(requireContext()) { task ->
            downloadQrCode(task)
        }
        rvRegistry.adapter = adapter

        // Setup filter button listeners
        filterToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedFilter = when (checkedId) {
                    R.id.btn_filter_all -> "ALL"
                    R.id.btn_filter_assembled -> "ASSEMBLED"
                    R.id.btn_filter_tested -> "TESTED"
                    R.id.btn_filter_packed -> "PACKED"
                    R.id.btn_filter_stored -> "STORED"
                    else -> "ALL"
                }
                filterAndDisplayTasks()
            }
        }
        // Check "All" by default
        filterToggleGroup.check(R.id.btn_filter_all)

        // Setup search bar listener
        etSearchSn.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim().lowercase(Locale.getDefault())
                filterAndDisplayTasks()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Collect database tasks flow
        val db = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            db.productionTaskDao().getAllTasks().collectLatest { tasks ->
                allTasks = tasks
                filterAndDisplayTasks()
            }
        }
    }

    private fun filterAndDisplayTasks() {
        val filteredList = allTasks.filter { task ->
            // Filter by Status Tab
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "ASSEMBLED" -> task.status == "QC_PENDING" || task.status == "READY_FOR_RETEST"
                "TESTED" -> task.status == "QC_PASSED"
                "PACKED" -> task.status == "PACKED"
                "STORED" -> task.status == "FINISHED"
                else -> true
            }

            // Filter by Search Bar Query (SN or Product Name)
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                task.serialNumber.lowercase(Locale.getDefault()).contains(searchQuery) ||
                task.productName.lowercase(Locale.getDefault()).contains(searchQuery)
            }

            matchesFilter && matchesSearch
        }

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            rvRegistry.visibility = View.GONE
            llEmptyRegistry.visibility = View.VISIBLE
        } else {
            rvRegistry.visibility = View.VISIBLE
            llEmptyRegistry.visibility = View.GONE
        }
    }

    private fun downloadQrCode(task: ProductionTask) {
        val qrBitmap = QrCodeGenerator.generateQrCode(task.serialNumber)
        if (qrBitmap != null) {
            val filename = "QR_${task.serialNumber}"
            val galleryUri = saveBitmapToGallery(requireContext(), qrBitmap, filename)
            
            // Simpan ke PC juga demi kemudahan Emulator
            var pcSuccess = false
            try {
                val baseDir = java.io.File("C:\\Users\\Alif Windows 10\\Documents\\infrastruktur\\QR_Codes")
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }
                val qrFile = java.io.File(baseDir, "$filename.png")
                val fos = java.io.FileOutputStream(qrFile)
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
                pcSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (galleryUri != null) {
                Toast.makeText(context, "QR Code untuk ${task.serialNumber} berhasil disimpan ke Galeri HP!\n(Pictures/FabricationPro/$filename.png)", Toast.LENGTH_LONG).show()
            } else if (pcSuccess) {
                Toast.makeText(context, "QR Code untuk ${task.serialNumber} berhasil diunduh ke PC!\n(C:\\Users\\Alif Windows 10\\Documents\\infrastruktur\\QR_Codes\\$filename.png)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Gagal mengunduh QR Code.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Gagal men-generate QR Code.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FabricationPro")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                val stream = resolver.openOutputStream(uri)
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                    stream.close()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return uri
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                resolver.delete(uri, null, null)
            }
        }
        return null
    }

    // RecyclerView Adapter
    private class RegistryAdapter(
        private val context: Context,
        private val onDownloadQrClick: (ProductionTask) -> Unit
    ) : RecyclerView.Adapter<RegistryAdapter.ViewHolder>() {

        private var items: List<ProductionTask> = emptyList()

        fun submitList(newList: List<ProductionTask>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_registry_unit, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvProductName.text = item.productName
            holder.tvSn.text = "Serial Number: ${item.serialNumber}"
            
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
            holder.tvTimestamp.text = "Tanggal: $dateStr"

            // Style status badge
            val statusColorRes = when (item.status) {
                "QC_PENDING", "READY_FOR_RETEST" -> R.color.accent_gold
                "QC_PASSED" -> R.color.green_status_text
                "PACKED" -> R.color.secondary
                "FINISHED" -> R.color.outline
                else -> R.color.on_surface_variant
            }
            holder.tvStatus.text = when (item.status) {
                "QC_PENDING" -> "DIRAKIT (ANTREAN QC)"
                "READY_FOR_RETEST" -> "SIAP UJI ULANG"
                "QC_PASSED" -> "LULUS QC"
                "PACKED" -> "TELAH DIPACKING"
                "FINISHED" -> "DI RAK PENYIMPANAN"
                else -> item.status
            }
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, statusColorRes))

            holder.btnDownloadQr.setOnClickListener {
                onDownloadQrClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvProductName: TextView = view.findViewById(R.id.tv_item_product_name)
            val tvStatus: TextView = view.findViewById(R.id.tv_item_status)
            val tvSn: TextView = view.findViewById(R.id.tv_item_sn)
            val tvTimestamp: TextView = view.findViewById(R.id.tv_item_timestamp)
            val btnDownloadQr: MaterialButton = view.findViewById(R.id.btn_item_download_qr)
        }
    }
}
