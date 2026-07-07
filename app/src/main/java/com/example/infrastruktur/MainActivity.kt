package com.example.infrastruktur

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.infrastruktur.data.AppDatabase
import com.example.infrastruktur.data.InventoryItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var userRole: String = "OPERATOR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userRole = sharedPref.getString("USER_ROLE", "OPERATOR") ?: "OPERATOR"
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.itemActiveIndicatorColor = ContextCompat.getColorStateList(this, R.color.secondary_container)
        setupNavigation(bottomNavigation)
        
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)
        fabAdd.setOnClickListener {
            loadFragment(ScanFragment())
        }

        val btnLogout = findViewById<View>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            
            val intent = android.content.Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment != null) {
                val isMainFragment = currentFragment is DashboardFragment || 
                                     currentFragment is OperationsFragment || 
                                     currentFragment is ProductionFragment || 
                                     currentFragment is SupervisorFragment ||
                                     currentFragment is RegistryFragment
                setBottomNavigationAndFabVisible(isMainFragment)
            }
        }

        seedDatabase()
    }

    private fun seedDatabase() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            // Seed Raw Material
            if (db.inventoryDao().getItemBySku("RAW-STEEL-01") == null) {
                db.inventoryDao().insertItem(InventoryItem("RAW-STEEL-01", "Pelat Baja Tipe-B", 0, "RAW"))
            }
            // Seed Finished Good placeholder
            if (db.inventoryDao().getItemBySku("FIN-PANEL-X2") == null) {
                db.inventoryDao().insertItem(InventoryItem("FIN-PANEL-X2", "Panel-X2 Jadi", 0, "FINISHED"))
            }
        }
    }

    private fun setupNavigation(nav: BottomNavigationView) {
        val menu = nav.menu
        when (userRole) {
            "OPERATOR" -> {
                menu.findItem(R.id.navigation_warehouse).isVisible = false
                menu.findItem(R.id.navigation_production).isVisible = true
                menu.findItem(R.id.navigation_monitoring).isVisible = false
                menu.findItem(R.id.navigation_registry).isVisible = true
            }
            "WAREHOUSE" -> {
                menu.findItem(R.id.navigation_production).isVisible = false
                menu.findItem(R.id.navigation_warehouse).isVisible = true
                menu.findItem(R.id.navigation_monitoring).isVisible = false
                menu.findItem(R.id.navigation_registry).isVisible = true
            }
            "SUPERVISOR" -> {
                menu.findItem(R.id.navigation_production).isVisible = true
                menu.findItem(R.id.navigation_warehouse).isVisible = true
                menu.findItem(R.id.navigation_monitoring).isVisible = true
                menu.findItem(R.id.navigation_registry).isVisible = true
            }
        }

        nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.navigation_dashboard -> DashboardFragment()
                R.id.navigation_warehouse -> OperationsFragment()
                R.id.navigation_production -> ProductionFragment()
                R.id.navigation_monitoring -> SupervisorFragment()
                R.id.navigation_registry -> RegistryFragment()
                else -> DashboardFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    fun setBottomNavigationAndFabVisible(visible: Boolean) {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add)
        bottomNavigation?.visibility = if (visible) View.VISIBLE else View.GONE
        fabAdd?.visibility = if (visible) View.VISIBLE else View.GONE
        
        val fragmentContainer = findViewById<View>(R.id.fragment_container)
        val paddingBottom = if (visible) {
            (80 * resources.displayMetrics.density).toInt()
        } else {
            0
        }
        fragmentContainer?.setPadding(0, 0, 0, paddingBottom)
    }

    private fun loadFragment(fragment: Fragment) {
        val isMainFragment = fragment is DashboardFragment || 
                             fragment is OperationsFragment || 
                             fragment is ProductionFragment || 
                             fragment is SupervisorFragment ||
                             fragment is RegistryFragment
        
        setBottomNavigationAndFabVisible(isMainFragment)

        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            
        if (!isMainFragment) {
            transaction.addToBackStack(null)
        }
        
        transaction.commit()
    }
    
    fun getUserRole(): String = userRole
}
