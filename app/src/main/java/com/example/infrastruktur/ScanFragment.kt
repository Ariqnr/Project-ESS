package com.example.infrastruktur

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.Operation
import com.example.infrastruktur.data.ProductionTask
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanFragment : Fragment() {

    companion object {
        const val ARG_FROM_QC = "from_qc"
        
        fun newInstance(fromQc: Boolean): ScanFragment {
            val fragment = ScanFragment()
            fragment.arguments = bundleOf(ARG_FROM_QC to fromQc)
            return fragment
        }
    }

    private var fromQc: Boolean = false
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private var flashlightOn = false

    // Request Camera Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            barcodeScannerView.resume()
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk memindai secara langsung!", Toast.LENGTH_LONG).show()
        }
    }

    // Register Gallery Image Picker for offline QR decoding
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val scannedSn = decodeQrFromUri(requireContext(), uri)
            if (scannedSn != null) {
                view?.findViewById<EditText>(R.id.et_scan_barcode)?.setText(scannedSn)
                Toast.makeText(context, "Berhasil membaca QR dari Galeri: $scannedSn", Toast.LENGTH_SHORT).show()
                if (fromQc) {
                    returnBarcode(scannedSn)
                } else {
                    val db = AppDatabase.getDatabase(requireContext())
                    val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"
                    showQuickActionDialog(scannedSn, db, role)
                }
            } else {
                Toast.makeText(context, "Gagal membaca QR Code dari gambar yang dipilih!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun decodeQrFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) return null

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            val reader = com.google.zxing.qrcode.QRCodeReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fromQc = it.getBoolean(ARG_FROM_QC, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"

        val etScanBarcode = view.findViewById<EditText>(R.id.et_scan_barcode)
        val btnSearchBarcode = view.findViewById<MaterialButton>(R.id.btn_search_barcode)
        val btnScanGallery = view.findViewById<MaterialButton>(R.id.btn_scan_gallery)
        val fabFlashlight = view.findViewById<FloatingActionButton>(R.id.fab_flashlight)

        // Set text input text color explicitly to on_surface to resolve contrast issues
        etScanBarcode.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))

        barcodeScannerView = view.findViewById(R.id.barcode_scanner)
        barcodeScannerView.setStatusText("") // Hapus tulisan panduan default bawaan SDK

        // Set up continuous barcode decoding callback
        barcodeScannerView.decodeContinuous { result ->
            activity?.runOnUiThread {
                val scannedSn = result.text
                if (scannedSn != null) {
                    barcodeScannerView.pause() // Hentikan decode sementara agar tidak terpanggil berkali-kali
                    etScanBarcode.setText(scannedSn)
                    
                    if (fromQc) {
                        returnBarcode(scannedSn)
                    } else {
                        showQuickActionDialog(scannedSn, db, role)
                    }
                }
            }
        }

        // Kontrol Flashlight fisik via DecoratedBarcodeView
        fabFlashlight.setOnClickListener {
            flashlightOn = !flashlightOn
            if (flashlightOn) {
                barcodeScannerView.setTorchOn()
                fabFlashlight.setImageResource(android.R.drawable.stat_sys_warning)
                Toast.makeText(context, "Senter Menyala", Toast.LENGTH_SHORT).show()
            } else {
                barcodeScannerView.setTorchOff()
                fabFlashlight.setImageResource(android.R.drawable.ic_menu_camera)
                Toast.makeText(context, "Senter Mati", Toast.LENGTH_SHORT).show()
            }
        }

        // PILIH DARI GALERI
        btnScanGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnSearchBarcode.setOnClickListener {
            val sn = etScanBarcode.text.toString().trim()
            if (sn.isEmpty()) {
                Toast.makeText(context, "Masukkan atau pindai Serial Number terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fromQc) {
                returnBarcode(sn)
            } else {
                showQuickActionDialog(sn, db, role)
            }
        }

        // Minta izin kamera dan jalankan preview
        checkAndRequestCameraPermission()
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            barcodeScannerView.resume()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            barcodeScannerView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeScannerView.pause()
    }

    private fun returnBarcode(sn: String) {
        parentFragmentManager.setFragmentResult("scan_result", bundleOf("serialNumber" to sn))
        parentFragmentManager.popBackStack()
    }

    private fun showQuickActionDialog(sn: String, db: AppDatabase, role: String) {
        lifecycleScope.launch {
            val task = db.productionTaskDao().getTaskBySn(sn)
            
            val onDialogDismiss = {
                // Resume pemindaian setelah dialog aksi cepat ditutup
                barcodeScannerView.resume()
            }

            if (task != null) {
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(task.timestamp))
                val readableStatus = when (task.status) {
                    "QC_PENDING" -> "DIRAKIT (ANTREAN QC)"
                    "READY_FOR_RETEST" -> "SIAP UJI ULANG"
                    "QC_PASSED" -> "LULUS QC"
                    "PACKED" -> "TELAH DIPACKING"
                    "FINISHED" -> "DI RAK PENYIMPANAN"
                    "SHIPPED" -> "DIKIRIM (OUTBOUND)"
                    else -> task.status
                }

                val baseMsg = "📌 Serial Number: ${task.serialNumber}\n" +
                              "⚙️ Status Saat Ini: $readableStatus\n" +
                              "🕒 Waktu Registrasi: $dateStr"
                var msg = baseMsg

                val builder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Detail Unit: ${task.productName}")
                    .setOnDismissListener { onDialogDismiss() }

                // Tombol Aksi berdasarkan status saat ini dan role
                if (role == "SUPERVISOR") {
                    msg = "$baseMsg\n\n*Mode Read-Only: Supervisor hanya dapat memantau detail unit."
                } else {
                    when (task.status) {
                        "QC_PENDING", "READY_FOR_RETEST" -> {
                            if (role == "OPERATOR") {
                                builder.setPositiveButton("Buka QC") { _, _ ->
                                    val qcFragment = QualityControlFragment()
                                    parentFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container, qcFragment)
                                        .addToBackStack(null)
                                        .commit()
                                }
                            } else {
                                msg = "$baseMsg\n\n*Akses Terkunci: Hanya Operator yang dapat membuka modul pengujian QC."
                            }
                        }
                        "QC_PASSED" -> {
                            if (role == "OPERATOR") {
                                builder.setPositiveButton("Kemas Unit (Packing)") { _, _ ->
                                    updateTaskStatus(task, "PACKED", "Pengepakan Selesai", "Unit selesai dikemas via Scan.", db)
                                }
                            } else {
                                msg = "$baseMsg\n\n*Akses Terkunci: Hanya Operator yang dapat mengemas unit."
                            }
                        }
                        "PACKED" -> {
                            if (role == "WAREHOUSE") {
                                builder.setPositiveButton("Simpan ke Gudang") { _, _ ->
                                    lifecycleScope.launch {
                                        val finishedGood = db.inventoryDao().getItemBySku("FIN-PANEL-X2")
                                        finishedGood?.let {
                                            it.quantity += 1
                                            db.inventoryDao().updateItem(it)
                                        }
                                        updateTaskStatus(task, "FINISHED", "Barang Jadi Disimpan", "Unit disimpan ke gudang via Scan.", db)
                                    }
                                }
                            } else {
                                msg = "$baseMsg\n\n*Akses Terkunci: Hanya Staff Warehouse yang dapat menyimpan barang jadi."
                            }
                        }
                        "FINISHED" -> {
                            if (role == "WAREHOUSE") {
                                builder.setPositiveButton("Kirim Outbound") { _, _ ->
                                    lifecycleScope.launch {
                                        val finishedGood = db.inventoryDao().getItemBySku("FIN-PANEL-X2")
                                        if (finishedGood != null && finishedGood.quantity > 0) {
                                            finishedGood.quantity -= 1
                                            db.inventoryDao().updateItem(finishedGood)
                                            updateTaskStatus(task, "SHIPPED", "Barang Dikirim", "Unit dikirim outbound via Scan.", db)
                                        } else {
                                            Toast.makeText(context, "Gagal: Stok barang jadi kosong!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                msg = "$baseMsg\n\n*Akses Terkunci: Hanya Staff Warehouse yang dapat mengirim unit outbound."
                            }
                        }
                        "QC_REJECTED" -> {
                            if (role == "OPERATOR") {
                                builder.setPositiveButton("Perbaiki Unit") { _, _ ->
                                    updateTaskStatus(task, "READY_FOR_RETEST", "Perbaikan Unit Selesai", "Unit diperbaiki dan siap uji ulang.", db)
                                }
                            } else {
                                msg = "$baseMsg\n\n*Akses Terkunci: Hanya Operator yang dapat melakukan perbaikan unit."
                            }
                        }
                        "SHIPPED" -> {
                            msg = "$baseMsg\n\nStatus: Unit ini telah dikirim ke pelanggan."
                        }
                    }
                }
                
                builder.setMessage(msg)
                builder.setNeutralButton("Tutup", null)
                builder.show()
            } else {
                // Task tidak terdaftar
                val builder = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Unit Tidak Ditemukan")
                    .setOnDismissListener { onDialogDismiss() }
                    .setNegativeButton("Tutup", null)

                if (role == "OPERATOR") {
                    builder.setMessage("Serial Number $sn tidak terdaftar di sistem.\nApakah Anda ingin memulai perakitan baru dengan SN ini?")
                    builder.setPositiveButton("Mulai Perakitan") { _, _ ->
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, AssemblyFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                } else if (role == "SUPERVISOR") {
                    builder.setMessage("Serial Number $sn tidak terdaftar di sistem.\n*Mode Read-Only: Supervisor dilarang melakukan perakitan baru.")
                } else {
                    builder.setMessage("Serial Number $sn tidak terdaftar di sistem.\n*Akses Terkunci: Hanya Operator yang dapat memulai perakitan baru.")
                }
                
                builder.show()
            }
        }
    }

    private fun updateTaskStatus(task: ProductionTask, newStatus: String, logTitle: String, logDesc: String, db: AppDatabase) {
        lifecycleScope.launch {
            task.status = newStatus
            db.productionTaskDao().updateTask(task)

            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            db.operationDao().insertOperation(Operation(
                title = logTitle,
                description = "SN: ${task.serialNumber}. $logDesc",
                timestamp = time,
                type = if (newStatus.contains("REJECTED")) "ERROR" else "SUCCESS"
            ))

            Toast.makeText(context, "Berhasil memperbarui status unit menjadi $newStatus", Toast.LENGTH_SHORT).show()
        }
    }
}