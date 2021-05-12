@file:Suppress("BlockingMethodInNonBlockingContext")

package com.learn.network

import com.learn.util.ApiException
import com.learn.util.Constant
import com.learn.util.MultipleAuthentication
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response


abstract class SafeApiRequest {

    suspend fun <T : Any> apiRequest(call: suspend () -> Response<T>): T {
        val response = call.invoke()
        if (response.isSuccessful) {
            return response.body()!!
        } else {
            val error = response.errorBody()?.string()
            val message = StringBuilder()
            var errNo: String? = null
            var code: String? = null
            error?.let {
                try {
                    code = JSONObject(it).getString("code")
                    message.append(JSONObject(it).getString("message"))
                    errNo = JSONObject(it).getString("errno")
                } catch (e: JSONException) {
                } finally {
                    message.append("\n")
                }
            }
            if (code == Constant.USER_UNAUTHORIZED) {
                MultipleAuthentication().bottomSheetAuthentication()
            }
            throw ApiException(message.toString(), errNo, response.code())
        }
    }
}
