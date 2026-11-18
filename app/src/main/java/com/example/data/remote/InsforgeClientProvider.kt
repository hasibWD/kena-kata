package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import dev.insforge.InsforgeClient
import dev.insforge.logging.InsforgeLogLevel
import dev.insforge.auth.Auth
import dev.insforge.auth.BrowserLauncher
import dev.insforge.auth.ClientType
import dev.insforge.auth.SessionStorage
import dev.insforge.createInsforgeClient
import dev.insforge.database.Database
import dev.insforge.functions.Functions
import dev.insforge.realtime.Realtime
import dev.insforge.storage.Storage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.insforgeAuthDataStore: DataStore<Preferences> by preferencesDataStore(name = "insforge_auth")

class DataStoreSessionStorage(private val context: Context) : SessionStorage {
    override suspend fun save(key: String, value: String) {
        context.insforgeAuthDataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun get(key: String): String? {
        return context.insforgeAuthDataStore.data.map { prefs ->
            prefs[stringPreferencesKey(key)]
        }.first()
    }

    override suspend fun remove(key: String) {
        context.insforgeAuthDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
        }
    }
}

object InsforgeClientProvider {
    @Volatile
    private var instance: InsforgeClient? = null

    fun resolveConfig(): Pair<String, String> {
        val envUrl = BuildConfig.INSFORGE_URL.trim()
        val envKey = BuildConfig.INSFORGE_ANON_KEY.trim()
        val baseUrl = if (envUrl.isNotBlank() && !envUrl.contains("your-project")) {
            envUrl
        } else {
            BuildConfig.INSFORGE_LOCAL_URL.trim()
        }
        val anonKey = if (envKey.isNotBlank() && !envKey.startsWith("MY_")) {
            envKey
        } else {
            BuildConfig.INSFORGE_LOCAL_ANON_KEY.trim()
        }
        return baseUrl to anonKey
    }

    fun isConfigured(): Boolean {
        val (url, key) = resolveConfig()
        return url.startsWith("https://") && key.isNotBlank()
    }

    fun getClient(context: Context): InsforgeClient? {
        if (!isConfigured()) return null
        return instance ?: synchronized(this) {
            instance ?: createClient(context.applicationContext).also { instance = it }
        }
    }

    private fun createClient(appContext: Context): InsforgeClient {
        val (baseUrl, anonKey) = resolveConfig()
        return createInsforgeClient(
            baseURL = baseUrl,
            anonKey = anonKey
        ) {
            logLevel = if (BuildConfig.DEBUG) InsforgeLogLevel.DEBUG else InsforgeLogLevel.ERROR

            install(Auth) {
                browserLauncher = BrowserLauncher { url ->
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    if (appContext is Activity) {
                        customTabsIntent.launchUrl(appContext, Uri.parse(url))
                    } else {
                        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        customTabsIntent.launchUrl(appContext, Uri.parse(url))
                    }
                }
                persistSession = true
                sessionStorage = DataStoreSessionStorage(appContext)
                clientType = ClientType.MOBILE
            }

            install(Database)
            install(Storage)
            install(Functions)
            install(Realtime) {
                autoReconnect = true
                reconnectDelay = 5000
            }
        }
    }
}
