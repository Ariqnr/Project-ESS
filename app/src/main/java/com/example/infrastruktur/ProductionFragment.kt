package com.example.infrastruktur

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

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
        
        // Tombol Buka Panduan Perakitan
        view.findViewById<MaterialButton>(R.id.btn_start_assembly).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AssemblyFragment())
                .addToBackStack(null)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit()
        }

        // Tombol Mulai Pengujian QC
        view.findViewById<MaterialButton>(R.id.btn_start_qc).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, QualityControlFragment())
                .addToBackStack(null)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit()
        }
    }
}
