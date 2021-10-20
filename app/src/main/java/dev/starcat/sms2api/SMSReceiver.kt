package dev.starcat.sms2api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import java.util.*

class SMSReceiver: BroadcastReceiver() {

    private val tag = this.javaClass.simpleName

    init {
        Log.d(tag, "Created.")
    }

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent != null && Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {

                Log.d(tag, "Message received: ${smsMessage}")

                val messageContainer = SmsMessageContainer(
                    smsMessage.timestampMillis,
                    smsMessage,
                    smsMessage.displayMessageBody,
                    smsMessage.displayOriginatingAddress,
                    smsMessage.serviceCenterAddress
                )

                Log.d(tag, "Message body: ${messageContainer.messageBody}")
                Log.d(tag, "Originating address: ${messageContainer.originatingAddress}")
                Log.d(tag, "Service center address: ${messageContainer.serviceCenterAddress}")

                Store.processMessage(messageContainer)
            }
        }
    }
}