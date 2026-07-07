package com.example.infrastruktur

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.ProductionTask
import com.example.infrastruktur.data.Operation
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeliveryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_delivery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = AppDatabase.getDatabase(requireContext())
        val taskDao = db.productionTaskDao()
        val inventoryDao = db.inventoryDao()
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"

        val etExpedition = view.findViewById<TextInputEditText>(R.id.et_expedition)
        val etResi = view.findViewById<TextInputEditText>(R.id.et_resi)
        val btnShip = view.findViewById<MaterialButton>(R.id.btn_process_shipping)

        if (role == "SUPERVISOR") {
            btnShip.isEnabled = false
            btnShip.text = "Monitoring Logistik (Read-Only)"
        }

        btnShip.setOnClickListener {
            val expedition = etExpedition.text.toString()
            val resi = etResi.text.toString()

            if (expedition.isEmpty() || resi.isEmpty()) {
                Toast.makeText(context, "Lengkapi Nama Ekspedisi & No Resi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Ambil barang yang statusnya FINISHED (Siap Kirim dari Gudang)
                val readyTasks = taskDao.getAllTasks().first().filter { it.status == "FINISHED" }
                
                if (readyTasks.isNotEmpty()) {
                    // Potong stok barang jadi
                    val finishedGood = inventoryDao.getItemBySku("FIN-PANEL-X2")
                    if (finishedGood == null || finishedGood.quantity <= 0) {
                        Toast.makeText(context, "Gagal: Stok barang jadi (FIN-PANEL-X2) kosong atau tidak mencukupi!", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val task = readyTasks.first()
                    
                    // 1. Update status di DB
                    task.status = "SHIPPED"
                    taskDao.updateTask(task)
                    
                    // 2. Potong stok
                    finishedGood.quantity -= 1
                    inventoryDao.updateItem(finishedGood)

                    // 3. Log ke Feed
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    db.operationDao().insertOperation(Operation(
                        title = "Barang Dikirim",
                        description = "SN: ${task.serialNumber} dikirim via $expedition (Resi: $resi).",
                        timestamp = time,
                        type = "SUCCESS"
                    ))

                    // 4. Simulasi Cetak Surat Jalan
                    Toast.makeText(context, "UC-09: Stok Dipotong. Surat Jalan Dicetak untuk SN: ${task.serialNumber}\nEkspedisi: $expedition", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Tidak ada barang 'Finished' di gudang yang siap dikirim!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
