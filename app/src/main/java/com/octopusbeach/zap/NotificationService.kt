package com.octopusbeach.zap

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.util.Base64
import android.util.Log
import com.firebase.client.DataSnapshot
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import java.io.ByteArrayOutputStream

/**
 * Created by hudson on 4/10/16.
 */
class NotificationService : NotificationListenerService() {
    private lateinit var context: Context
    private val TITLE = "android.title"
    private val TEXT = "android.text"
    private val ANDROID = "android"
    private val EMPTY_VIBRATE = longArrayOf(0)

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == ANDROID) return
        val note = sbn.notification
        // don't mirror a silent notification
        if (note.sound == null && (note.vibrate == null || note.vibrate == EMPTY_VIBRATE)) return
        val extras = note.extras
        val text = extras.get(TEXT).toString()
        val title = extras.getString(TITLE)
        push(sbn.packageName, title, text)
    }

    private fun pushNote(title: String, text: String, safePackageName:String) {
        val ref = (context as ZapApplication).getRef()
        val uid = ref.auth?.uid ?: return
        Firebase(ZapApplication.FIREBASE_ROOT + "/presence/" + uid).
                addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // logged into chrome
                            val noteRef = ref.child("notifications/" + uid)
                            noteRef.push().setValue(mapOf(Pair("title", title), Pair("text", text),
                                    Pair("app", safePackageName)))
                        }
                    }

                    override fun onCancelled(p0: FirebaseError?) {
                        // na
                    }
                })
    }

    private fun push(packageName: String, title:String, text:String) {
        val safeName = packageName.split('.').joinToString("")
        val iconRef = Firebase(ZapApplication.FIREBASE_ROOT + "/icons/" + safeName)
        iconRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // If the image has never been saved for this app, send it over now
                if (!snapshot.exists()) {
                    val img = getIcon(packageName)
                    if (!img.isEmpty()) // don't save a blank image
                        iconRef.setValue(img)
                }
                // push notification no matter what
                pushNote(title, text, safeName)
            }
            override fun onCancelled(p0: FirebaseError?) {
                //na
            }
        })
    }

    private fun getIcon(packageName: String): String {
        try {
            val icon = packageManager.getApplicationIcon(packageName)
            val map = (icon as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            map.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val file = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            return file
        } catch (e:Exception) {
            return ""
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun createRatingNotification() {
        // after 30 notifications have been served, ask for a review!
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=com.octopusbeach.zap")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)
        val builder = NotificationCompat.Builder(applicationContext)
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        builder.setLargeIcon(largeIcon)
            .setContentTitle("Like Zap?")
            .setContentText("Rate us in the app store!")
            .setSmallIcon(R.drawable.notification)
        builder.setContentIntent(pendingIntent)
        val note = builder.build()
        note.flags = Notification.FLAG_AUTO_CANCEL
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(0, note)
    }
}
