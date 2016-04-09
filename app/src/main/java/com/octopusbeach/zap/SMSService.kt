package com.octopusbeach.zap

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.firebase.client.DataSnapshot
import com.firebase.client.Firebase
import com.firebase.client.FirebaseError
import com.firebase.client.ValueEventListener
import com.octopusbeach.zap.ui.MainActivity

/**
 * Created by hudson on 4/9/16.
 */
class SMSService : Service(){

    var ref : Firebase? = null
    var receiver:SMSReceiver? = null
    val binder = LocalBinder()
    val TAG = "SMSService"
    val NOT_ID = 1
    private lateinit var manager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onCreate() {
        ref = (application as ZapApplication).getRef()
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connect()
        createNotification()
        //TODO create listener for new messages coming from the backend.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // time to disconnect.
        disconnect()
        removeNotification()
    }

    /**
     * Create a persistent notification to show that we are listening for new data
     */
    private fun createNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_NO_CREATE)
        val builder = NotificationCompat.Builder(this)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.content_title))
            .setContentText(getString(R.string.content_text))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
        manager.notify(NOT_ID, builder.build())
    }


    private fun removeNotification() {
        manager.cancel(NOT_ID)
    }

    /**
     * Connect to Firebase and start sending texts
     */
    private fun connect() {
        Log.d(TAG, "connecting...")
        receiver = SMSReceiver()
        val filter = IntentFilter(SMSReceiver.SMS_ACTION)
        registerReceiver(receiver, filter)
        // set up a listener for user's presence
        val connectedRef = ref?.child("presence/" + ref?.auth?.uid)
        ref?.child(".info/connected")?.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snap: DataSnapshot?) {
                if (snap !=null ){
                    connectedRef?.onDisconnect()?.removeValue()
                    connectedRef?.setValue(true)
                }
            }
            override fun onCancelled(p0: FirebaseError?) {}
        })
    }

    /**
     * Stops the direct connection to firebase.
     */
    private fun disconnect() {
        Log.d(TAG, "disconnecting...")
        if (receiver != null) {
            try {
                unregisterReceiver(receiver)
            } catch(e:Exception) {
                Log.d("disconnect", "Receiver not registered")
            }
        }
        ref?.child("presence/" + ref?.auth?.uid)?.removeValue()
    }

    inner class LocalBinder: Binder() {
        fun getService(): SMSService = this@SMSService
    }
}