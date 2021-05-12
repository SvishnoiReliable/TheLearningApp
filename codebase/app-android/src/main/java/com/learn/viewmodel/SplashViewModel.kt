package com.dizvik.viewmodel

import androidx.lifecycle.ViewModel
import com.dizvik.repository.SplashRepository
import com.dizvik.util.DeviceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by Abhin.
 */
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
