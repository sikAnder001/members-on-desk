package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePlanRequest(
    @Json(name = "name") val name: String,
    @Json(name = "duration_days") val duration_days: Int,
    @Json(name = "price") val price: Double,
    @Json(name = "description") val description: String? = null,
    @Json(name = "is_active") val is_active: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class PlanDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "duration_days") val duration_days: Int? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "is_active") val is_active: Boolean? = true
)
