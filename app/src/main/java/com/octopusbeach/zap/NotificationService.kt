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
import android.os.Bundle
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.util.Base64
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
    private val EMPTY_VIBRATE = longArrayOf(0)
    private val ANDROID = "android"
    private val NOTIFICATION_COUNT = "note_count"
    private val SHOWN_NOTIFICATION = "shown"

    private var askedForRating = false
    private var notificationsServed = 0

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // check to see if we still need to serve the rating notification
        askedForRating = prefs.getBoolean(SHOWN_NOTIFICATION, false)
        if (!askedForRating)
            notificationsServed = prefs.getInt(NOTIFICATION_COUNT, 0)
    }

    /*
    Note that this is not always going to be called when the service stops on something like
    a restart. However, it's non-critical and will simply delay the time until we ask for a review.
     */
    override fun onDestroy() {
        if (!askedForRating) {
            val edit = PreferenceManager.getDefaultSharedPreferences(context).edit()
            edit.putInt(NOTIFICATION_COUNT, notificationsServed)
            edit.apply()
        }
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = validate(sbn)
        if (extras != null) {
            val text = extras.get(TEXT).toString()
            val title = extras.getString(TITLE)
            push(sbn!!.packageName, title, text)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun validate(sbn:StatusBarNotification?): Bundle? {
        if (sbn == null) return null
        if (sbn.packageName == ANDROID) return null
        val note = sbn.notification
        if (note.sound == null && (note.vibrate == null || note.vibrate == EMPTY_VIBRATE)) return null
        if (!askedForRating) {
            notificationsServed++
            if (notificationsServed > 30) createRatingNotification()
        }
        return note.extras
    }

    private fun pushNote(title: String, text: String, safePackageName:String) {
        val ref = (context as ZapApplication).getRef()
        val uid = ref.auth?.uid ?: return
        //TODO create a single listener for presence that starts/stop with service lifecycle.
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

    private fun createRatingNotification() {
        askedForRating = true // this will stop counting notifications
        // save it now to make sure we don't ever ask again.
        val edit = PreferenceManager.getDefaultSharedPreferences(context).edit()
        edit.putBoolean(SHOWN_NOTIFICATION, true)
        edit.apply()

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=com.octopusbeach.zap")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val builder = NotificationCompat.Builder(context)
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