package com.charlztech.charlztechtv.data.remote

import android.content.Context
import com.charlztech.charlztechtv.BuildConfig
import com.charlztech.charlztechtv.data.cache.ResponseCache
import com.charlztech.charlztechtv.data.model.ChannelStreamResponse
import com.charlztech.charlztechtv.data.model.LiveEvent
import com.charlztech.charlztechtv.data.model.M3uChannel
import com.charlztech.charlztechtv.data.model.Provider
import com.charlztech.charlztechtv.util.CryptoUtils
import com.charlztech.charlztechtv.util.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class StreamApiService(private val appContext: Context? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedBaseUrl: String? = null
    private val listCache = ResponseCache(ttlMs = 3 * 60 * 1000)
    private val streamCache = ResponseCache(ttlMs = 5 * 60 * 1000)
    private val m3uCache = ResponseCache(ttlMs = 30 * 60 * 1000)

    suspend fun warmConnection() = withContext(Dispatchers.IO) {
        try {
            val base = resolveBaseUrl()
            fetchText("$base/cats.txt", useCache = false)
        } catch (_: Exception) {
        }
    }

    suspend fun resolveBaseUrl(): String = withContext(Dispatchers.IO) {
        cachedBaseUrl?.let { return@withContext it }

        val urls = FirebaseRemoteConfigFetcher.getApiUrls()
        val candidates = listOfNotNull(
            urls?.second,
            urls?.first,
            BuildConfig.DEFAULT_API_BASE,
            BuildConfig.FALLBACK_API_BASE
        ).map { it.trimEnd('/') }.distinct()

        for (base in candidates) {
            if (probeBackend(base)) {
                cachedBaseUrl = base
                return@withContext base
            }
        }

        val fallback = BuildConfig.DEFAULT_API_BASE.trimEnd('/')
        cachedBaseUrl = fallback
        fallback
    }

    private fun probeBackend(baseUrl: String): Boolean {
        return try {
            val body = fetchText("$baseUrl/cats.txt", useCache = false) ?: return false
            val decrypted = CryptoUtils.decryptData(body)
            !decrypted.isNullOrBlank() && (decrypted.startsWith("[") || decrypted.startsWith("{"))
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchProviders(): List<Provider> = withContext(Dispatchers.IO) {
        listCache.get<List<Provider>>("providers")?.let { return@withContext it }
        val base = resolveBaseUrl()
        val fromApi = fetchEncryptedList<Provider>("$base/cats.txt")
        val result = if (fromApi.isNotEmpty()) {
            fromApi.filter { !it.catLink.isNullOrBlank() && it.catLink != "null" && it.catLink != "ok" }
        } else {
            FallbackData.providers
        }
        val merged = mergeWithRegional(result)
        listCache.put("providers", merged)
        merged
    }

    private fun mergeWithRegional(apiProviders: List<Provider>): List<Provider> {
        val apiTitles = apiProviders.map { it.title.lowercase() }.toSet()
        val regional = RegionalProviders.providers.filter { it.title.lowercase() !in apiTitles }
        return regional + apiProviders
    }

    suspend fun fetchLiveEvents(): List<LiveEvent> = withContext(Dispatchers.IO) {
        listCache.get<List<LiveEvent>>("live_events")?.let { return@withContext it }
        val base = resolveBaseUrl()
        val fromApi = fetchEncryptedList<LiveEvent>("$base/categories/live-events.txt")
        val result = fromApi.filter { it.publish == 1 }
        listCache.put("live_events", result)
        result
    }

    suspend fun fetchChannelStreams(slug: String): ChannelStreamResponse? = withContext(Dispatchers.IO) {
        val cacheKey = "stream_$slug"
        streamCache.get<ChannelStreamResponse>(cacheKey)?.let { return@withContext it }
        try {
            val base = resolveBaseUrl()
            val body = fetchText("$base/channels/$slug.txt", useCache = false) ?: return@withContext null
            val decrypted = CryptoUtils.decryptData(body) ?: CryptoUtils.decryptContent(body)
            val response = json.decodeFromString<ChannelStreamResponse>(decrypted)
            streamCache.put(cacheKey, response)
            response
        } catch (_: Exception) {
            null
        }
    }

    suspend fun prefetchChannelStreams(slugs: List<String>) = withContext(Dispatchers.IO) {
        slugs.distinct().take(8).forEach { slug ->
            if (streamCache.get<ChannelStreamResponse>("stream_$slug") == null) {
                fetchChannelStreams(slug)
            }
        }
    }

    suspend fun fetchM3uChannels(catLink: String): List<M3uChannel> = withContext(Dispatchers.IO) {
        val cacheKey = "m3u_${catLink.hashCode()}"
        m3uCache.get<List<M3uChannel>>(cacheKey)?.let { return@withContext it }
        try {
            val isAsset = catLink.startsWith(RegionalProviders.ASSET_PREFIX)
            val body = when {
                isAsset -> loadAssetPlaylist(catLink.removePrefix(RegionalProviders.ASSET_PREFIX))
                else -> fetchText(catLink)
            } ?: return@withContext emptyList()
            val content = if (isAsset) body else CryptoUtils.decryptContent(body)
            val channels = M3uParser.parse(content)
            m3uCache.put(cacheKey, channels)
            channels
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadAssetPlaylist(assetPath: String): String? {
        val context = appContext ?: return null
        return try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private inline fun <reified T> fetchEncryptedList(url: String): List<T> {
        return try {
            val body = fetchText(url) ?: return emptyList()
            val decrypted = CryptoUtils.decryptData(body) ?: return emptyList()
            if (decrypted.startsWith("[") || decrypted.startsWith("{")) {
                json.decodeFromString<List<T>>(decrypted)
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchText(url: String, useCache: Boolean = true): String? {
        val cacheKey = "text_${url.hashCode()}"
        if (useCache) {
            listCache.get<String>(cacheKey)?.let { return it }
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
        if (body != null && useCache) listCache.put(cacheKey, body)
        return body
    }

    fun invalidateCache() {
        cachedBaseUrl = null
        listCache.clear()
        streamCache.clear()
        m3uCache.clear()
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0"
    }
}
