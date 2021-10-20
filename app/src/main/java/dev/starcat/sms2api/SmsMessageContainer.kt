package dev.starcat.sms2api

import android.telephony.SmsMessage

class SmsMessageContainer(
    var timestampMillis: Long,
    var originalMessage: SmsMessage,
    var messageBody: String,
    var originatingAddress: String,
    var serviceCenterAddress: String) {

    var userDefinedId: String? = null
    var apiKey: String? = null
}
