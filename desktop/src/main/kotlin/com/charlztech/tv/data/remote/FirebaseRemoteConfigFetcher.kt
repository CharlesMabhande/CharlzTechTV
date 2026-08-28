package com.charlztech.tv.data.remote

import com.charlztech.tv.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

object FirebaseRemoteConfigFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val packageNames = listOf(
        AppConfig.CRICFY_PACKAGE_NAME,
        "com.cricfy.tv"
    ).distinct()

    suspend fun getApiBaseUrl(): String? = withContext(Dispatchers.IO) {
        val apiKey = AppConfig.FIREBASE_API_KEY
        val appId = AppConfig.FIREBASE_APP_ID
        val projectNumber = AppConfig.FIREBASE_PROJECT_NUMBER
        if (apiKey.isBlank() || appId.isBlank() || projectNumber.isBlank()) return@withContext null

        repeat(3) { attempt ->
            for (packageName in packageNames) {
                val entries = fetchRemoteConfig(apiKey, appId, projectNumber, packageName)
                if (entries != null) {
                    return@withContext entries["cric_api2"]?.trimEnd('/')
                        ?: entries["cric_api1"]?.trimEnd('/')
                }
            }
            if (attempt < 2) Thread.sleep(500)
        }
        null
    }

    suspend fun getApiUrls(): Pair<String?, String?>? = withContext(Dispatchers.IO) {
        val apiKey = AppConfig.FIREBASE_API_KEY
        val appId = AppConfig.FIREBASE_APP_ID
        val projectNumber = AppConfig.FIREBASE_PROJECT_NUMBER
        if (apiKey.isBlank() || appId.isBlank() || projectNumber.isBlank()) return@withContext null

        repeat(3) {
            for (packageName in packageNames) {
                val entries = fetchRemoteConfig(apiKey, appId, projectNumber, packageName)
                if (entries != null) {
                    return@withContext Pair(
                        entries["cric_api1"]?.trimEnd('/'),
                        entries["cric_api2"]?.trimEnd('/')
                    )
                }
            }
        }
        null
    }

    private fun fetchRemoteConfig(
        apiKey: String,
        appId: String,
        projectNumber: String,
        packageName: String
    ): Map<String, String>? {
        return try {
            val url =
                "https://firebaseremoteconfig.googleapis.com/v1/projects/$projectNumber/namespaces/firebase:fetch"
            val instanceId = UUID.randomUUID().toString().replace("-", "")
            val payload = """
                {
                  "appInstanceId": "$instanceId",
                  "appInstanceIdToken": "",
                  "appId": "$appId",
                  "countryCode": "US",
                  "languageCode": "en-US",
                  "platformVersion": "30",
                  "timeZone": "UTC",
                  "appVersion": "6.1",
                  "appBuild": "61",
                  "packageName": "$packageName",
                  "sdkVersion": "22.1.0",
                  "analyticsUserProperties": {}
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Android-Package", packageName)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Google-GFE-Can-Retry", "yes")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                json.decodeFromString<RemoteConfigResponse>(body).entries
            }
        } catch (_: Exception) {
            null
        }
    }
}

@kotlinx.serialization.Serializable
private data class RemoteConfigResponse(
    val entries: Map<String, String>? = null
)
