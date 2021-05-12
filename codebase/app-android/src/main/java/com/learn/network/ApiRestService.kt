package com.learn.network

import android.content.Context
import com.learn.R


interface ApiRestService {

    @POST("api/v1/tokens/send-otp")
    suspend fun sendOTP(
        @Body otpRequest: SendOTPRequest,
        @Header("x-otp-type") otp_type: String
    ): Response<SendOTPResponse>

    @POST("/api/v1/auth/otp/sign-up")
    suspend fun postSignup(
        @Header(KEY_FCM_TOKEN) fcmToken: String,
        @Body user: SignupRequest,
        @Query("instituteId") instituteId: String?
    ): Response<SignUpResponse>

    @POST("api/v1/auth/otp/sign-in")
    suspend fun userLogin(
        @Header(KEY_FCM_TOKEN) fcmToken: String,
        @Body loginRequest: LogInRequest
    ): Response<LoginResponse>

    @GET("api/v1/factory/home/{id}")
    suspend fun getHomeData(@Path("id") id: String): Response<HomeDataResponse>

   

    companion object {
        operator fun invoke(
            context: Context,
            networkInterceptor: NetworkInterceptor,
            headerInterceptor: HeaderInterceptor
        ): ApiRestService {

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val cacheSize = 20 * 1024 * 1024L // 20 MB

            fun provideCache(): Cache? {
                var cache: Cache? = null
                try {
                    cache = Cache(File(context.cacheDir, "Cache_directory"), cacheSize)
                } catch (e: Exception) {
                    e.message?.let { FirebaseCrashlytics.getInstance().log(it) }
                }
                return cache
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cache(provideCache())
                .addInterceptor(headerInterceptor)
                .addInterceptor(networkInterceptor)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder().baseUrl(context.getString(R.string.base_url))
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                .build()
                .create(ApiRestService::class.java)
        }
    }
}

