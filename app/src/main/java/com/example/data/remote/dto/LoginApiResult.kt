package com.example.data.remote.dto

sealed class LoginApiResult {
    data class Success(val response: LoginResponse) : LoginApiResult()
    data class InvalidCredentials(val message: String, val errorDetails: String? = null) : LoginApiResult()
    data class NetworkUnavailable(val message: String) : LoginApiResult()
}
