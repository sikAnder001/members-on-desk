package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AttendanceRequest(
    @Json(name = "token") val token: String? = null,
    @Json(name = "member_id") val member_id: Long? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "member_id") val member_id: Long? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "check_in_at") val check_in_at: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "created_at") val created_at: String? = null
)
