package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kena_kata_prefs")

class SessionPreferences(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
        val RECENTLY_VIEWED = stringPreferencesKey("recently_viewed")
        val AI_CONSENT = booleanPreferencesKey("ai_personalization_consent")
        val NOTIFICATION_CONSENT = booleanPreferencesKey("notification_consent")
        val GUEST_ID = stringPreferencesKey("guest_session_id")
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.LANGUAGE] == AppLanguage.BN.code) AppLanguage.BN else AppLanguage.EN
    }

    val currentUserIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val searchHistoryFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEARCH_HISTORY].orEmpty().split("||").filter { it.isNotBlank() }
    }

    val recentlyViewedFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENTLY_VIEWED].orEmpty().split(",").filter { it.isNotBlank() }
    }

    val aiConsentFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AI_CONSENT] ?: true
    }

    val notificationConsentFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_CONSENT] ?: true
    }

    val guestIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.GUEST_ID] ?: "guest_local_device"
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit {
            if (userId.isNullOrBlank()) it.remove(Keys.CURRENT_USER_ID)
            else it[Keys.CURRENT_USER_ID] = userId
        }
    }

    suspend fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_HISTORY].orEmpty().split("||").filter { it.isNotBlank() }
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
            prefs[Keys.SEARCH_HISTORY] = updated.joinToString("||")
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
    }

    suspend fun recordProductView(productId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENTLY_VIEWED].orEmpty().split(",").filter { it.isNotBlank() }
            val updated = (listOf(productId) + current.filterNot { it == productId }).take(10)
            prefs[Keys.RECENTLY_VIEWED] = updated.joinToString(",")
        }
    }

    suspend fun setAiConsent(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AI_CONSENT] = enabled }
    }

    suspend fun setNotificationConsent(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_CONSENT] = enabled }
    }
}
