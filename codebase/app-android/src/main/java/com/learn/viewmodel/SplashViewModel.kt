package com.learn.viewmodel

import androidx.lifecycle.ViewModel
import com.learn.repository.SplashRepository
import com.learn.util.DeviceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplashViewModel(
    private val deviceUtil: DeviceUtil,
    private val repository: SplashRepository
) : ViewModel() {
    suspend fun getPhoneNumber() = withContext(Dispatchers.IO) {
        repository.getPhoneNumber(deviceUtil.getDeviceId())
    }

    suspend fun updateDeviceInfo() = withContext(Dispatchers.IO) {
        repository.getPhoneNumber(deviceUtil.getDeviceId())
    }
}
