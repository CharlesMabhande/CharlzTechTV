package com.charlztech.tv.util

import com.charlztech.tv.data.model.StreamServer

object StreamServerUtils {

    enum class ServerKind(val label: String) {
        HLS("HLS"),
        DASH("DASH"),
        MP4("MP4"),
        EMBED("WEB"),
        UNKNOWN("LINK")
    }

    fun kind(server: StreamServer): ServerKind {
        val link = server.link ?: return ServerKind.UNKNOWN
        val (url, _) = StreamLinkParser.parse(link)
        return when (server.type) {
            "7" -> ServerKind.DASH
            else -> when {
                url.contains(".mpd", ignoreCase = true) -> ServerKind.DASH
                url.contains(".mp4", ignoreCase = true) -> ServerKind.MP4
                StreamLinkParser.isDirectStream(url) -> ServerKind.HLS
                !server.webLink.isNullOrBlank() -> ServerKind.EMBED
                else -> ServerKind.EMBED
            }
        }
    }

    fun isFastDirect(server: StreamServer): Boolean {
        val link = server.link ?: return false
        val (url, _) = StreamLinkParser.parse(link)
        return StreamLinkParser.isDirectStream(url) && server.type != "7"
    }

    fun displayName(server: StreamServer, index: Int): String =
        server.title?.takeIf { it.isNotBlank() } ?: "Server ${index + 1}"

    fun pickBestServer(servers: List<StreamServer>): StreamServer? {
        if (servers.isEmpty()) return null
        return servers.firstOrNull { isFastDirect(it) }
            ?: servers.firstOrNull { kind(it) == ServerKind.HLS }
            ?: servers.first()
    }

    fun sortServersFastFirst(servers: List<StreamServer>): List<StreamServer> =
        servers.sortedByDescending { isFastDirect(it) }
}
