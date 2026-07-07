package com.example.infrastruktur

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.ProductionTask
import com.example.infrastruktur.data.Operation
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssemblyFragment : Fragment() {

    private data class AssemblyStep(val title: String, val description: String, val imageResId: Int)

    private val steps = listOf(
        AssemblyStep("Persiapan Rangka Utama", "Langkah 1: Siapkan rangka utama panel besi pada stasiun perakitan. Dibutuhkan 10 unit Pelat Baja Tipe-B.", android.R.drawable.ic_dialog_map),
        AssemblyStep("Pemasangan Modul Sensor", "Langkah 2: Pasang modul sensor kelistrikan digital di bagian dalam rangka utama. Kencangkan baut modul.", android.R.drawable.ic_menu_compass),
        AssemblyStep("Instalasi Kabel Daya", "Langkah 3: Hubungkan kabel daya utama ke modul sensor dan panel sekering. Pastikan jalur kabel rapi dan terisolasi.", android.R.drawable.ic_menu_preferences),
        AssemblyStep("Perakitan Engsel & Pintu", "Langkah 4: Pasangkan pintu pelindung luar ke rangka panel menggunakan engsel baja 4-inci.", android.R.drawable.ic_menu_gallery),
        AssemblyStep("Kalibrasi & Finishing", "Langkah 5: Lakukan inspeksi visual akhir dan pembersihan sisa kotoran perakitan. Klik SELESAI untuk mengirim ke antrean QC.", android.R.drawable.ic_menu_slideshow)
    )

    private var currentStep = 1 // 1-based index (1 to 5)
    private var rawStockQty = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assembly, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val db = AppDatabase.getDatabase(requireContext())
        val inventoryDao = db.inventoryDao()
        val taskDao = db.productionTaskDao()
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"

        // Bind Views
        val tvStepIndicator = view.findViewById<TextView>(R.id.tv_step_indicator)
        val tvStepPercentage = view.findViewById<TextView>(R.id.tv_step_percentage)
        val progressIndicator = view.findViewById<LinearProgressIndicator>(R.id.progress_indicator)
        val tvStepTitle = view.findViewById<TextView>(R.id.tv_step_title)
        val tvStepDescription = view.findViewById<TextView>(R.id.tv_step_description)
        val tvCurrentStock = view.findViewById<TextView>(R.id.tv_assembly_current_stock)
        val imgStepGuide = view.findViewById<ImageView>(R.id.img_step_guide)
        val btnPrev = view.findViewById<MaterialButton>(R.id.btn_prev_step)
        val btnNext = view.findViewById<MaterialButton>(R.id.btn_next_step)

        if (role == "SUPERVISOR") {
            btnNext.isEnabled = false
            btnNext.text = "Mode Read-Only"
        }

        // Tampilkan Stok Bahan Baku Real-time
        viewLifecycleOwner.lifecycleScope.launch {
            inventoryDao.getAllInventory().collectLatest { items ->
                if (!isAdded) return@collectLatest
                val rawMaterial = items.find { it.sku == "RAW-STEEL-01" }
                rawStockQty = rawMaterial?.quantity ?: 0
                
                tvCurrentStock.text = "Stok Bahan Baku Saat Ini: $rawStockQty Unit"
                
                if (rawStockQty >= 10) {
                    tvCurrentStock.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                } else {
                    tvCurrentStock.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                }
                
                // PENTING: Panggil updateStepUI agar sinkron dengan rawStockQty terbaru
                updateStepUI(tvStepIndicator, tvStepPercentage, progressIndicator, tvStepTitle, tvStepDescription, imgStepGuide, btnPrev, btnNext)
            }
        }

        btnPrev.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI(tvStepIndicator, tvStepPercentage, progressIndicator, tvStepTitle, tvStepDescription, imgStepGuide, btnPrev, btnNext)
            }
        }

        btnNext.setOnClickListener {
            if (currentStep < 5) {
                currentStep++
                updateStepUI(tvStepIndicator, tvStepPercentage, progressIndicator, tvStepTitle, tvStepDescription, imgStepGuide, btnPrev, btnNext)
            } else {
                // Skenario Selesai Perakitan (Langkah 5)
                lifecycleScope.launch {
                    val rawMaterial = inventoryDao.getItemBySku("RAW-STEEL-01")
                    if (rawMaterial != null && rawMaterial.quantity >= 10) {
                        rawMaterial.quantity -= 10
                        inventoryDao.updateItem(rawMaterial)
                        
                        val sn = "SN-" + System.currentTimeMillis().toString().takeLast(6)
                        taskDao.insertTask(ProductionTask(sn, "Panel-X2", "QC_PENDING"))
                        
                        // Catat log feed
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        db.operationDao().insertOperation(Operation(
                            title = "Perakitan Selesai",
                            description = "SN: $sn. Unit baru dirakit & masuk antrean QC.",
                            timestamp = time,
                            type = "SUCCESS"
                        ))

                        // Tampilkan Dialog QR Code Perakitan Selesai
                        showAssemblyCompletedQrDialog(sn)
                    } else {
                        Toast.makeText(context, "Gagal: Stok bahan baku (Pelat Baja Tipe-B) tidak mencukupi!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showAssemblyCompletedQrDialog(sn: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_assembly_completed, null)
        val tvSn = dialogView.findViewById<TextView>(R.id.tv_dialog_sn)
        val imgQr = dialogView.findViewById<ImageView>(R.id.img_dialog_qr)
        val btnDownload = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_download_qr)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_close)

        tvSn.text = sn

        // Generate QR Code bitmap
        val qrBitmap = QrCodeGenerator.generateQrCode(sn)
        if (qrBitmap != null) {
            imgQr.setImageBitmap(qrBitmap)
        }

        val alertDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnDownload.setOnClickListener {
            if (qrBitmap != null) {
                // 1. Simpan ke Galeri Ponsel (Pictures/FabricationPro) via MediaStore
                val galleryUri = saveBitmapToGallery(requireContext(), qrBitmap, "QR_$sn")

                // 2. Simpan Fallback ke Windows PC Directory (untuk kemudahan Emulator)
                var pcSuccess = false
                try {
                    val baseDir = java.io.File("C:\\Users\\Alif Windows 10\\Documents\\infrastruktur\\QR_Codes")
                    if (!baseDir.exists()) {
                        baseDir.mkdirs()
                    }
                    val qrFile = java.io.File(baseDir, "QR_$sn.png")
                    val fos = java.io.FileOutputStream(qrFile)
                    qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    pcSuccess = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (galleryUri != null) {
                    Toast.makeText(context, "QR Code Berhasil Diunduh ke Galeri HP!\n(Pictures/FabricationPro/QR_$sn.png)", Toast.LENGTH_LONG).show()
                } else if (pcSuccess) {
                    Toast.makeText(context, "QR Code Berhasil Diunduh ke PC!\n(C:\\Users\\Alif Windows 10\\Documents\\infrastruktur\\QR_Codes\\QR_$sn.png)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal mengunduh QR Code ke penyimpanan.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnClose.setOnClickListener {
            alertDialog.dismiss()
            parentFragmentManager.popBackStack()
        }

        alertDialog.show()
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
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(uri, null, null)
            }
        }
        return null
    }

    private fun updateStepUI(
        tvIndicator: TextView,
        tvPercentage: TextView,
        progressIndicator: LinearProgressIndicator,
        tvTitle: TextView,
        tvDesc: TextView,
        imgGuide: ImageView,
        btnPrev: MaterialButton,
        btnNext: MaterialButton
    ) {
        val stepData = steps[currentStep - 1]
        
        tvIndicator.text = "Langkah $currentStep dari 5"
        val progress = currentStep * 20
        tvPercentage.text = "$progress% Selesai"
        progressIndicator.progress = progress
        
        tvTitle.text = stepData.title
        tvDesc.text = stepData.description
        imgGuide.setImageResource(stepData.imageResId)

        // Show/Hide buttons based on current step
        btnPrev.visibility = if (currentStep == 1) View.GONE else View.VISIBLE
        
        if (currentStep == 5) {
            btnNext.text = "SELESAI PERAKITAN"
            btnNext.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.tertiary)
        } else {
            btnNext.text = "SELANJUTNYA"
            btnNext.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
        }

        // Disable next button on step 1 if stock is insufficient
        if (currentStep == 1 && rawStockQty < 10) {
            btnNext.isEnabled = false
            btnNext.text = "Stok Tidak Cukup"
        } else {
            val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"
            if (role != "SUPERVISOR") {
                btnNext.isEnabled = true
            }
        }
    }
}
