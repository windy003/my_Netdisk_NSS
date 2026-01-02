package com.netdisk.app.storage

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveServerConfig(host: String, port: Int) {
        prefs.edit().apply {
            putString(KEY_SERVER_HOST, host)
            putInt(KEY_SERVER_PORT, port)
            putBoolean(KEY_HAS_CONFIGURED, true)  // Mark as configured
            apply()
        }
    }

    fun hasConfigured(): Boolean {
        return prefs.getBoolean(KEY_HAS_CONFIGURED, false)
    }

    fun getServerHost(): String {
        return prefs.getString(KEY_SERVER_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    }

    fun getServerPort(): Int {
        return prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
    }

    fun getServerUrl(): String {
        val host = getServerHost()
        val port = getServerPort()
        return "http://$host:$port"
    }

    fun saveAuthCookies(cookies: String) {
        prefs.edit().putString(KEY_AUTH_COOKIES, cookies).apply()
    }

    fun getAuthCookies(): String? {
        return prefs.getString(KEY_AUTH_COOKIES, null)
    }

    fun clearAuthCookies() {
        prefs.edit().remove(KEY_AUTH_COOKIES).apply()
    }

    fun saveStreamToken(token: String) {
        prefs.edit().putString(KEY_STREAM_TOKEN, token).apply()
    }

    fun getStreamToken(): String? {
        return prefs.getString(KEY_STREAM_TOKEN, null)
    }

    fun clearStreamToken() {
        prefs.edit().remove(KEY_STREAM_TOKEN).apply()
    }

    companion object {
        private const val PREF_NAME = "netdisk_prefs"
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_AUTH_COOKIES = "auth_cookies"
        private const val KEY_HAS_CONFIGURED = "has_configured"
        private const val KEY_STREAM_TOKEN = "stream_token"
        private const val DEFAULT_HOST = "192.168.1.100"
        private const val DEFAULT_PORT = 5003
    }
}
