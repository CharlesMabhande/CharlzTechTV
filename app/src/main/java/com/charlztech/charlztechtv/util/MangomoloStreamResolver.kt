package com.charlztech.charlztechtv.util

import com.charlztech.charlztechtv.data.remote.StreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Resolves Mangomolo player pages to tokenized HLS URLs on the user's device/network. */
object MangomoloStreamResolver {
    private val streamRegex = Regex("""src\s*:\s*(["'])(https?://\S+?\.m3u8\S*?)\1""")
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun isPlayerUrl(url: String): Boolean =
        url.contains("player.mangomolo.com/v1/", ignoreCase = true)

    fun isSabcEntertainmentUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("/sabc1/") || lower.contains("/sabc2/") || lower.contains("/sabc3/")
    }

    fun needsResolution(url: String): Boolean =
        isPlayerUrl(url) || isSabcEntertainmentUrl(url)

    suspend fun resolve(url: String): String? = withContext(Dispatchers.IO) {
        when {
            isPlayerUrl(url) -> fetchStreamFromPlayer(url)
            isSabcEntertainmentUrl(url) -> {
                SabcChannels.playerUrlForDirect(url)?.let { fetchStreamFromPlayer(it) }
            }
            else -> null
        }
    }

    private fun fetchStreamFromPlayer(playerUrl: String): String? {
        val request = Request.Builder()
            .url(playerUrl)
            .header("User-Agent", StreamApiService.USER_AGENT)
            .header("Referer", "https://player.mangomolo.com/")
            .header("Origin", "https://player.mangomolo.com")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                extractStreamUrl(response.body?.string().orEmpty())
            }
        }.getOrNull()
    }

    private fun extractStreamUrl(html: String): String? =
        streamRegex.find(html)?.groupValues?.getOrNull(2)
            ?: Regex("""(https://[^\s"']+\.m3u8[^\s"']*)""").find(html)?.groupValues?.getOrNull(1)
}
