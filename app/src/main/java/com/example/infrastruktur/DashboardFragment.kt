package com.example.infrastruktur

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

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
        
        // Navigasi ke Halaman Logistik/Delivery dari Dashboard
        view.findViewById<MaterialCardView>(R.id.card_logistics).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DeliveryFragment())
                .addToBackStack(null)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit()
        }
    }
}
