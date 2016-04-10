package com.octopusbeach.zap.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatButton
import com.firebase.client.Firebase
import com.octopusbeach.zap.NotificationService
import com.octopusbeach.zap.R
import com.octopusbeach.zap.ZapApplication

class MainActivity : AppCompatActivity() {

    private lateinit var btnConnect: AppCompatButton
    private lateinit var ref:Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ref = (application as ZapApplication).getRef()
        if (ref.auth == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnConnect = findViewById(R.id.btn_connect) as AppCompatButton
        btnConnect.setOnClickListener { toggleConnection() }
    }

    private fun toggleConnection() {
        if (ref.auth == null) {
            signOut()
            return
        }

        if (btnConnect.text == getString(R.string.connect)) {
            startService(Intent(this, NotificationService::class.java))
            btnConnect.text = getString(R.string.disconnect)
        } else if (btnConnect.text == getString(R.string.disconnect))  {
            stopService(Intent(this, NotificationService::class.java))
            btnConnect.text = getString(R.string.connect)
        }
    }

    private fun signOut() {
        stopService(Intent(this, NotificationService::class.java))
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
