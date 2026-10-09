package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.remote.dto.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TAG = "MemberOnDeskApi"
    private const val HTTP_TAG = "MemberOnDeskHttp"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    private var apiServiceInstance: ApiService? = null

    @Volatile
    private var lastBaseUrl: String? = null

    fun getApiService(context: Context): ApiService {
        val currentBaseUrl = ApiConstants.resolvedBaseUrl
        val current = apiServiceInstance

        if (current != null && lastBaseUrl == currentBaseUrl) {
            return current
        }

        return synchronized(this) {
            if (apiServiceInstance != null && lastBaseUrl == currentBaseUrl) {
                apiServiceInstance!!
            } else {
                val tokenManager = TokenManager.getInstance(context)
                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(tokenManager))
                    .addInterceptor(HttpLoggingInterceptor { msg ->
                        // Outputs complete HTTP wire request headers, method, URL, status code, and JSON body
                        Log.i(HTTP_TAG, msg)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(ApiConstants.TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(ApiConstants.TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(ApiConstants.TIMEOUT_WRITE_SECONDS, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()

                Log.i(TAG, "Initialized Retrofit client pointing to: $currentBaseUrl")

                val retrofit = Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
                    .build()

                val service = retrofit.create(ApiService::class.java)
                apiServiceInstance = service
                lastBaseUrl = currentBaseUrl
                service
            }
        }
    }

    val lastApiError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun clearLastError() {
        lastApiError.value = null
    }

    fun parseErrorMessage(code: Int, errorBody: String?): String {
        if (errorBody.isNullOrBlank()) {
            return when (code) {
                400 -> "Bad Request (HTTP 400)"
                401 -> "Session expired or unauthorized (HTTP 401)"
                403 -> "Access forbidden (HTTP 403)"
                404 -> "Resource not found (HTTP 404)"
                422 -> "Validation failed (HTTP 422)"
                in 500..599 -> "Server error (HTTP $code). Please try again later."
                else -> "HTTP error $code"
            }
        }
        return try {
            val json = JSONObject(errorBody.trim())
            when {
                json.has("message") -> json.getString("message")
                json.has("error") -> json.getString("error")
                json.has("errors") -> {
                    val errorsObj = json.optJSONObject("errors")
                    if (errorsObj != null && errorsObj.length() > 0) {
                        val firstKey = errorsObj.keys().next()
                        val arr = errorsObj.optJSONArray(firstKey)
                        if (arr != null && arr.length() > 0) arr.getString(0) else errorsObj.optString(firstKey)
                    } else {
                        "Validation error (HTTP $code)"
                    }
                }
                else -> "HTTP $code: $errorBody"
            }
        } catch (e: Exception) {
            "HTTP $code: ${errorBody.take(120)}"
        }
    }

    suspend fun <T> safeApiCall(
        operationName: String,
        call: suspend () -> Response<ResponseBody>,
        parser: (String) -> T?
    ): T? {
        Log.i(TAG, "--> START API CALL: [$operationName]")
        return try {
            val response = call()
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: ""
                Log.i(TAG, "<-- API SUCCESS [$operationName] HTTP ${response.code()}: $bodyString")
                parser(bodyString)
            } else {
                val errorString = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                val parsedMsg = parseErrorMessage(response.code(), errorString)
                Log.e(TAG, "<-- API SERVER ERROR [$operationName] HTTP ${response.code()}: $errorString ($parsedMsg)")
                lastApiError.value = parsedMsg
                null
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is java.net.SocketTimeoutException -> "Network timeout while connecting to server"
                is java.net.UnknownHostException -> "Unable to reach server. Please check internet connection"
                is java.io.IOException -> "Network error: ${e.message ?: "Connection failure"}"
                else -> e.message ?: "Network error"
            }
            Log.w(TAG, "<-- API NETWORK EXCEPTION [$operationName]: ${e.javaClass.simpleName} - $errorMsg")
            lastApiError.value = errorMsg
            null
        }
    }

    fun <T : Any> parseObject(json: String, clazz: Class<T>): T? {
        if (json.isBlank()) return null
        return try {
            val trimmed = json.trim()
            if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val targetString = if (jsonObject.has("data") && jsonObject.optJSONObject("data") != null) {
                    jsonObject.getJSONObject("data").toString()
                } else {
                    trimmed
                }
                moshi.adapter(clazz).fromJson(targetString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse object of type ${clazz.simpleName}: ${e.message}")
            null
        }
    }

    fun <T : Any> parseList(json: String, clazz: Class<T>): List<T>? {
        if (json.isBlank()) return null
        return try {
            val trimmed = json.trim()
            val listType = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(listType)

            if (trimmed.startsWith("[")) {
                adapter.fromJson(trimmed)
            } else if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                if (jsonObject.has("data") && jsonObject.optJSONArray("data") != null) {
                    adapter.fromJson(jsonObject.getJSONArray("data").toString())
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse list of type ${clazz.simpleName}: ${e.message}")
            null
        }
    }

    fun createJsonRequestBody(jsonString: String) =
        jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
}
