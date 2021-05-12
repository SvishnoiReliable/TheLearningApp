package com.dizvik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.util.regex.Pattern



class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val liveData: MutableLiveData<String> by lazy {
            MutableLiveData<String>()
        }

        fun get(): MutableLiveData<String> {
            return liveData
        }
    }

    override fun onReceive(p0: Context?, intent: Intent?) {

        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras
            val status: Status? = extras!![SmsRetriever.EXTRA_STATUS] as Status?
            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val message = extras[SmsRetriever.EXTRA_SMS_MESSAGE] as CharSequence
                    val pattern = Pattern.compile("(|^)\\d{4}")
                    val matcher = pattern.matcher(message)
                    if (matcher.find()) {
                        liveData.value = matcher.group(0)
                    }
                }
            }
        }
    }
}
