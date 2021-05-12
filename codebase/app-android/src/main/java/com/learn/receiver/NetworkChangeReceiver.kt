package com.learn.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.learn.util.isInternetAvailable

class NetworkChangeReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    companion object {
        private val mIsConnected: MutableLiveData<Boolean> by lazy {
            MutableLiveData<Boolean>()
        }

        fun get(): MutableLiveData<Boolean> {
            return mIsConnected
        }
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        val isConnected: Boolean? = context?.let { isInternetAvailable(it) }
        mIsConnected.postValue(isConnected)
    }
}
