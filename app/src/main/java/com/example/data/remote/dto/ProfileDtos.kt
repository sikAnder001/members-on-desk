package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "password") val password: String? = null,
    @Json(name = "password_confirmation") val password_confirmation: String? = null,
    @Json(name = "business_name") val business_name: String? = null,
    @Json(name = "business_email") val business_email: String? = null,
    @Json(name = "business_phone") val business_phone: String? = null,
    @Json(name = "upi_id") val upi_id: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "pincode") val pincode: String? = null,
    @Json(name = "whatsapp_enabled") val whatsapp_enabled: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ProfileDataDto(
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "business") val business: BusinessDto? = null
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: ProfileDataDto? = null
)
