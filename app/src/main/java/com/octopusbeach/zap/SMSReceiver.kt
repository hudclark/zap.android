package com.octopusbeach.zap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsMessage
import com.octopusbeach.zap.model.SMS
import java.util.*

/**
 * Created by hudson on 4/9/16.
 */

class SMSReceiver : BroadcastReceiver() {
    companion object{
        val SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED"
    }
    val ID_INDEX = 0
    val PHONE_LOOKUP = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action.equals(SMS_ACTION)) {
            val ref = (context.applicationContext as ZapApplication).getRef()
            if (ref.auth != null) {
                val msgRef = ref.child("messages/" + ref.auth.uid)
                val messages = readSMS(intent, context)
                for (sms in messages) {
                    msgRef?.child(sms.name)?.push()?.setValue(sms)
                }
            }
        }
    }

    private fun readSMS(intent:Intent, context:Context):ArrayList<SMS> {
        val messages = ArrayList<SMS>()
        for (sms:SmsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val message = SMS(getNameForNumber(sms.originatingAddress, context), sms.displayMessageBody,
                    sms.originatingAddress)
            messages.add(message)
        }
        return messages
    }

    private fun getNameForNumber(number:String, context:Context):String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, number)
        val cursor = context.contentResolver.query(uri, PHONE_LOOKUP, null, null, null)
        if (cursor.moveToFirst()) {
            val name = cursor.getString(ID_INDEX)
            cursor.close()
            return name
        }
        return number
    }
}
