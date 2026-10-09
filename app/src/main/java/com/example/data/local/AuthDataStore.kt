package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.dto.BusinessDto
import com.example.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session_datastore")

/**
 * Manages persistent local session and auth tokens via Android Jetpack DataStore Preferences.
 */
class AuthDataStore(private val context: Context) {

    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_bearer_token")
        val TOKEN_TYPE = stringPreferencesKey("auth_token_type")
        val USER_ID = longPreferencesKey("auth_user_id")
        val USER_NAME = stringPreferencesKey("auth_user_name")
        val USER_EMAIL = stringPreferencesKey("auth_user_email")
        val USER_PHONE = stringPreferencesKey("auth_user_phone")
        val USER_ROLE = stringPreferencesKey("auth_user_role")
        val USER_BUSINESS_ID = longPreferencesKey("auth_user_business_id")
        val BUSINESS_ID = longPreferencesKey("auth_business_id")
        val BUSINESS_NAME = stringPreferencesKey("auth_business_name")
        val BUSINESS_TYPE = stringPreferencesKey("auth_business_type")
        val BUSINESS_STATUS = stringPreferencesKey("auth_business_status")
        val BUSINESS_UPI_ID = stringPreferencesKey("auth_business_upi_id")
    }

    val authTokenFlow: Flow<String?> = context.authDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[PreferencesKeys.AUTH_TOKEN] }

    val userEmailFlow: Flow<String?> = context.authDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[PreferencesKeys.USER_EMAIL] }

    val userRoleFlow: Flow<String?> = context.authDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[PreferencesKeys.USER_ROLE] }

    /**
     * Saves auth token and user/business session details locally to DataStore Preferences.
     */
    suspend fun saveAuthSession(
        token: String,
        tokenType: String? = "Bearer",
        user: UserDto? = null,
        business: BusinessDto? = null
    ) {
        context.authDataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTH_TOKEN] = token
            if (tokenType != null) prefs[PreferencesKeys.TOKEN_TYPE] = tokenType

            user?.let { u ->
                u.id?.let { prefs[PreferencesKeys.USER_ID] = it }
                u.name?.let { prefs[PreferencesKeys.USER_NAME] = it }
                u.email?.let { prefs[PreferencesKeys.USER_EMAIL] = it }
                u.phone?.let { prefs[PreferencesKeys.USER_PHONE] = it }
                u.role?.let { prefs[PreferencesKeys.USER_ROLE] = it }
                u.business_id?.let { prefs[PreferencesKeys.USER_BUSINESS_ID] = it }
            }

            business?.let { b ->
                b.id?.let { prefs[PreferencesKeys.BUSINESS_ID] = it }
                b.name?.let { prefs[PreferencesKeys.BUSINESS_NAME] = it }
                b.type?.let { prefs[PreferencesKeys.BUSINESS_TYPE] = it }
                b.status?.let { prefs[PreferencesKeys.BUSINESS_STATUS] = it }
                b.upi_id?.let { prefs[PreferencesKeys.BUSINESS_UPI_ID] = it }
            }
        }
    }

    /**
     * Clears all session credentials and tokens upon logout.
     */
    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthDataStore? = null

        fun getInstance(context: Context): AuthDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthDataStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
