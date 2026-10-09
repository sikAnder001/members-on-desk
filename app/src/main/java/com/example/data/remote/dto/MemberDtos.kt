package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateMemberRequest(
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "date_of_birth") val date_of_birth: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "emergency_contact_name") val emergency_contact_name: String? = null,
    @Json(name = "emergency_contact_phone") val emergency_contact_phone: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "joined_at") val joined_at: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateMemberRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class MemberDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "date_of_birth") val date_of_birth: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "emergency_contact_name") val emergency_contact_name: String? = null,
    @Json(name = "emergency_contact_phone") val emergency_contact_phone: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "status_label") val status_label: String? = null,
    @Json(name = "account_status") val account_status: String? = null,
    @Json(name = "joined_at") val joined_at: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "member_code") val member_code: String? = null,
    @Json(name = "qr_token") val qr_token: String? = null,
    @Json(name = "created_at") val created_at: String? = null,
    @Json(name = "plan_name") val plan_name: String? = null,
    @Json(name = "plan_id") val plan_id: Long? = null
)

