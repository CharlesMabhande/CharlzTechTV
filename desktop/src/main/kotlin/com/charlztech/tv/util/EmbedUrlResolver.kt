package com.charlztech.tv.util

object EmbedUrlResolver {

    fun refererOrigin(packageName: String): String = "https://$packageName/"

    fun pageReferer(url: String, packageName: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("sabc-plus.com") -> "https://sabc-plus.com/"
            lower.contains("player.mangomolo.com") -> "https://player.mangomolo.com/"
            lower.contains("massmedia.co.bw") -> "https://massmedia.co.bw/"
            lower.contains("streaming.massmedia.co.bw") && lower.endsWith(".html") ->
                "https://massmedia.co.bw/"
            lower.contains("znbc.co.zm") -> "https://www.znbc.co.zm/"
            lower.contains("aniview.global") -> "https://www.znbc.co.zm/"
            lower.contains("approvaltv.com") -> "https://live.approvaltv.com/"
            lower.contains("nbcplus.na") || lower.contains("nbc.na") -> "https://www.nbc.na/"
            lower.contains("aljazeera.com") -> "https://www.aljazeera.com/"
            lower.contains("viloud.tv") -> "https://ztnonline.co.zw/"
            lower.contains("ztnonline.co.zw") || lower.contains("ztn.co.zw") -> "https://ztnonline.co.zw/"
            lower.contains("castr.com") || lower.contains("ottplatform.com") -> "https://zbc.ottplatform.com/"
            else -> refererOrigin(packageName)
        }
    }

    fun resolve(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return trimmed
        return trimmed
    }

    fun isWebEmbed(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("viloud.tv") ||
            lower.contains("player.castr.com") ||
            lower.contains("player.mangomolo.com/v1/live") ||
            (lower.contains("streaming.massmedia.co.bw") && lower.endsWith(".html")) ||
            lower.contains("znbc.co.zm") ||
            lower.contains("ztnonline.co.zw") ||
            lower.contains("ztn.co.zw") ||
            lower.contains("3ktv.co.zw") ||
            lower.contains("hstv.co.zw") ||
            lower.contains("zbc.ottplatform.com") ||
            (!StreamLinkParser.isDirectStream(url) && lower.startsWith("http"))
    }

    fun isIframeEmbed(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("viloud.tv/embed") ||
            lower.contains("player.castr.com")
    }

    fun buildIframeHtml(embedUrl: String, referer: String = "https://com.charlztech.tv/"): String {
        val src = embedUrl.replace("&amp;", "&")
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
              <meta name="referrer" content="strict-origin-when-cross-origin">
              <style>
                * { margin: 0; padding: 0; }
                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { position: fixed; inset: 0; width: 100%; height: 100%; border: 0; }
              </style>
            </head>
            <body>
              <iframe
                src="$src"
                title="Live TV"
                allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
                allowfullscreen
                referrerpolicy="strict-origin-when-cross-origin">
              </iframe>
            </body>
            </html>
        """.trimIndent()
    }
}
