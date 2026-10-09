package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePaymentRequest(
    @Json(name = "member_id") val member_id: Long,
    @Json(name = "amount") val amount: Double,
    @Json(name = "method") val method: String,
    @Json(name = "subscription_id") val subscription_id: Long? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "member_id") val member_id: Long? = null,
    @Json(name = "member_name") val member_name: String? = null,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "method") val method: String? = null,
    @Json(name = "payment_mode") val payment_mode: String? = null,
    @Json(name = "subscription_id") val subscription_id: Long? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "invoice_number") val invoice_number: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "paid_at") val paid_at: String? = null,
    @Json(name = "created_at") val created_at: String? = null
)
