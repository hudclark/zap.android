package com.octopusbeach.zap

import android.app.Application
import com.firebase.client.Firebase

/**
 * Created by hudson on 4/9/16.
 */
class ZapApplication : Application() {
    companion object  {
        val FIREBASE_ROOT = "https://zap-extension.firebaseio.com"
    }

    private lateinit var ref:Firebase

    override fun onCreate() {
        super.onCreate()
        Firebase.setAndroidContext(this)
        ref = Firebase(FIREBASE_ROOT)
    }

    fun getRef() = ref
}