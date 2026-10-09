package com.example.data.remote

import com.example.data.remote.dto.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<ResponseBody>

    @POST("auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<ResponseBody>

    @GET("dashboard")
    suspend fun getDashboard(): Response<ResponseBody>

    @GET("members")
    suspend fun getMembers(
        @Query("q") query: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null
    ): Response<ResponseBody>

    @Streaming
    @GET
    suspend fun exportMembers(
        @Url url: String
    ): Response<ResponseBody>

    @POST("members")
    suspend fun createMember(
        @Body body: CreateMemberRequest
    ): Response<ResponseBody>

    @GET("members/{id}")
    suspend fun getMemberById(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @PUT("members/{id}")
    suspend fun updateMember(
        @Path("id") id: Long,
        @Body body: UpdateMemberRequest
    ): Response<ResponseBody>

    @PATCH("members/{id}")
    suspend fun patchMember(
        @Path("id") id: Long,
        @Body body: UpdateMemberRequest
    ): Response<ResponseBody>

    @DELETE("members/{id}")
    suspend fun deleteMember(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @GET("plans")
    suspend fun getPlans(): Response<ResponseBody>

    @POST("plans")
    suspend fun createPlan(
        @Body body: CreatePlanRequest
    ): Response<ResponseBody>

    @GET("plans/{id}")
    suspend fun getPlanById(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @DELETE("plans/{id}")
    suspend fun deletePlan(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @GET("subscriptions")
    suspend fun getSubscriptions(
        @Query("member_id") memberId: Long? = null,
        @Query("page") page: Int? = null
    ): Response<ResponseBody>

    @POST("subscriptions")
    suspend fun createSubscription(
        @Body body: CreateSubscriptionRequest
    ): Response<ResponseBody>

    @GET("subscriptions/{id}")
    suspend fun getSubscriptionById(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @DELETE("subscriptions/{id}")
    suspend fun deleteSubscription(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @GET("attendance")
    suspend fun getAttendance(
        @Query("date") date: String? = null,
        @Query("page") page: Int? = null
    ): Response<ResponseBody>

    @POST("attendance")
    suspend fun markAttendance(
        @Body body: AttendanceRequest
    ): Response<ResponseBody>

    @GET("payments")
    suspend fun getPayments(
        @Query("member_id") memberId: Long? = null,
        @Query("page") page: Int? = null
    ): Response<ResponseBody>

    @POST("payments")
    suspend fun createPayment(
        @Body body: CreatePaymentRequest
    ): Response<ResponseBody>

    @GET("payment-requests")
    suspend fun getPaymentRequests(
        @Query("page") page: Int? = null
    ): Response<ResponseBody>

    @POST("payment-requests")
    suspend fun createPaymentRequest(
        @Body body: CreatePaymentRequestDto
    ): Response<ResponseBody>

    @GET("payment-requests/{id}")
    suspend fun getPaymentRequestById(
        @Path("id") id: Long
    ): Response<ResponseBody>

    @PUT("profile")
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest
    ): Response<ResponseBody>

    @PATCH("profile")
    suspend fun patchProfile(
        @Body body: UpdateProfileRequest
    ): Response<ResponseBody>

    @POST("webhooks/razorpay")
    suspend fun postRazorpayWebhook(
        @Header("X-Razorpay-Signature") signature: String,
        @Body payload: RequestBody
    ): Response<ResponseBody>
}
