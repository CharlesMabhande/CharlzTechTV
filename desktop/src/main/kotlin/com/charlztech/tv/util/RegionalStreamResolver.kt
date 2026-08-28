package com.charlztech.tv.util

import com.charlztech.tv.data.remote.StreamApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Resolves Southern Africa regional streams (Botswana, Zambia, Namibia) at play time. */
object RegionalStreamResolver {
    private val m3u8Regex = Regex("""(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)""")

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val znbcPages = mapOf(
        "znbc1" to "https://www.znbc.co.zm/live-tv-stream",
        "znbc2" to "https://www.znbc.co.zm/tv2-live-streaming",
        "znbc3" to "https://www.znbc.co.zm/tv3/live-tv-stream",
        "znbc4" to "https://www.znbc.co.zm/live-tv4"
    )

    private val znbcAniview = mapOf(
        "znbc1" to "https://uvotv-aniview.global.ssl.fastly.net/hls/live/2119689/znbc1/playlist.m3u8",
        "znbc2" to "https://uvotv-aniview.global.ssl.fastly.net/hls/live/2119689/znbc2/playlist.m3u8",
        "znbc3" to "https://uvotv-aniview.global.ssl.fastly.net/hls/live/2119689/znbc3/playlist.m3u8",
        "znbc4" to "https://uvotv-aniview.global.ssl.fastly.net/hls/live/2119690/znbc4/playlist.m3u8"
    )

    private val nbcAniview = mapOf(
        "nbc1" to "https://uvotv-aniview.global.ssl.fastly.net/hls/live/2120427/nbc1/playlist.m3u8",
        "nbc2" to "https://uvotv-aniview.global.ssl.fastly.net/hls/live/2120427/nbc2/playlist.m3u8"
    )

    private val nbcHls = mapOf(
        "nbc1" to "https://hls2.nbcplus.na/hls/high_nbc1.m3u8",
        "nbc2" to "https://hls2.nbcplus.na/hls/high_nbc2.m3u8",
        "nbc3" to "https://hls2.nbcplus.na/hls/high_nbc3.m3u8"
    )

    fun needsResolution(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("massmedia.co.bw") ||
            lower.contains("znbc.co.zm") ||
            lower.contains("aniview.global") && (lower.contains("znbc") || isNbcAniview(lower)) ||
            lower.contains("nbcplus.na")
    }

    suspend fun resolveCandidates(url: String): List<String> = withContext(Dispatchers.IO) {
        val candidates = linkedSetOf<String>()
        val lower = url.lowercase()

        when {
            lower.contains("massmedia.co.bw") -> {
                candidates.add(url)
                refreshMassmediaPlaylist(url)?.let { refreshed ->
                    if (refreshed != url) candidates.add(refreshed)
                }
                massmediaHtmlEmbed(url)?.let { candidates.add(it) }
            }
            lower.contains("znbc.co.zm") -> {
                znbcKeyFromPage(url)?.let { key ->
                    znbcAniview[key]?.let { candidates.add(it) }
                    extractM3u8FromPage(url, znbcHeaders())?.let { candidates.add(it) }
                }
                candidates.add(url)
            }
            lower.contains("aniview") && lower.contains("znbc") -> {
                candidates.add(url)
                znbcKeyFromAniview(url)?.let { key ->
                    znbcPages[key]?.let { candidates.add(it) }
                }
            }
            isNbcAniview(lower) || lower.contains("nbcplus.na") -> {
                candidates.add(url)
                nbcKeyFromUrl(url)?.let { key ->
                    nbcHls[key]?.let { candidates.add(it) }
                }
            }
        }

        if (url !in candidates) candidates.add(url)
        candidates.toList()
    }

    fun headersForUrl(url: String): Map<String, String> {
        val lower = url.lowercase()
        return when {
            lower.contains("massmedia.co.bw") -> massmediaHeaders()
            lower.contains("znbc.co.zm") || lower.contains("znbc") && lower.contains("aniview") ->
                znbcHeaders()
            isNbcAniview(lower) || lower.contains("nbcplus.na") -> nbcHeaders()
            else -> emptyMap()
        }
    }

    private fun isNbcAniview(lower: String): Boolean =
        lower.contains("aniview") && Regex("""/nbc[123]/""").containsMatchIn(lower)

    private fun znbcKeyFromPage(url: String): String? =
        znbcPages.entries.firstOrNull { it.value.equals(url, ignoreCase = true) }?.key

    private fun znbcKeyFromAniview(url: String): String? =
        znbcAniview.entries.firstOrNull { it.value.equals(url, ignoreCase = true) }?.key
            ?: Regex("""/znbc([1-4])/""").find(url.lowercase())?.groupValues?.getOrNull(1)
                ?.let { "znbc$it" }

    private fun nbcKeyFromUrl(url: String): String? {
        val lower = url.lowercase()
        return when {
            lower.contains("nbc1") -> "nbc1"
            lower.contains("nbc2") -> "nbc2"
            lower.contains("nbc3") -> "nbc3"
            else -> null
        }
    }

    private fun refreshMassmediaPlaylist(masterUrl: String): String? {
        val body = httpGet(masterUrl, massmediaHeaders()) ?: return null
        val mediaLine = body.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("#") && it.contains(".m3u8", ignoreCase = true) }
            ?: return masterUrl

        return if (mediaLine.startsWith("http", ignoreCase = true)) {
            mediaLine
        } else {
            val base = masterUrl.substringBeforeLast('/') + "/"
            base + mediaLine
        }
    }

    private fun massmediaHtmlEmbed(m3u8Url: String): String? {
        val uuid = Regex("""memfs/([a-f0-9-]+)\.m3u8""", RegexOption.IGNORE_CASE)
            .find(m3u8Url)?.groupValues?.getOrNull(1)
            ?: return null
        return "https://streaming.massmedia.co.bw/$uuid.html"
    }

    private fun extractM3u8FromPage(pageUrl: String, headers: Map<String, String>): String? {
        val html = httpGet(pageUrl, headers) ?: return null
        return m3u8Regex.findAll(html)
            .map { it.groupValues[1] }
            .firstOrNull { it.contains("aniview", ignoreCase = true) || it.contains("m3u8", ignoreCase = true) }
    }

    private fun massmediaHeaders(): Map<String, String> = mapOf(
        "Referer" to "https://massmedia.co.bw/",
        "Origin" to "https://massmedia.co.bw",
        "User-Agent" to StreamApiService.USER_AGENT
    )

    private fun znbcHeaders(): Map<String, String> = mapOf(
        "Referer" to "https://www.znbc.co.zm/",
        "Origin" to "https://www.znbc.co.zm",
        "User-Agent" to StreamApiService.USER_AGENT
    )

    private fun nbcHeaders(): Map<String, String> = mapOf(
        "Referer" to "https://www.nbc.na/",
        "Origin" to "https://www.nbc.na",
        "User-Agent" to StreamApiService.USER_AGENT
    )

    private fun httpGet(url: String, headers: Map<String, String>): String? {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (key, value) -> header(key, value) }
        }.build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        }.getOrNull()
    }
}
