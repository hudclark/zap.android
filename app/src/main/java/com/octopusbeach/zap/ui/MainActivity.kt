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
        ref = (application as ZapApplication).getRef()
        if (ref.auth == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        setContentView(R.layout.activity_main)

        btnConnect = findViewById(R.id.btn_connect) as AppCompatButton
    }

    private fun signOut() {
        stopService(Intent(this, NotificationService::class.java))
        startActivity(Intent(this, LoginActivity::class.java))
    }
}
