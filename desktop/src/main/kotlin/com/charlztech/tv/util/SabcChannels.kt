package com.charlztech.tv.util

import com.charlztech.tv.data.remote.StreamApiService

/** SABC streams via Mangomolo — direct HLS where open, player resolver for tokenized channels. */
object SabcChannels {
    private const val PLAYER =
        "https://player.mangomolo.com/v1/live?id=MTk0&countries=Q0M=&filter=DENY"

    const val SABC1_PLAYER = "$PLAYER&channelid=MzIw&signature=afef9c2e6bf161cd3b3d929cfa9d51cb"
    const val SABC2_PLAYER = "$PLAYER&channelid=MzIx&signature=9d92dff59c1eddf29e8f8d87ee3e78da"
    const val SABC3_PLAYER = "$PLAYER&channelid=MzIy&signature=c8cb953e450cc1b2b851663b64928cfe"

    const val SABC1_DIRECT =
        "https://sabconeta.cdn.mangomolo.com/sabc1/smil:sabc1.stream.smil/chunklist_b1600000_t64NzIwcA==.m3u8"
    const val SABC2_DIRECT =
        "https://sabctwota.cdn.mangomolo.com/sabc2/smil:sabc2.stream.smil/chunklist_b1600000_t64NzIwcA==.m3u8"
    const val SABC3_DIRECT =
        "https://sabctreta.cdn.mangomolo.com/sabc3/smil:sabc3.stream.smil/chunklist_b1600000_t64NzIwcA==.m3u8"
    const val SABC_NEWS_DIRECT =
        "https://sabconetanw.cdn.mangomolo.com/news/smil:news.stream.smil/master.m3u8"
    const val SABC_SPORT_DIRECT =
        "https://sabctretalh.cdn.mangomolo.com/lehae/smil:lehae.stream.smil/master.m3u8"

    fun playerUrlForDirect(url: String): String? = when {
        url.contains("/sabc1/", ignoreCase = true) -> SABC1_PLAYER
        url.contains("/sabc2/", ignoreCase = true) -> SABC2_PLAYER
        url.contains("/sabc3/", ignoreCase = true) -> SABC3_PLAYER
        else -> null
    }

    fun masterUrlForDirect(url: String): String? = when {
        url.contains("/sabc1/", ignoreCase = true) ->
            "https://sabconeta.cdn.mangomolo.com/sabc1/smil:sabc1.stream.smil/master.m3u8"
        url.contains("/sabc2/", ignoreCase = true) ->
            "https://sabctwota.cdn.mangomolo.com/sabc2/smil:sabc2.stream.smil/master.m3u8"
        url.contains("/sabc3/", ignoreCase = true) ->
            "https://sabctreta.cdn.mangomolo.com/sabc3/smil:sabc3.stream.smil/master.m3u8"
        else -> null
    }

    fun mangomoloHeaders(): Map<String, String> = mapOf(
        "Referer" to "https://player.mangomolo.com/",
        "Origin" to "https://player.mangomolo.com",
        "User-Agent" to StreamApiService.USER_AGENT
    )

    fun headersForUrl(url: String): Map<String, String> = when {
        url.contains("mangomolo.com", ignoreCase = true) ||
            url.contains("sportscastafrica.com", ignoreCase = true) -> mangomoloHeaders()
        url.contains("znbc.co.zm", ignoreCase = true) ||
            url.contains("aniview.global", ignoreCase = true) ||
            url.contains("massmedia.co.bw", ignoreCase = true) ||
            url.contains("nbcplus.na", ignoreCase = true) ->
            RegionalStreamResolver.headersForUrl(url)
        else -> emptyMap()
    }
}
