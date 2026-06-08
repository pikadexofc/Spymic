package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)

    fun getApiKeys(): List<String> {
        val keysStr = prefs.getString("api_keys", "") ?: ""
        return if (keysStr.isEmpty()) emptyList() else keysStr.split(",")
    }

    fun addApiKey(key: String) {
        val keys = getApiKeys().toMutableList()
        if (!keys.contains(key)) {
            keys.add(key)
            prefs.edit().putString("api_keys", keys.joinToString(",")).apply()
        }
    }

    fun removeApiKey(key: String) {
        val keys = getApiKeys().toMutableList()
        keys.remove(key)
        prefs.edit().putString("api_keys", keys.joinToString(",")).apply()
    }
}
