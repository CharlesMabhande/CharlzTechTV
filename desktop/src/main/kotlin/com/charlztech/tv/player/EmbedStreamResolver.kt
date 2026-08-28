package com.charlztech.tv.player

import com.charlztech.tv.data.model.PlaybackRequest
import com.charlztech.tv.data.model.StreamType
import com.charlztech.tv.data.remote.StreamApiService
import com.charlztech.tv.util.EmbedUrlResolver
import com.charlztech.tv.util.NewsChannels
import com.charlztech.tv.util.RegionalStreamResolver
import com.charlztech.tv.util.SabcChannels
import com.charlztech.tv.util.StreamLinkParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object EmbedStreamResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun resolveStreamUrl(pageUrl: String, headers: Map<String, String> = emptyMap()): String? =
        withContext(Dispatchers.IO) {
            try {
                val referer = EmbedUrlResolver.pageReferer(pageUrl, com.charlztech.tv.config.AppConfig.APP_PACKAGE_NAME)
                val requestHeaders = headers.toMutableMap()
                requestHeaders.putIfAbsent("User-Agent", StreamApiService.USER_AGENT)
                requestHeaders.putIfAbsent("Referer", referer)

                val requestBuilder = Request.Builder().url(pageUrl)
                requestHeaders.forEach { (k, v) -> requestBuilder.header(k, v) }
                val html = client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                } ?: return@withContext null

                val patterns = listOf(
                    Regex("""src:\s*"([^"]+\.m3u8[^"]*)""""),
                    Regex("""(https?://[^"'\s]+aniview[^"'\s]+\.m3u8[^"'\s]*)"""),
                    Regex("""(https?://[^"'\s]+massmedia[^"'\s]+\.m3u8[^"'\s]*)"""),
                    Regex("""(https?://[^"'\s]+\.m3u8[^"'\s]*)"""),
                    Regex("""(https?://[^"'\s]+\.mpd[^"'\s]*)"""),
                    Regex("""(https?://[^"'\s]+\.mp4[^"'\s]*)""")
                )
                for (pattern in patterns) {
                    val match = pattern.find(html)?.groupValues?.getOrNull(1)
                    if (!match.isNullOrBlank() && isPlayableStreamUrl(match)) {
                        return@withContext match
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

    fun isPlayableStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        return StreamLinkParser.isDirectStream(url) ||
            lower.contains("aniview.global") ||
            lower.contains("massmedia.co.bw") ||
            lower.contains("googlevideo.com") ||
            lower.contains("videoplayback") ||
            lower.contains("mime=video")
    }

    fun streamTypeForUrl(url: String): StreamType = when {
        url.contains(".mpd", ignoreCase = true) -> StreamType.DASH
        url.contains(".m3u8", ignoreCase = true) -> StreamType.HLS
        url.contains(".mp4", ignoreCase = true) ||
            url.contains("googlevideo.com", ignoreCase = true) ||
            url.contains("videoplayback", ignoreCase = true) -> StreamType.MP4
        StreamLinkParser.isDirectStream(url) -> StreamType.HLS
        else -> StreamType.MP4
    }
}
