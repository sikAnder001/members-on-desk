package com.example.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        val isLoginEndpoint = path.endsWith("/auth/login") || path.endsWith("auth/login")
        val isRazorpayWebhook = path.contains("webhooks/razorpay")

        val builder = original.newBuilder()

        if (original.header("Content-Type") == null) {
            builder.header("Content-Type", "application/json")
        }
        if (original.header("Accept") == null) {
            builder.header("Accept", "application/json")
        }

        if (!isLoginEndpoint && !isRazorpayWebhook) {
            val rawToken = tokenManager.getToken()
            if (!rawToken.isNullOrBlank()) {
                val cleanToken = rawToken.trim()
                val authHeaderValue = if (cleanToken.startsWith("Bearer ", ignoreCase = true)) {
                    cleanToken
                } else {
                    "Bearer $cleanToken"
                }
                builder.header("Authorization", authHeaderValue)
            }
        }

        return chain.proceed(builder.build())
    }
}
