package com.example.data.remote

/**
 * Central configuration for backend API integration.
 */
object ApiConstants {

    /**
     * The single central Base URL used for all backend endpoints across the app.
     */
    var BASE_URL: String = "https://memberondesk.wasmer.app/"

    /**
     * Ensures Retrofit receives a properly formatted base URL with /api/v1/ and trailing slash.
     */
    val resolvedBaseUrl: String
        get() {
            var url = BASE_URL.trim()
            if (!url.endsWith("/")) {
                url = "$url/"
            }
            if (!url.contains("/api/v1") && !url.contains("/api/v1/")) {
                url = "${url}api/v1/"
            }
            return url
        }

    /**
     * Endpoint path for exporting members (CSV/Excel).
     * Defaults to "members/export" which resolves to "${resolvedBaseUrl}members/export".
     * Can be updated whenever the specific backend endpoint path is provided.
     */
    var EXPORT_MEMBERS_ENDPOINT: String = "members/export"

    const val TIMEOUT_CONNECT_SECONDS = 15L
    const val TIMEOUT_READ_SECONDS = 20L
    const val TIMEOUT_WRITE_SECONDS = 20L
}
