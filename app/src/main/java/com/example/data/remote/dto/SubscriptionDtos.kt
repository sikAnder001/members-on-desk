package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateSubscriptionRequest(
    @Json(name = "member_id") val member_id: Long,
    @Json(name = "membership_plan_id") val membership_plan_id: Long? = null,
    @Json(name = "plan_id") val plan_id: Long? = null,
    @Json(name = "starts_at") val starts_at: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "collect_cash") val collect_cash: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class SubscriptionDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "member_id") val member_id: Long? = null,
    @Json(name = "membership_plan_id") val membership_plan_id: Long? = null,
    @Json(name = "plan_id") val plan_id: Long? = null,
    @Json(name = "plan_name") val plan_name: String? = null,
    @Json(name = "starts_at") val starts_at: String? = null,
    @Json(name = "expires_at") val expires_at: String? = null,
    @Json(name = "ends_at") val ends_at: String? = null,
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "paid_amount") val paid_amount: Double? = null,
    @Json(name = "balance") val balance: Double? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "created_at") val created_at: String? = null
)

