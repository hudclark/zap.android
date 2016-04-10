package com.octopusbeach.zap

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.firebase.client.DataSnapshot
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener

/**
 * Created by hudson on 4/10/16.
 */
class NotificationService : NotificationListenerService() {
    private lateinit var context: Context
    val TITLE = "android.title"
    val TEXT = "android.text"

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification.extras
        val title = extras.getString(TITLE)
        val text = extras.getString(TEXT)
        push(title, text)
    }

    private fun push(title:String, text:String){
        val ref = (context as ZapApplication).getRef()
        val uid = ref.auth?.uid ?: return

        Firebase(ZapApplication.FIREBASE_ROOT + "/presence/" + uid).
                addListenerForSingleValueEvent(object:ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) { // logged into chrome
                            val noteRef = ref.child("notifications/" + uid)
                            noteRef.push().setValue(mapOf(Pair("title", title), Pair("text", text)))
                        }
                    }
                    override fun onCancelled(p0: FirebaseError?) {
                        // na
                    }
                })
    }
}