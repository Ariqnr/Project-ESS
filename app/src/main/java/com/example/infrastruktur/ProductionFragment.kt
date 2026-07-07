package com.example.infrastruktur

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.Operation
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_production, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val db = AppDatabase.getDatabase(requireContext())
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"
        
        val btnAssembly = view.findViewById<MaterialButton>(R.id.btn_action_assembly)
        val btnQC = view.findViewById<MaterialButton>(R.id.btn_action_qc)
        val btnPacking = view.findViewById<MaterialButton>(R.id.btn_action_packing)
        val tvPackingHint = view.findViewById<TextView>(R.id.tv_packing_hint)

        // 3. Logika Validasi Alur Kerja (Conditional State Disable)
        // Berdasarkan Activity Diagram, produk dilarang Packing jika belum QC Passed.
        viewLifecycleOwner.lifecycleScope.launch {
            db.productionTaskDao().getReadyToPack().collectLatest { tasks ->
                val isReady = tasks.isNotEmpty()
                
                if (role == "SUPERVISOR") {
                    btnPacking.isEnabled = false
                    btnPacking.text = "Monitoring Pengemasan (Read-Only)"
                    tvPackingHint.visibility = View.GONE
                } else {
                    btnPacking.isEnabled = isReady
                    if (isReady) {
                        tvPackingHint.text = getString(R.string.hint_packing_ready, tasks.size)
                        tvPackingHint.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    } else {
                        tvPackingHint.text = getString(R.string.hint_packing_locked)
                        tvPackingHint.setTextColor(resources.getColor(R.color.error, null))
                    }
                }
            }
        }

        // Supervisor: Mode Read-Only (Kunci semua tombol aksi teknis)
        if (role == "SUPERVISOR") {
            btnAssembly.isEnabled = false
            btnAssembly.text = "Monitoring Produksi (Read-Only)"
            btnQC.isEnabled = false
            btnQC.text = "Monitoring Pengujian (Read-Only)"
        }

        btnAssembly.setOnClickListener { loadFragment(AssemblyFragment()) }
        btnQC.setOnClickListener { loadFragment(QualityControlFragment()) }

        btnPacking.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val tasks = db.productionTaskDao().getReadyToPack().first()
                if (tasks.isNotEmpty()) {
                    val task = tasks.first()

                    val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pack_material, null)
                    dialog.setContentView(dialogView)

                    val etBoxId = dialogView.findViewById<EditText>(R.id.et_dialog_box_id)
                    val etWeight = dialogView.findViewById<EditText>(R.id.et_dialog_weight)
                    val etNotes = dialogView.findViewById<EditText>(R.id.et_dialog_notes)
                    val btnConfirm = dialogView.findViewById<View>(R.id.btn_dialog_pack_confirm)
                    val btnCancel = dialogView.findViewById<View>(R.id.btn_dialog_pack_cancel)

                    // Prefill Box ID
                    val randomBoxId = "BOX-" + System.currentTimeMillis().toString().takeLast(6)
                    etBoxId.setText(randomBoxId)

                    btnConfirm.setOnClickListener {
                        val boxId = etBoxId.text.toString().trim()
                        val weight = etWeight.text.toString().trim()
                        val notes = etNotes.text.toString().trim()

                        if (boxId.isEmpty() || weight.isEmpty()) {
                            Toast.makeText(context, "Gagal: Data pengemasan harus lengkap!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        lifecycleScope.launch {
                            task.status = "PACKED"
                            db.productionTaskDao().updateTask(task)

                            // Catat log feed
                            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            db.operationDao().insertOperation(Operation(
                                title = "Pengepakan Selesai",
                                description = "SN: ${task.serialNumber} dikemas di $boxId ($weight kg). Catatan: $notes.",
                                timestamp = time,
                                type = "SUCCESS"
                            ))

                            Toast.makeText(context, "SN: ${task.serialNumber} Selesai dikemas. Silakan Staff Warehouse melakukan penyimpanan.", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        }
                    }

                    btnCancel.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.show()
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
