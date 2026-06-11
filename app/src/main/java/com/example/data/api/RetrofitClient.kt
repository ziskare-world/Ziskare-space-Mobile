package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var apiInstance: ZiskareSpaceApi? = null
    private var lastUrl: String? = null
    private var lastToken: String? = null
    private var lastCsrf: String? = null
    private var lastSessionId: String? = null

    @Synchronized
    fun getApi(
        baseUrl: String,
        token: String? = null,
        csrf: String? = null,
        sessionId: String? = null
    ): ZiskareSpaceApi {
        // Recreate only if essential properties change (such as server URL shifts)
        val urlToUse = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (apiInstance == null || 
            lastUrl != urlToUse || 
            lastToken != token || 
            lastCsrf != csrf || 
            lastSessionId != sessionId
        ) {
            val okHttpClient = OkHttpClient.Builder().apply {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                writeTimeout(20, TimeUnit.SECONDS)

                addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    
                    if (!token.isNullOrEmpty()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                    if (!csrf.isNullOrEmpty()) {
                        requestBuilder.header("X-CSRF-Token", csrf)
                    }
                    if (!sessionId.isNullOrEmpty()) {
                        requestBuilder.header("Cookie", "sessionId=$sessionId")
                    }
                    
                    chain.proceed(requestBuilder.build())
                }

                // Append developer logging
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                addInterceptor(logging)
            }.build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(urlToUse)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiInstance = retrofit.create(ZiskareSpaceApi::class.java)
            lastUrl = urlToUse
            lastToken = token
            lastCsrf = csrf
            lastSessionId = sessionId
        }
        return apiInstance!!
    }
}
