package com.example.infrastruktur

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.ProductionTask
import com.example.infrastruktur.data.Operation
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QualityControlFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_quality_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = AppDatabase.getDatabase(requireContext())
        val taskDao = db.productionTaskDao()
        val role = (activity as? MainActivity)?.getUserRole() ?: "OPERATOR"
        
        val llQueueContainer = view.findViewById<LinearLayout>(R.id.ll_qc_queue_container)
        val etSerialNumber = view.findViewById<TextInputEditText>(R.id.et_serial_number)
        val tilSerialNumber = view.findViewById<TextInputLayout>(R.id.til_serial_number)
        val btnPass = view.findViewById<MaterialButton>(R.id.btn_pass_qc)
        val btnFail = view.findViewById<MaterialButton>(R.id.btn_fail_qc)
        val btnRepair = view.findViewById<MaterialButton>(R.id.btn_repair)

        // Ubah label tombol pass menjadi pengujian manual kelayakan
        btnPass.text = "Mulai Pengujian Kelayakan"
        btnPass.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
        
        // Sembunyikan tombol simulasi instan gagal karena sudah digantikan oleh form evaluasi manual
        btnFail.visibility = View.GONE

        // Skenario Supervisor: Read-Only
        if (role == "SUPERVISOR") {
            btnPass.isEnabled = false
            btnRepair.isEnabled = false
            btnPass.text = "Lihat Status (Supervisor)"
            tilSerialNumber.isEndIconVisible = false
        }

        // Listener untuk Scan Barcode
        tilSerialNumber.setEndIconOnClickListener {
            val scanFragment = ScanFragment.newInstance(fromQc = true)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, scanFragment)
                .addToBackStack(null)
                .commit()
        }

        // Menerima hasil scan dari ScanFragment
        parentFragmentManager.setFragmentResultListener("scan_result", viewLifecycleOwner) { _, bundle ->
            val sn = bundle.getString("serialNumber")
            etSerialNumber.setText(sn)
        }

        // Tampilkan Antrean Unit Siap Diuji secara Real-time
        viewLifecycleOwner.lifecycleScope.launch {
            taskDao.getAllTasks().collectLatest { tasks ->
                llQueueContainer.removeAllViews()
                val queue = tasks.filter { it.status == "QC_PENDING" || it.status == "READY_FOR_RETEST" }
                
                if (queue.isEmpty()) {
                    val tvEmpty = TextView(context).apply {
                        text = "Tidak ada antrean unit yang perlu diuji."
                        setPadding(0, 24, 0, 24)
                        gravity = android.view.Gravity.CENTER
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.outline))
                        textSize = 13f
                    }
                    llQueueContainer.addView(tvEmpty)
                } else {
                    queue.forEach { task ->
                        val itemCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
                            }
                            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                            radius = 8 * resources.displayMetrics.density
                            strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
                            strokeWidth = (1 * resources.displayMetrics.density).toInt()
                            cardElevation = 0f
                            isClickable = true
                            isFocusable = true
                            
                            val padding = (12 * resources.displayMetrics.density).toInt()
                            setPadding(padding, padding, padding, padding)
                            
                            val layout = LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                            }
                            
                            val textLayout = LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }
                            
                            val tvSn = TextView(context).apply {
                                text = task.serialNumber
                                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                                textSize = 14f
                                paint.isFakeBoldText = true
                            }
                            
                            val tvStatus = TextView(context).apply {
                                text = if (task.status == "QC_PENDING") "Antrean Baru" else "Siap Uji Ulang"
                                setTextColor(ContextCompat.getColor(context, if (task.status == "QC_PENDING") R.color.secondary else R.color.tertiary))
                                textSize = 11f
                                setPadding(0, 4, 0, 0)
                            }
                            
                            textLayout.addView(tvSn)
                            textLayout.addView(tvStatus)
                            layout.addView(textLayout)
                            
                            val ivNext = ImageView(context).apply {
                                setImageResource(android.R.drawable.ic_media_next)
                                imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.outline))
                            }
                            layout.addView(ivNext)
                            
                            addView(layout)
                            
                            setOnClickListener {
                                if (role != "SUPERVISOR") {
                                    etSerialNumber.setText(task.serialNumber)
                                    openQcQuestionnaireDialog(task, taskDao, db, btnRepair)
                                }
                            }
                        }
                        llQueueContainer.addView(itemCard)
                    }
                }
            }
        }

        btnPass.setOnClickListener {
            val sn = etSerialNumber.text.toString().trim()
            if (sn.isEmpty()) {
                Toast.makeText(context, "Masukkan atau pilih Serial Number dari antrean!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            lifecycleScope.launch {
                val task = taskDao.getTaskBySn(sn)
                if (task != null && (task.status == "QC_PENDING" || task.status == "READY_FOR_RETEST" || task.status == "QC_REJECTED")) {
                    openQcQuestionnaireDialog(task, taskDao, db, btnRepair)
                } else {
                    Toast.makeText(context, "Unit dengan SN tersebut tidak berada dalam antrean uji atau sudah lulus!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRepair.setOnClickListener {
            val sn = etSerialNumber.text.toString().trim()
            if (sn.isEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                val task = taskDao.getTaskBySn(sn)
                task?.let {
                    it.status = "READY_FOR_RETEST"
                    taskDao.updateTask(it)

                    // Log ke Feed
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    db.operationDao().insertOperation(Operation(
                        title = "Perbaikan Selesai",
                        description = "SN: ${task.serialNumber} telah diperbaiki & siap diuji ulang.",
                        timestamp = time,
                        type = "SUCCESS"
                    ))

                    btnRepair.visibility = View.GONE
                    Toast.makeText(context, "UC-05: Perbaikan Selesai -> Status: Siap Uji Ulang", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openQcQuestionnaireDialog(
        task: ProductionTask,
        taskDao: com.example.infrastruktur.data.ProductionTaskDao,
        db: AppDatabase,
        btnRepair: MaterialButton
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qc_questions, null)
        val rgQ1 = dialogView.findViewById<RadioGroup>(R.id.rg_q1)
        val rgQ2 = dialogView.findViewById<RadioGroup>(R.id.rg_q2)
        val rgQ3 = dialogView.findViewById<RadioGroup>(R.id.rg_q3)
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Kirim Hasil Uji") { _, _ ->
                val checkedQ1 = rgQ1.checkedRadioButtonId
                val checkedQ2 = rgQ2.checkedRadioButtonId
                val checkedQ3 = rgQ3.checkedRadioButtonId

                if (checkedQ1 == -1 || checkedQ2 == -1 || checkedQ3 == -1) {
                    Toast.makeText(context, "Gagal: Semua pertanyaan pengujian harus dijawab!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Check if all answers are correct
                val isCorrect1 = checkedQ1 == R.id.rb_q1_correct
                val isCorrect2 = checkedQ2 == R.id.rb_q2_correct
                val isCorrect3 = checkedQ3 == R.id.rb_q3_correct

                val isPassed = isCorrect1 && isCorrect2 && isCorrect3

                lifecycleScope.launch {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    if (isPassed) {
                        task.status = "QC_PASSED"
                        taskDao.updateTask(task)

                        // Log ke Feed
                        db.operationDao().insertOperation(Operation(
                            title = "QC Lulus (Manual)",
                            description = "SN: ${task.serialNumber} lulus uji kelayakan (Jawaban Benar).",
                            timestamp = time,
                            type = "SUCCESS"
                        ))

                        btnRepair.visibility = View.GONE
                        Toast.makeText(context, "Hasil: QC PASSED! Unit siap dikemas.", Toast.LENGTH_LONG).show()
                    } else {
                        task.status = "QC_REJECTED"
                        taskDao.updateTask(task)

                        // Log ke Feed
                        db.operationDao().insertOperation(Operation(
                            title = "QC Gagal (Manual)",
                            description = "SN: ${task.serialNumber} gagal uji kelayakan (Rejected).",
                            timestamp = time,
                            type = "ERROR"
                        ))

                        btnRepair.visibility = View.VISIBLE
                        Toast.makeText(context, "Hasil: QC REJECTED! Silakan lakukan perbaikan.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
