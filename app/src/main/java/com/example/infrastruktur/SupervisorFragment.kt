package com.example.infrastruktur

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class SupervisorFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_supervisor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        
        // Find Views
        val progressStockCircular = view.findViewById<CircularProgressIndicator>(R.id.progress_stock_circular)
        val tvStockTotalUnits = view.findViewById<TextView>(R.id.tv_stock_total_units)
        val tvStockRawPercentage = view.findViewById<TextView>(R.id.tv_stock_raw_percentage)

        val tvProductionPercentage = view.findViewById<TextView>(R.id.tv_production_percentage)
        val progressProductionTimeline = view.findViewById<LinearProgressIndicator>(R.id.progress_production_timeline)

        val tvChartPassedVal = view.findViewById<TextView>(R.id.tv_chart_passed_val)
        val viewChartPassedBar = view.findViewById<View>(R.id.view_chart_passed_bar)
        val tvChartRejectedVal = view.findViewById<TextView>(R.id.tv_chart_rejected_val)
        val viewChartRejectedBar = view.findViewById<View>(R.id.view_chart_rejected_bar)

        // 1. Observe Inventory for Warehouse stock status
        viewLifecycleOwner.lifecycleScope.launch {
            db.inventoryDao().getAllInventory().collectLatest { inventoryItems ->
                val rawSteel = inventoryItems.find { it.sku == "RAW-STEEL-01" }?.quantity ?: 0
                val finishedPanel = inventoryItems.find { it.sku == "FIN-PANEL-X2" }?.quantity ?: 0
                val totalStock = rawSteel + finishedPanel

                tvStockTotalUnits.text = String.format(Locale.getDefault(), "%,d Units", totalStock)

                if (totalStock > 0) {
                    val rawPercent = (rawSteel.toFloat() / totalStock * 100).toInt()
                    progressStockCircular.progress = rawPercent
                    tvStockRawPercentage.text = getString(R.string.welcome_supervisor).replace("Halo, Marcus", "Bahan Baku ($rawPercent%)")
                } else {
                    progressStockCircular.progress = 0
                    tvStockRawPercentage.text = "Bahan Baku (0%)"
                }
            }
        }

        // 2. Observe Production Tasks for timeline & quality chart
        viewLifecycleOwner.lifecycleScope.launch {
            db.productionTaskDao().getAllTasks().collectLatest { tasks ->
                // Timeline: Ratio of Finished + Shipped to Total tasks
                val totalTasks = tasks.size
                val completedTasks = tasks.filter { it.status in listOf("FINISHED", "SHIPPED") }.size

                if (totalTasks > 0) {
                    val completedPercent = (completedTasks.toFloat() / totalTasks * 100).toInt()
                    progressProductionTimeline.progress = completedPercent
                    tvProductionPercentage.text = String.format(Locale.getDefault(), "%d%%", completedPercent)
                } else {
                    progressProductionTimeline.progress = 0
                    tvProductionPercentage.text = "0%"
                }

                // Quality Chart: Passed (QC_PASSED, PACKED, FINISHED, SHIPPED) vs Rejected (QC_REJECTED, READY_FOR_RETEST)
                val passedCount = tasks.filter {
                    it.status in listOf("QC_PASSED", "PACKED", "FINISHED", "SHIPPED")
                }.size
                val rejectedCount = tasks.filter {
                    it.status in listOf("QC_REJECTED", "READY_FOR_RETEST")
                }.size

                tvChartPassedVal.text = passedCount.toString()
                tvChartRejectedVal.text = rejectedCount.toString()

                // Render Chart Bars Dynamically
                val maxBarHeightDp = 100
                val maxVal = maxOf(passedCount, rejectedCount, 1)

                val density = resources.displayMetrics.density
                val passedHeightPx = ((passedCount.toFloat() / maxVal) * maxBarHeightDp * density).toInt()
                val rejectedHeightPx = ((rejectedCount.toFloat() / maxVal) * maxBarHeightDp * density).toInt()

                val minHeightPx = (8 * density).toInt()

                viewChartPassedBar.layoutParams = viewChartPassedBar.layoutParams.apply {
                    height = maxOf(passedHeightPx, minHeightPx)
                }
                viewChartRejectedBar.layoutParams = viewChartRejectedBar.layoutParams.apply {
                    height = maxOf(rejectedHeightPx, minHeightPx)
                }
                
                // Force layouts to update
                viewChartPassedBar.requestLayout()
                viewChartRejectedBar.requestLayout()
            }
        }
    }
}