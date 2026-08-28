package com.charlztech.charlztechtv.util

import com.charlztech.charlztechtv.data.remote.StreamApiService

/** HTTP headers for international free news streams. */
object NewsChannels {
    fun headersForUrl(url: String): Map<String, String> {
        val lower = url.lowercase()
        return when {
            lower.contains("getaj.net") || lower.contains("aljazeera") -> mapOf(
                "Referer" to "https://www.aljazeera.com/",
                "Origin" to "https://www.aljazeera.com",
                "User-Agent" to StreamApiService.USER_AGENT
            )
            lower.contains("france24.com") -> mapOf(
                "Referer" to "https://www.france24.com/",
                "Origin" to "https://www.france24.com",
                "User-Agent" to StreamApiService.USER_AGENT
            )
            lower.contains("dwamdstream") || lower.contains("dw.com") -> mapOf(
                "Referer" to "https://www.dw.com/",
                "Origin" to "https://www.dw.com",
                "User-Agent" to StreamApiService.USER_AGENT
            )
            lower.contains("rttv.com") -> mapOf(
                "Referer" to "https://odysee.com/",
                "Origin" to "https://odysee.com",
                "User-Agent" to StreamApiService.USER_AGENT
            )
            else -> emptyMap()
        }
    }
}
