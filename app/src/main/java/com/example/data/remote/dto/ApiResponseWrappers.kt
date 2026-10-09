package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiListEnvelope<T>(
    @Json(name = "data") val data: List<T>? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiSingleEnvelope<T>(
    @Json(name = "data") val data: T? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null
)
