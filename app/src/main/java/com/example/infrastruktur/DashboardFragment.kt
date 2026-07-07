package com.example.infrastruktur

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.InventoryItem
import com.example.infrastruktur.data.Operation
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"
        setupUIByRole(view, role)
        setupNavigation(view)

        val db = AppDatabase.getDatabase(requireContext())
        observeDatabaseData(view, db)
        setupExportAction(view, db, role)
    }

    private fun setupUIByRole(view: View, role: String) {
        val welcomeText = view.findViewById<TextView>(R.id.text_welcome)
        val roleSubtitle = view.findViewById<TextView>(R.id.text_role_subtitle)
        val btnExport = view.findViewById<MaterialButton>(R.id.btn_export_pdf)
        val sectionCharts = view.findViewById<View>(R.id.section_charts)
        
        val sectionWarehouse = view.findViewById<View>(R.id.section_warehouse)
        val sectionProduction = view.findViewById<View>(R.id.section_production)
        val sectionLogistics = view.findViewById<View>(R.id.section_logistics)

        when (role) {
            "OPERATOR" -> {
                welcomeText.text = getString(R.string.welcome_operator)
                roleSubtitle.text = getString(R.string.subtitle_operator)
                btnExport.visibility = View.GONE
                sectionCharts.visibility = View.GONE
                
                sectionWarehouse.visibility = View.VISIBLE
                sectionProduction.visibility = View.VISIBLE
                sectionLogistics.visibility = View.GONE
                
                view.findViewById<View>(R.id.card_stock).visibility = View.GONE
                view.findViewById<View>(R.id.card_store_finished).visibility = View.GONE
            }
            "WAREHOUSE" -> {
                welcomeText.text = getString(R.string.welcome_warehouse)
                roleSubtitle.text = getString(R.string.subtitle_warehouse)
                btnExport.visibility = View.GONE
                sectionCharts.visibility = View.GONE
                
                sectionWarehouse.visibility = View.VISIBLE
                sectionProduction.visibility = View.GONE
                sectionLogistics.visibility = View.VISIBLE
                
                view.findViewById<View>(R.id.card_receiving).visibility = View.GONE
            }
            "SUPERVISOR" -> {
                welcomeText.text = getString(R.string.welcome_supervisor)
                roleSubtitle.text = getString(R.string.subtitle_supervisor)
                btnExport.visibility = View.VISIBLE
                sectionCharts.visibility = View.VISIBLE
                
                sectionWarehouse.visibility = View.VISIBLE
                sectionProduction.visibility = View.VISIBLE
                sectionLogistics.visibility = View.VISIBLE
            }
        }
    }

    private fun setupNavigation(view: View) {
        view.findViewById<MaterialCardView>(R.id.card_receiving).setOnClickListener { loadFragment(OperationsFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_stock).setOnClickListener { loadFragment(OperationsFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_store_finished).setOnClickListener { loadFragment(OperationsFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_assembly).setOnClickListener { loadFragment(ProductionFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_qc).setOnClickListener { loadFragment(ProductionFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_packing).setOnClickListener { loadFragment(ProductionFragment()) }
        view.findViewById<MaterialCardView>(R.id.card_outbound).setOnClickListener { loadFragment(DeliveryFragment()) }
    }

    private fun observeDatabaseData(view: View, db: AppDatabase) {
        val tvBentoReceiving = view.findViewById<TextView>(R.id.tv_bento_receiving)
        val tvBentoCriticalStock = view.findViewById<TextView>(R.id.tv_bento_critical_stock)
        val tvBentoAssemblyTasks = view.findViewById<TextView>(R.id.tv_bento_assembly_tasks)
        val feedContainer = view.findViewById<LinearLayout>(R.id.feed_container)

        // 1. Observe Inventory for stats
        viewLifecycleOwner.lifecycleScope.launch {
            db.inventoryDao().getAllInventory().collectLatest { inventoryItems ->
                if (!isAdded) return@collectLatest
                
                // Penerimaan: total raw steel received (kumulatif dari SharedPreferences)
                val sharedPref = requireContext().getSharedPreferences("StatsData", Context.MODE_PRIVATE)
                val totalReceived = sharedPref.getInt("TOTAL_RECEIVED_RAW", 0)
                tvBentoReceiving.text = String.format(Locale.getDefault(), "%d Unit", totalReceived)

                // Stok Kritis: count items with quantity < 10
                val criticalItemsCount = inventoryItems.filter { it.quantity < 10 }.size
                tvBentoCriticalStock.text = String.format(Locale.getDefault(), "%02d Item", criticalItemsCount)
            }
        }

        // 2. Observe Production Tasks for active queue count
        viewLifecycleOwner.lifecycleScope.launch {
            db.productionTaskDao().getAllTasks().collectLatest { tasks ->
                if (!isAdded) return@collectLatest
                val activeQueueCount = tasks.filter {
                    it.status in listOf("QC_PENDING", "READY_FOR_RETEST", "QC_PASSED", "PACKED")
                }.size
                tvBentoAssemblyTasks.text = String.format(Locale.getDefault(), "%d Antrian Aktif", activeQueueCount)
            }
        }

        // 3. Observe Operations for dynamic Feed
        viewLifecycleOwner.lifecycleScope.launch {
            db.operationDao().getAllOperations().collectLatest { operations ->
                feedContainer.removeAllViews()
                
                if (operations.isEmpty()) {
                    val noDataText = TextView(context).apply {
                        text = "Belum ada aktivitas tercatat."
                        setPadding(16, 16, 16, 16)
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.outline))
                    }
                    feedContainer.addView(noDataText)
                    return@collectLatest
                }

                // Ambil 5 aktivitas terbaru
                val latestOperations = operations.take(5)
                latestOperations.forEach { operation ->
                    val feedItemView = LayoutInflater.from(context).inflate(R.layout.item_operation_feed, feedContainer, false)
                    
                    val textTitle = feedItemView.findViewById<TextView>(R.id.text_title)
                    val textSubtitle = feedItemView.findViewById<TextView>(R.id.text_subtitle)
                    val textStatus = feedItemView.findViewById<TextView>(R.id.text_status)
                    val statusPill = feedItemView.findViewById<MaterialCardView>(R.id.status_pill)
                    val textTimestamp = feedItemView.findViewById<TextView>(R.id.text_timestamp)

                    textTitle.text = operation.title
                    textSubtitle.text = operation.description
                    textTimestamp.text = operation.timestamp

                    if (operation.type == "ERROR") {
                        textStatus.text = "REJECTED"
                        textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_error_container))
                        statusPill.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error_container))
                    } else {
                        textStatus.text = "SUCCESS"
                        textStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_status_text))
                        statusPill.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_status_bg))
                    }

                    feedContainer.addView(feedItemView)
                }
            }
        }
    }

    private fun setupExportAction(view: View, db: AppDatabase, role: String) {
        val btnExport = view.findViewById<MaterialButton>(R.id.btn_export_pdf)
        
        btnExport.setOnClickListener {
            if (role != "SUPERVISOR") {
                Toast.makeText(context, "Akses Ditolak: Hanya Supervisor yang dapat mengekspor laporan.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val inventory = db.inventoryDao().getAllInventory().first()
                val tasks = db.productionTaskDao().getAllTasks().first()
                val operations = db.operationDao().getAllOperations().first()

                val reportBuilder = StringBuilder()
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

                reportBuilder.append("=========================================\n")
                reportBuilder.append("     LAPORAN SHIFT - FABRICATIONPRO      \n")
                reportBuilder.append("=========================================\n")
                reportBuilder.append("Waktu Ekspor : $dateStr\n")
                reportBuilder.append("Petugas      : Shift A Plant Supervisor\n")
                reportBuilder.append("-----------------------------------------\n\n")

                reportBuilder.append("[ STATUS INVENTORY ]\n")
                inventory.forEach { item ->
                    reportBuilder.append("- SKU: ${item.sku} | ${item.name} : ${item.quantity} Unit (${item.type})\n")
                }
                reportBuilder.append("\n")

                reportBuilder.append("[ ANTRIAN PRODUKSI ]\n")
                val activeTasks = tasks.filter { it.status != "SHIPPED" }
                if (activeTasks.isEmpty()) {
                    reportBuilder.append("Tidak ada antrian aktif saat ini.\n")
                } else {
                    activeTasks.forEach { task ->
                        reportBuilder.append("- SN: ${task.serialNumber} | Produk: ${task.productName} | Status: ${task.status}\n")
                    }
                }
                reportBuilder.append("\n")

                reportBuilder.append("[ 10 LOG OPERASI TERAKHIR ]\n")
                operations.take(10).forEach { op ->
                    reportBuilder.append("[${op.timestamp}] [${op.type}] ${op.title} : ${op.description}\n")
                }
                reportBuilder.append("\n=========================================")

                var phoneUri: android.net.Uri? = null
                var pcSuccess = false
                val reportContent = reportBuilder.toString()
                val filename = "laporan_shift_a"

                // 1. Simpan ke HP (Downloads/FabricationPro) via MediaStore
                try {
                    val resolver = requireContext().contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$filename.txt")
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/FabricationPro")
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    } else {
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                        val appDir = File(downloadsDir, "FabricationPro")
                        if (!appDir.exists()) appDir.mkdirs()
                        val file = File(appDir, "$filename.txt")
                        file.writeText(reportContent)
                        android.media.MediaScannerConnection.scanFile(requireContext(), arrayOf(file.absolutePath), null, null)
                        android.net.Uri.fromFile(file)
                    }

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { stream ->
                            val writer = java.io.OutputStreamWriter(stream)
                            writer.write(reportContent)
                            writer.flush()
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }
                        phoneUri = uri
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Simpan Fallback ke Windows PC Directory (untuk kemudahan Emulator)
                try {
                    val baseDir = File("C:\\Users\\Alif Windows 10\\Documents\\infrastruktur")
                    if (!baseDir.exists()) {
                        baseDir.mkdirs()
                    }
                    val reportFile = File(baseDir, "laporan_shift_a.txt")
                    reportFile.writeText(reportContent)
                    pcSuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (phoneUri != null) {
                    Toast.makeText(
                        context, 
                        "Laporan Shift Berhasil Diekspor ke HP!\nFolder: Downloads/FabricationPro/$filename.txt", 
                        Toast.LENGTH_LONG
                    ).show()
                } else if (pcSuccess) {
                    Toast.makeText(
                        context, 
                        "Laporan Shift Berhasil Diekspor ke PC!\nLokasi: C:\\Users\\Alif Windows 10\\Documents\\infrastruktur\\$filename.txt", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Gagal mengekspor laporan.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
