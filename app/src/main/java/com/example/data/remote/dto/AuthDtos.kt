package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "type") val type: String,
    @Json(name = "device_name") val device_name: String = "android"
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "token") val token: String? = null,
    @Json(name = "access_token") val access_token: String? = null,
    @Json(name = "token_type") val token_type: String? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "business") val business: BusinessDto? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
) {
    fun resolveToken(): String? = token ?: access_token
}

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "business_id") val business_id: Long? = null,
    @Json(name = "created_at") val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class BusinessDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "upi_id") val upi_id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "business_email") val business_email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "business_phone") val business_phone: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "pincode") val pincode: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthMeResponse(
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "business") val business: BusinessDto? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null
)
