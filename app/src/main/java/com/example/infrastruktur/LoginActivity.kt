package com.example.infrastruktur

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val roleToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.roleToggleGroup)
        val btnAuthenticate = findViewById<MaterialButton>(R.id.btn_authenticate)
        val etOperatorId = findViewById<TextInputEditText>(R.id.operatorId)
        val etAccessCode = findViewById<TextInputEditText>(R.id.accessCode)

        btnAuthenticate.setOnClickListener {
            val operatorId = etOperatorId.text.toString().trim()
            val accessCode = etAccessCode.text.toString().trim()

            val selectedRoleId = roleToggleGroup.checkedButtonId
            val role = when (selectedRoleId) {
                R.id.btnRoleOperator -> "OPERATOR"
                R.id.btnRoleWarehouse -> "WAREHOUSE"
                R.id.btnRoleSupervisor -> "SUPERVISOR"
                else -> "OPERATOR"
            }

            val expectedUserId = when (role) {
                "OPERATOR" -> "OP123"
                "WAREHOUSE" -> "WH888"
                "SUPERVISOR" -> "SP999"
                else -> "OP123"
            }
            val expectedAccessCode = "123456"

            var isValid = true
            if (operatorId.isEmpty()) {
                etOperatorId.error = "User ID tidak boleh kosong!"
                isValid = false
            } else if (operatorId != expectedUserId) {
                etOperatorId.error = "User ID salah untuk peran ini!"
                isValid = false
            }

            if (accessCode.isEmpty()) {
                etAccessCode.error = "Access Code tidak boleh kosong!"
                isValid = false
            } else if (accessCode != expectedAccessCode) {
                etAccessCode.error = "Access Code salah!"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // Save role in session (SharedPreferences for simplicity)
            val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("USER_ROLE", role)
                putString("OPERATOR_ID", operatorId)
                apply()
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
