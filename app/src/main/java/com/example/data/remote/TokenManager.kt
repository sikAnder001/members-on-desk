package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AuthDataStore
import com.example.data.remote.dto.BusinessDto
import com.example.data.remote.dto.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages authentication tokens and session credentials.
 * Automatically persists to both Jetpack DataStore Preferences and fast in-memory/SharedPreferences
 * so synchronous HTTP interceptors can supply the Bearer token without blocking.
 */
class TokenManager(context: Context) {

    private val appContext = context.applicationContext
    private val authDataStore = AuthDataStore.getInstance(appContext)

    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Volatile
    private var cachedToken: String? = null

    init {
        cachedToken = prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun getToken(): String? {
        if (cachedToken == null) {
            cachedToken = prefs.getString(KEY_AUTH_TOKEN, null)
        }
        return cachedToken
    }

    fun saveToken(token: String?) {
        cachedToken = token
        prefs.edit().apply {
            if (token != null) {
                putString(KEY_AUTH_TOKEN, token)
            } else {
                remove(KEY_AUTH_TOKEN)
            }
            apply()
        }

        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                authDataStore.saveAuthSession(token = token)
            }
        }
    }

    /**
     * Saves full authentication session details including token, user info and business info
     * into Jetpack DataStore Preferences.
     */
    suspend fun saveFullSession(
        token: String,
        tokenType: String? = "Bearer",
        user: UserDto? = null,
        business: BusinessDto? = null
    ) {
        cachedToken = token
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            user?.email?.let { putString(KEY_USER_EMAIL, it) }
            apply()
        }
        authDataStore.saveAuthSession(
            token = token,
            tokenType = tokenType,
            user = user,
            business = business
        )
    }

    fun clearToken() {
        cachedToken = null
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .apply()

        CoroutineScope(Dispatchers.IO).launch {
            authDataStore.clearSession()
        }
    }

    suspend fun clearSession() {
        cachedToken = null
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .apply()
        authDataStore.clearSession()
    }

    fun saveUserEmail(email: String?) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "app_auth_session_prefs"
        private const val KEY_AUTH_TOKEN = "auth_bearer_token"
        private const val KEY_USER_EMAIL = "auth_user_email"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context).also { INSTANCE = it }
            }
        }
    }
}
