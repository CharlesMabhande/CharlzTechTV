package com.charlztech.charlztechtv.util

import com.charlztech.charlztechtv.data.model.M3uChannel

object M3uParser {

    fun parse(content: String): List<M3uChannel> {
        val lines = content.lines()
        val channels = mutableListOf<M3uChannel>()
        var currentName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentHeaders = mutableMapOf<String, String>()
        var currentUserAgent: String? = null
        var currentReferer: String? = null
        var currentCookie: String? = null
        var currentLicense: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("#EXTVLCOPT:http-user-agent=") -> {
                    currentUserAgent = trimmed.substringAfter("=").trim()
                    currentHeaders["User-Agent"] = currentUserAgent
                }
                trimmed.startsWith("#EXTVLCOPT:http-referrer=") -> {
                    currentReferer = trimmed.substringAfter("=").trim()
                    currentHeaders["Referer"] = currentReferer
                }
                trimmed.startsWith("#EXTHTTP:") -> {
                    parseExtHttp(trimmed.removePrefix("#EXTHTTP:"), currentHeaders) { ua, ref, cookie ->
                        currentUserAgent = ua ?: currentUserAgent
                        currentReferer = ref ?: currentReferer
                        currentCookie = cookie ?: currentCookie
                    }
                }
                trimmed.startsWith("#KODIPROP:inputstream.adaptive.license_key=") -> {
                    currentLicense = trimmed.substringAfter("=", "").trim()
                }
                trimmed.startsWith("#EXTINF:") -> {
                    currentName = extractAttribute(trimmed, "tvg-name")
                        ?: trimmed.substringAfter(",").trim().ifBlank { "Channel" }
                    currentLogo = extractAttribute(trimmed, "tvg-logo")
                    currentGroup = extractAttribute(trimmed, "group-title")
                }
                !trimmed.startsWith("#") && currentName != null -> {
                    val (url, inlineHeaders) = StreamLinkParser.parse(trimmed)
                    val mergedHeaders = currentHeaders.toMutableMap()
                    inlineHeaders.forEach { (k, v) ->
                        mergedHeaders[k] = v
                        when (k.lowercase()) {
                            "user-agent" -> currentUserAgent = v
                            "referer" -> currentReferer = v
                            "cookie" -> currentCookie = v
                        }
                    }
                    currentUserAgent?.let { mergedHeaders["User-Agent"] = it }
                    currentReferer?.let { mergedHeaders["Referer"] = it }
                    currentCookie?.let { mergedHeaders["Cookie"] = it }

                    channels += M3uChannel(
                        name = currentName,
                        url = url,
                        logo = currentLogo,
                        group = currentGroup,
                        headers = mergedHeaders,
                        userAgent = currentUserAgent,
                        referer = currentReferer,
                        cookie = currentCookie,
                        licenseString = currentLicense,
                        isDrm = !currentLicense.isNullOrBlank()
                    )
                    currentName = null
                    currentLogo = null
                    currentGroup = null
                    currentHeaders = mutableMapOf()
                    currentUserAgent = null
                    currentReferer = null
                    currentCookie = null
                    currentLicense = null
                }
            }
        }
        return channels
    }

    private fun extractAttribute(line: String, key: String): String? {
        val pattern = """$key="([^"]*)"""".toRegex()
        return pattern.find(line)?.groupValues?.getOrNull(1)
    }

    private fun parseExtHttp(
        json: String,
        headers: MutableMap<String, String>,
        onParsed: (ua: String?, referer: String?, cookie: String?) -> Unit
    ) {
        try {
            var ua: String? = null
            var referer: String? = null
            var cookie: String? = null
            """"user-agent"\s*:\s*"([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
                .find(json)?.groupValues?.getOrNull(1)?.let {
                    ua = it
                    headers["User-Agent"] = it
                }
            """"referer"\s*:\s*"([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
                .find(json)?.groupValues?.getOrNull(1)?.let {
                    referer = it
                    headers["Referer"] = it
                }
            """"cookie"\s*:\s*"([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
                .find(json)?.groupValues?.getOrNull(1)?.let {
                    cookie = it
                    headers["Cookie"] = it
                }
            onParsed(ua, referer, cookie)
        } catch (_: Exception) {
        }
    }
}
