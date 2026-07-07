package com.example.infrastruktur

import android.content.Context
import android.os.Bundle
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
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.InventoryItem
import com.example.infrastruktur.data.Operation
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OperationsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_operations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val inventoryDao = db.inventoryDao()
        val taskDao = db.productionTaskDao()
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"

        val btnSimulasiTerima = view.findViewById<MaterialButton>(R.id.btn_simulasi_terima_100)
        val btnSimpanJadi = view.findViewById<MaterialButton>(R.id.btn_simulasi_simpan_jadi)
        val tvPackedStatus = view.findViewById<TextView>(R.id.tv_packed_status)
        val stockListContainer = view.findViewById<LinearLayout>(R.id.stock_list_container)

        // Skenario Supervisor: Read-Only
        if (role == "SUPERVISOR") {
            btnSimulasiTerima.isEnabled = false
            btnSimpanJadi.isEnabled = false
            btnSimulasiTerima.text = "Mode Read-Only"
        }

        // UC-02 & Penerimaan Barang: Mengelola Stok Bahan Baku (Dialog Dinamis)
        btnSimulasiTerima.setOnClickListener {
            val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_receive_material, null)
            dialog.setContentView(dialogView)

            val etSupplier = dialogView.findViewById<EditText>(R.id.et_dialog_supplier)
            val etInvoice = dialogView.findViewById<EditText>(R.id.et_dialog_invoice)
            val etQty = dialogView.findViewById<EditText>(R.id.et_dialog_qty)
            val btnReceive = dialogView.findViewById<View>(R.id.btn_dialog_receive)
            val btnCancel = dialogView.findViewById<View>(R.id.btn_dialog_cancel)

            btnReceive.setOnClickListener {
                val supplier = etSupplier.text.toString().trim()
                val invoice = etInvoice.text.toString().trim()
                val qtyStr = etQty.text.toString().trim()

                if (supplier.isEmpty() || invoice.isEmpty() || qtyStr.isEmpty()) {
                    Toast.makeText(context, "Gagal: Data penerimaan harus lengkap!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val qty = qtyStr.toIntOrNull() ?: 100

                lifecycleScope.launch {
                    val item = inventoryDao.getItemBySku("RAW-STEEL-01") ?: InventoryItem("RAW-STEEL-01", "Pelat Baja Tipe-B", 0, "RAW")
                    item.quantity += qty
                    inventoryDao.insertItem(item)

                    // Update stats untuk dashboard penerimaan (kumulatif)
                    val sharedPref = requireContext().getSharedPreferences("StatsData", Context.MODE_PRIVATE)
                    val currentTotal = sharedPref.getInt("TOTAL_RECEIVED_RAW", 0)
                    sharedPref.edit().putInt("TOTAL_RECEIVED_RAW", currentTotal + qty).apply()

                    // Log ke Feed
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    db.operationDao().insertOperation(Operation(
                        title = "Penerimaan Bahan Baku",
                        description = "Menerima $qty unit Pelat Baja dari $supplier (Invoice: $invoice).",
                        timestamp = time,
                        type = "SUCCESS"
                    ))

                    dialog.dismiss()
                }
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        // UC-08: Menyimpan Barang Jadi (Konfirmasi dari rak packing)
        viewLifecycleOwner.lifecycleScope.launch {
            taskDao.getAllTasks().collectLatest { tasks ->
                val packedTasks = tasks.filter { it.status == "PACKED" }
                
                // Update tvPackedStatus regardless of role (Warehouse sees action, Supervisor read-only status)
                tvPackedStatus.text = if (packedTasks.isNotEmpty()) "${packedTasks.size} unit siap disimpan ke rak" else "Belum ada unit yang selesai dipacking"
                
                if (role == "WAREHOUSE") {
                    btnSimpanJadi.isEnabled = packedTasks.isNotEmpty()
                }
            }
        }

        btnSimpanJadi.setOnClickListener {
            lifecycleScope.launch {
                val packedTasks = taskDao.getAllTasks().first().filter { it.status == "PACKED" }
                if (packedTasks.isNotEmpty()) {
                    val task = packedTasks.first()
                    
                    // Show a rack confirmation dialog
                    val rackOptions = arrayOf("Rak A1", "Rak A2", "Rak B1", "Rak B2")
                    var selectedRack = "Rak A1"
                    
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Konfirmasi Penyimpanan ke Gudang")
                        .setSingleChoiceItems(rackOptions, 0) { _, which ->
                            selectedRack = rackOptions[which]
                        }
                        .setPositiveButton("Simpan") { _, _ ->
                            lifecycleScope.launch {
                                task.status = "FINISHED"
                                taskDao.updateTask(task)

                                // Update Saldo Barang Jadi
                                val finishedGood = inventoryDao.getItemBySku("FIN-PANEL-X2")
                                finishedGood?.let {
                                    it.quantity += 1
                                    inventoryDao.updateItem(it)
                                }

                                // Log ke Feed
                                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                db.operationDao().insertOperation(Operation(
                                    title = "Barang Jadi Disimpan",
                                    description = "SN: ${task.serialNumber} disimpan di $selectedRack.",
                                    timestamp = time,
                                    type = "SUCCESS"
                                ))

                                Toast.makeText(context, "Berhasil: Unit disimpan ke $selectedRack & stok barang jadi bertambah.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }
            }
        }

        // Tampilkan Daftar Stok secara Real-time dengan Card Indah
        lifecycleScope.launch {
            inventoryDao.getAllInventory().collectLatest { items ->
                stockListContainer.removeAllViews()
                items.forEach { item ->
                    val itemView = LayoutInflater.from(context).inflate(R.layout.item_inventory_stock, stockListContainer, false)
                    val tvItemName = itemView.findViewById<TextView>(R.id.tv_item_name)
                    val tvItemSku = itemView.findViewById<TextView>(R.id.tv_item_sku)
                    val tvItemQuantity = itemView.findViewById<TextView>(R.id.tv_item_quantity)
                    val tvItemType = itemView.findViewById<TextView>(R.id.tv_item_type)
                    val typeCard = itemView.findViewById<MaterialCardView>(R.id.type_card)

                    tvItemName.text = item.name
                    tvItemSku.text = "SKU: ${item.sku}"
                    tvItemQuantity.text = "${item.quantity} Unit"

                    if (item.type == "RAW") {
                        tvItemType.text = "BAHAN BAKU"
                        tvItemType.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_secondary_container))
                        typeCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary_container))
                    } else {
                        tvItemType.text = "BARANG JADI"
                        tvItemType.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_status_text))
                        typeCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_status_bg))
                    }

                    stockListContainer.addView(itemView)
                }
            }
        }
    }
}
