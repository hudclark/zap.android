package com.octopusbeach.zap.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.firebase.client.Firebase
import com.octopusbeach.zap.R
import com.octopusbeach.zap.ZapApplication

class MainActivity : AppCompatActivity() {

    private val ref = Firebase(ZapApplication.FIREBASE_ROOT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ref.auth == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
