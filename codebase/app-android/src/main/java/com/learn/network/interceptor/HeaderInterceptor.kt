package com.learn.network.interceptor

import com.learn.BuildConfig
import com.learn.prefence.PreferenceProvider
import com.learn.util.Constant.DEVICE_TYPE
import com.learn.util.Constant.LANGUAGE
import com.learn.util.Constant.USER_TOKEN
import com.learn.util.DeviceUtil
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HeaderInterceptor(
    private val deviceUtil: DeviceUtil,
    val preferenceProvider: PreferenceProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        return chain.run {
            var userAgent = ""
            var languageId = ""
            var institute = ""
            var authToken = ""
            val deviceName = deviceUtil.getDeviceName()
            deviceUtil.getUserAgent()?.let { userAgent = it }
            preferenceProvider.getString(LANGUAGE_ID)?.let { languageId = it }
            preferenceProvider.getString(INSTITUTE_ID)?.let { institute = it }
            preferenceProvider.getString(INSTITUTE_ID)?.let { institute = it }
            preferenceProvider.getString(USER_TOKEN)?.let { authToken = it }

            val builder: Request.Builder = request().newBuilder()
                .addHeader("Application-Name", "android")
                .addHeader("x-device-type", DEVICE_TYPE)
                .addHeader("x-device-id", deviceUtil.getDeviceId())
                .addHeader("x-language-id", languageId)
                .addHeader("x-access-active-institute", institute)
                .addHeader("x-access-token", authToken)
                .addHeader("x-device-name", deviceName)
                .addHeader("user-agent", userAgent)

            builder.addHeader("Application-Version", BuildConfig.VERSION_CODE.toString())
            proceed(builder.build())
        }
    }
}
