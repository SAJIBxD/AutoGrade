package com.autograde.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prefs by preferencesDataStore("auth")

class TokenStore(private val context: Context) {
    private val access = stringPreferencesKey("accessToken")
    private val refresh = stringPreferencesKey("refreshToken")
    private val userId = stringPreferencesKey("userId")

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.prefs.edit {
            it[access] = accessToken
            it[refresh] = refreshToken
        }
    }

    suspend fun saveUserId(id: String) {
        context.prefs.edit { it[userId] = id }
    }

    suspend fun clear() {
        context.prefs.edit { it.clear() }
    }

    suspend fun accessToken(): String? =
        context.prefs.data.map { it[access] }.first()

    suspend fun refreshToken(): String? =
        context.prefs.data.map { it[refresh] }.first()

    suspend fun userId(): String? =
        context.prefs.data.map { it[userId] }.first()

    suspend fun isLoggedIn(): Boolean =
        accessToken() != null && userId() != null
}
