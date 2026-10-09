package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePaymentRequestDto(
    @Json(name = "member_id") val member_id: Long,
    @Json(name = "amount") val amount: Double,
    @Json(name = "method") val method: String,
    @Json(name = "subscription_id") val subscription_id: Long
)

@JsonClass(generateAdapter = true)
data class PaymentRequestDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "member_id") val member_id: Long? = null,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "method") val method: String? = null,
    @Json(name = "subscription_id") val subscription_id: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "upi_url") val upi_url: String? = null,
    @Json(name = "payment_url") val payment_url: String? = null,
    @Json(name = "created_at") val created_at: String? = null
)
