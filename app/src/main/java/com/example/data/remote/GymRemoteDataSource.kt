package com.example.data.remote

import android.content.Context
import com.example.data.remote.dto.*

class GymRemoteDataSource(private val context: Context) {

    private val apiService: ApiService
        get() = ApiClient.getApiService(context)

    private val tokenManager: TokenManager
        get() = TokenManager.getInstance(context)

    suspend fun login(email: String, password: String,type: String, deviceName: String = "android"): LoginApiResult {
        val request = LoginRequest(
            email = email.trim(),
            password = password,
            type=type,
            device_name = deviceName
        )
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                val loginResponse = ApiClient.parseObject(bodyString, LoginResponse::class.java)
                val token = loginResponse?.resolveToken()
                if (!token.isNullOrBlank()) {
                    tokenManager.saveFullSession(
                        token = token,
                        tokenType = loginResponse.token_type ?: "Bearer",
                        user = loginResponse.user,
                        business = loginResponse.business
                    )
                    LoginApiResult.Success(loginResponse)
                } else {
                    LoginApiResult.InvalidCredentials("Failed to parse authentication token from server.")
                }
            } else {
                val errorString = response.errorBody()?.string() ?: ""
                var parsedMsg = "The provided credentials are incorrect."
                try {
                    if (errorString.isNotBlank()) {
                        val json = org.json.JSONObject(errorString)
                        if (json.has("message")) {
                            parsedMsg = json.getString("message")
                        } else if (json.has("errors")) {
                            val errorsObj = json.optJSONObject("errors")
                            if (errorsObj != null && errorsObj.has("email")) {
                                val emailErrors = errorsObj.optJSONArray("email")
                                if (emailErrors != null && emailErrors.length() > 0) {
                                    parsedMsg = emailErrors.getString(0)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore JSON parse error
                }

                if (response.code() == 401 || response.code() == 422 || response.code() == 403) {
                    LoginApiResult.InvalidCredentials(parsedMsg, errorString)
                } else {
                    LoginApiResult.NetworkUnavailable("Server returned HTTP ${response.code()}: $parsedMsg")
                }
            }
        } catch (e: Exception) {
            LoginApiResult.NetworkUnavailable(e.message ?: "Network unavailable")
        }
    }

    suspend fun logout(): Boolean {
        try {
            ApiClient.safeApiCall("auth/logout", {
                apiService.logout()
            }) { true }
        } finally {
            tokenManager.clearToken()
        }
        return true
    }

    suspend fun getCurrentUser(): UserDto? {
        return ApiClient.safeApiCall("auth/me", {
            apiService.getCurrentUser()
        }) { json ->
            val authMe = ApiClient.parseObject(json, AuthMeResponse::class.java)
            authMe?.user ?: ApiClient.parseObject(json, UserDto::class.java)
        }
    }

    suspend fun getAuthMe(): AuthMeResponse? {
        return ApiClient.safeApiCall("auth/me", {
            apiService.getCurrentUser()
        }) { json ->
            ApiClient.parseObject(json, AuthMeResponse::class.java)
        }
    }

    fun hasAuthToken(): Boolean = tokenManager.hasToken()
    fun getStoredToken(): String? = tokenManager.getToken()

    suspend fun getMembers(
        query: String? = null,
        status: String? = null,
        page: Int? = null
    ): List<MemberDto>? {
        return ApiClient.safeApiCall("members", {
            apiService.getMembers(query, status, page)
        }) { json ->
            ApiClient.parseList(json, MemberDto::class.java)
        }
    }

    suspend fun getDashboard(): DashboardResponse? {
        return ApiClient.safeApiCall("GET dashboard", {
            apiService.getDashboard()
        }) { json ->
            ApiClient.parseObject(json, DashboardResponse::class.java)
        }
    }

    /**
     * Calls the export endpoint (dynamic URL or relative to ApiConstants.resolvedBaseUrl)
     * and returns the raw ResponseBody byte stream if successful.
     */
    suspend fun exportMembers(endpointOrUrl: String = ApiConstants.EXPORT_MEMBERS_ENDPOINT): okhttp3.ResponseBody? {
        val targetUrl = if (endpointOrUrl.startsWith("http://") || endpointOrUrl.startsWith("https://")) {
            endpointOrUrl
        } else {
            val base = ApiConstants.resolvedBaseUrl
            if (base.endsWith("/") && endpointOrUrl.startsWith("/")) {
                base + endpointOrUrl.substring(1)
            } else if (!base.endsWith("/") && !endpointOrUrl.startsWith("/")) {
                "$base/$endpointOrUrl"
            } else {
                base + endpointOrUrl
            }
        }

        return try {
            val response = apiService.exportMembers(targetUrl)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createMember(request: CreateMemberRequest): MemberDto? {
        return ApiClient.safeApiCall("POST members", {
            apiService.createMember(request)
        }) { json ->
            ApiClient.parseObject(json, MemberDto::class.java)
        }
    }

    suspend fun getMemberById(id: Long): MemberDto? {
        return ApiClient.safeApiCall("GET members/$id", {
            apiService.getMemberById(id)
        }) { json ->
            ApiClient.parseObject(json, MemberDto::class.java)
        }
    }

    suspend fun updateMember(id: Long, request: UpdateMemberRequest): MemberDto? {
        return ApiClient.safeApiCall("PUT members/$id", {
            apiService.updateMember(id, request)
        }) { json ->
            ApiClient.parseObject(json, MemberDto::class.java)
        }
    }

    suspend fun patchMember(id: Long, request: UpdateMemberRequest): MemberDto? {
        return ApiClient.safeApiCall("PATCH members/$id", {
            apiService.patchMember(id, request)
        }) { json ->
            ApiClient.parseObject(json, MemberDto::class.java)
        }
    }

    suspend fun deleteMember(id: Long): Boolean {
        val result = ApiClient.safeApiCall("DELETE members/$id", {
            apiService.deleteMember(id)
        }) { true }
        return result ?: false
    }

    suspend fun getPlans(): List<PlanDto>? {
        return ApiClient.safeApiCall("GET plans", {
            apiService.getPlans()
        }) { json ->
            ApiClient.parseList(json, PlanDto::class.java)
        }
    }

    suspend fun createPlan(request: CreatePlanRequest): PlanDto? {
        return ApiClient.safeApiCall("POST plans", {
            apiService.createPlan(request)
        }) { json ->
            ApiClient.parseObject(json, PlanDto::class.java)
        }
    }

    suspend fun getPlanById(id: Long): PlanDto? {
        return ApiClient.safeApiCall("GET plans/$id", {
            apiService.getPlanById(id)
        }) { json ->
            ApiClient.parseObject(json, PlanDto::class.java)
        }
    }

    suspend fun deletePlan(id: Long): Boolean {
        val result = ApiClient.safeApiCall("DELETE plans/$id", {
            apiService.deletePlan(id)
        }) { true }
        return result ?: false
    }

    suspend fun getSubscriptions(memberId: Long? = null, page: Int? = null): List<SubscriptionDto>? {
        return ApiClient.safeApiCall("GET subscriptions", {
            apiService.getSubscriptions(memberId, page)
        }) { json ->
            ApiClient.parseList(json, SubscriptionDto::class.java)
        }
    }

    suspend fun createSubscription(request: CreateSubscriptionRequest): SubscriptionDto? {
        return ApiClient.safeApiCall("POST subscriptions", {
            apiService.createSubscription(request)
        }) { json ->
            ApiClient.parseObject(json, SubscriptionDto::class.java)
        }
    }

    suspend fun getSubscriptionById(id: Long): SubscriptionDto? {
        return ApiClient.safeApiCall("GET subscriptions/$id", {
            apiService.getSubscriptionById(id)
        }) { json ->
            ApiClient.parseObject(json, SubscriptionDto::class.java)
        }
    }

    suspend fun deleteSubscription(id: Long): Boolean {
        val result = ApiClient.safeApiCall("DELETE subscriptions/$id", {
            apiService.deleteSubscription(id)
        }) { true }
        return result ?: false
    }

    suspend fun getAttendance(date: String? = null, page: Int? = null): List<AttendanceDto>? {
        return ApiClient.safeApiCall("GET attendance", {
            apiService.getAttendance(date, page)
        }) { json ->
            ApiClient.parseList(json, AttendanceDto::class.java)
        }
    }

    suspend fun markAttendance(tokenOrId: String): AttendanceDto? {
        val idLong = tokenOrId.trim().toLongOrNull()
        val request = if (idLong != null) {
            AttendanceRequest(member_id = idLong)
        } else {
            AttendanceRequest(token = tokenOrId.trim())
        }

        return ApiClient.safeApiCall("POST attendance", {
            apiService.markAttendance(request)
        }) { json ->
            ApiClient.parseObject(json, AttendanceDto::class.java)
        }
    }

    suspend fun getPayments(memberId: Long? = null, page: Int? = null): List<PaymentDto>? {
        return ApiClient.safeApiCall("GET payments", {
            apiService.getPayments(memberId, page)
        }) { json ->
            ApiClient.parseList(json, PaymentDto::class.java)
        }
    }

    suspend fun createPayment(request: CreatePaymentRequest): PaymentDto? {
        return ApiClient.safeApiCall("POST payments", {
            apiService.createPayment(request)
        }) { json ->
            ApiClient.parseObject(json, PaymentDto::class.java)
        }
    }

    suspend fun getPaymentRequests(page: Int? = null): List<PaymentRequestDto>? {
        return ApiClient.safeApiCall("GET payment-requests", {
            apiService.getPaymentRequests(page)
        }) { json ->
            ApiClient.parseList(json, PaymentRequestDto::class.java)
        }
    }

    suspend fun createPaymentRequest(request: CreatePaymentRequestDto): PaymentRequestDto? {
        return ApiClient.safeApiCall("POST payment-requests", {
            apiService.createPaymentRequest(request)
        }) { json ->
            ApiClient.parseObject(json, PaymentRequestDto::class.java)
        }
    }

    suspend fun getPaymentRequestById(id: Long): PaymentRequestDto? {
        return ApiClient.safeApiCall("GET payment-requests/$id", {
            apiService.getPaymentRequestById(id)
        }) { json ->
            ApiClient.parseObject(json, PaymentRequestDto::class.java)
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ProfileResponse? {
        return ApiClient.safeApiCall("PUT profile", {
            apiService.updateProfile(request)
        }) { json ->
            ApiClient.parseObject(json, ProfileResponse::class.java)
        }
    }

    suspend fun patchProfile(request: UpdateProfileRequest): ProfileResponse? {
        return ApiClient.safeApiCall("PATCH profile", {
            apiService.patchProfile(request)
        }) { json ->
            ApiClient.parseObject(json, ProfileResponse::class.java)
        }
    }

    suspend fun sendRazorpayWebhook(signature: String, jsonPayload: String): Boolean {
        val body = ApiClient.createJsonRequestBody(jsonPayload)
        val result = ApiClient.safeApiCall("POST webhooks/razorpay", {
            apiService.postRazorpayWebhook(signature, body)
        }) { true }
        return result ?: false
    }
}
