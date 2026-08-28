package com.charlztech.tv.data.repository

import com.charlztech.tv.data.local.FavoriteEntity
import com.charlztech.tv.data.local.FavoritesStore
import com.charlztech.tv.data.model.EventStatus
import com.charlztech.tv.data.model.LiveEvent
import com.charlztech.tv.data.model.LiveEventUi
import com.charlztech.tv.data.model.M3uChannel
import com.charlztech.tv.data.model.PlaybackRequest
import com.charlztech.tv.data.model.Provider
import com.charlztech.tv.data.model.StreamServer
import com.charlztech.tv.data.model.StreamType
import com.charlztech.tv.data.remote.StreamApiService
import com.charlztech.tv.util.EmbedUrlResolver
import com.charlztech.tv.util.MangomoloStreamResolver
import com.charlztech.tv.util.NewsChannels
import com.charlztech.tv.util.RegionalStreamResolver
import com.charlztech.tv.util.SabcChannels
import com.charlztech.tv.util.EventStatusUtils
import com.charlztech.tv.util.StreamLinkParser
import com.charlztech.tv.util.StreamServerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StreamRepository(
    private val api: StreamApiService,
    private val favoritesStore: FavoritesStore
) {
    private val mutex = Mutex()
    private val _liveEvents = MutableStateFlow<List<LiveEventUi>>(emptyList())
    val liveEvents: StateFlow<List<LiveEventUi>> = _liveEvents.asStateFlow()

    private val _providers = MutableStateFlow<List<Provider>>(emptyList())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private var lastRefreshMs = 0L

    suspend fun warmUp() {
        api.warmConnection()
    }

    suspend fun refreshAll(force: Boolean = false) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastRefreshMs < 45_000 && _liveEvents.value.isNotEmpty()) return@withLock
            if (force) api.invalidateCache()

            coroutineScope {
                val eventsDeferred = async { api.fetchLiveEvents() }
                val providersDeferred = async { api.fetchProviders() }
                val events = eventsDeferred.await().map { toUi(it) }
                    .sortedWith(
                        compareByDescending<LiveEventUi> { it.status == EventStatus.LIVE }
                            .thenByDescending { it.status == EventStatus.UPCOMING }
                            .thenBy { it.event.eventInfo?.startTime }
                    )
                _liveEvents.value = events
                _providers.value = providersDeferred.await()
                lastRefreshMs = now

                val liveSlugs = events
                    .filter { it.status == EventStatus.LIVE }
                    .take(6)
                    .map { it.event.slug }
                launch { api.prefetchChannelStreams(liveSlugs) }
            }
        }
    }

    suspend fun getChannels(provider: Provider): List<M3uChannel> {
        val link = provider.catLink ?: return emptyList()
        return api.fetchM3uChannels(link)
    }

    suspend fun buildPlaybackForEvent(event: LiveEvent): PlaybackRequest? {
        val streams = api.fetchChannelStreams(event.slug) ?: return null
        val servers = StreamServerUtils.sortServersFastFirst(streams.streamUrls.orEmpty())
        if (servers.isEmpty()) return null
        val primary = StreamServerUtils.pickBestServer(servers) ?: servers.first()
        return buildFromServer(
            title = EventStatusUtils.displayTitle(event),
            slug = event.slug,
            server = primary,
            servers = servers,
            prevSlug = streams.prevChannel,
            nextSlug = streams.nextChannel,
            posterUrl = event.image
        )
    }

    suspend fun buildPlaybackForSlug(slug: String, title: String): PlaybackRequest? {
        val streams = api.fetchChannelStreams(slug) ?: return null
        val servers = StreamServerUtils.sortServersFastFirst(streams.streamUrls.orEmpty())
        if (servers.isEmpty()) return null
        val primary = StreamServerUtils.pickBestServer(servers) ?: servers.first()
        return buildFromServer(
            title = title,
            slug = slug,
            server = primary,
            servers = servers,
            prevSlug = streams.prevChannel,
            nextSlug = streams.nextChannel
        )
    }

    suspend fun buildPlaybackForChannel(channel: M3uChannel): PlaybackRequest = withContext(Dispatchers.IO) {
        val rawUrl = EmbedUrlResolver.resolve(channel.url)
        val candidates = resolveStreamCandidates(rawUrl)
        val resolvedUrl = candidates.first()
        val headers = enrichChannelHeaders(channel, resolvedUrl)
        val type = when {
            EmbedUrlResolver.isWebEmbed(resolvedUrl) -> StreamType.EMBED
            resolvedUrl.contains(".mpd", ignoreCase = true) -> StreamType.DASH
            resolvedUrl.contains(".mp4", ignoreCase = true) -> StreamType.MP4
            StreamLinkParser.isDirectStream(resolvedUrl) -> StreamType.HLS
            else -> StreamType.EMBED
        }
        val alternateServers = candidates.drop(1).mapIndexed { idx, url ->
            StreamServer(
                id = idx + 1,
                link = url,
                title = "Stream ${idx + 2}",
                type = "0"
            )
        }
        val drm = channel.licenseString?.split(":")
        PlaybackRequest(
            title = channel.name,
            url = resolvedUrl,
            headers = headers,
            streamType = type,
            drmKid = drm?.getOrNull(0),
            drmKey = drm?.getOrNull(1),
            servers = alternateServers,
            isLiveBroadcast = type != StreamType.MP4
        )
    }

    private suspend fun resolveStreamCandidates(rawUrl: String): List<String> {
        val candidates = mutableListOf<String>()
        // Al Jazeera: try working CDN mirrors (web host often fails DNS)
        if (rawUrl.contains("getaj.net", ignoreCase = true) ||
            rawUrl.contains("thehlive.com", ignoreCase = true) ||
            (rawUrl.contains("aljazeera", ignoreCase = true) && rawUrl.contains(".m3u8", ignoreCase = true))
        ) {
            val isEnglish = rawUrl.contains("AJE", ignoreCase = true) ||
                rawUrl.contains("aje", ignoreCase = true) ||
                rawUrl.contains("english", ignoreCase = true)
            if (isEnglish) {
                candidates += "https://live-hls-apps-aje-fa.getaj.net/AJE/index.m3u8"
                candidates += "https://live-hls-apps-aje-v3-fa.getaj.net/AJE/index.m3u8"
                candidates += "https://live-hls-apps-aje.getaj.net/AJE/index.m3u8"
                candidates += "https://live-hls-web-aje-fa.thehlive.com/AJE/index.m3u8"
            }
        }
        if (RegionalStreamResolver.needsResolution(rawUrl)) {
            candidates.addAll(RegionalStreamResolver.resolveCandidates(rawUrl))
        }
        if (MangomoloStreamResolver.needsResolution(rawUrl)) {
            MangomoloStreamResolver.resolve(rawUrl)?.let { candidates.add(it) }
            SabcChannels.masterUrlForDirect(rawUrl)?.let { master ->
                if (master !in candidates) candidates.add(master)
            }
            SabcChannels.playerUrlForDirect(rawUrl)?.let { playerUrl ->
                MangomoloStreamResolver.resolve(playerUrl)?.let { resolved ->
                    if (resolved !in candidates) candidates.add(resolved)
                }
            }
        }
        if (rawUrl !in candidates) candidates.add(0, rawUrl)
        if (candidates.isEmpty()) return listOf(rawUrl)
        return candidates.distinct()
    }

    private fun enrichChannelHeaders(channel: M3uChannel, url: String): Map<String, String> {
        val headers = channel.headers.toMutableMap()
        headers.putAll(RegionalStreamResolver.headersForUrl(url))
        NewsChannels.headersForUrl(url).forEach { (k, v) -> headers.putIfAbsent(k, v) }
        if (url.contains("mangomolo.com", ignoreCase = true) ||
            url.contains("sportscastafrica.com", ignoreCase = true)
        ) {
            headers.putAll(SabcChannels.mangomoloHeaders())
        }
        if (!headers.containsKey("Referer")) {
            when {
                url.contains("bozztv.com", ignoreCase = true) ||
                    url.contains("viewmedia.tv", ignoreCase = true) -> {
                    runCatching {
                        val uri = java.net.URI(url)
                        headers["Referer"] = "${uri.scheme}://${uri.host}/"
                    }
                }
                url.contains("approvaltv.com", ignoreCase = true) -> {
                    headers["Referer"] = "https://live.approvaltv.com/"
                }
                url.contains("mediatool.tv", ignoreCase = true) -> {
                    headers["Referer"] = "https://www.ntv.na/"
                }
                url.contains("telemedia.co.za", ignoreCase = true) -> {
                    headers["Referer"] = "https://www.etv.co.za/"
                }
                url.contains("castr.net", ignoreCase = true) -> {
                    headers["Referer"] = "https://zbc.ottplatform.com/"
                    headers["Origin"] = "https://zbc.ottplatform.com"
                }
                url.contains("viloud.tv", ignoreCase = true) -> {
                    headers["Referer"] = "https://ztnonline.co.zw/"
                }
            }
        }
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = com.charlztech.tv.data.remote.StreamApiService.USER_AGENT
        }
        return headers
    }

    private fun buildFromServer(
        title: String,
        slug: String,
        server: StreamServer,
        servers: List<StreamServer>,
        prevSlug: String?,
        nextSlug: String?,
        posterUrl: String? = null
    ): PlaybackRequest? {
        val link = server.link ?: return null
        val (rawUrl, headers) = StreamLinkParser.parse(link)
        val url = when {
            StreamLinkParser.isDirectStream(rawUrl) -> rawUrl
            !server.webLink.isNullOrBlank() -> server.webLink
            else -> rawUrl
        }
        val streamType = when (server.type) {
            "7" -> StreamType.DASH
            else -> when {
                url.contains(".mpd", ignoreCase = true) -> StreamType.DASH
                url.contains(".mp4", ignoreCase = true) -> StreamType.MP4
                StreamLinkParser.isDirectStream(url) -> StreamType.HLS
                else -> StreamType.EMBED
            }
        }
        val drm = server.api?.split(":")
        return PlaybackRequest(
            title = title,
            slug = slug,
            url = url,
            headers = headers,
            drmKid = drm?.getOrNull(0),
            drmKey = drm?.getOrNull(1),
            streamType = streamType,
            servers = servers,
            prevSlug = prevSlug,
            nextSlug = nextSlug,
            posterUrl = posterUrl,
            isLiveBroadcast = streamType == StreamType.HLS || streamType == StreamType.DASH
        )
    }

    fun groupEventsByCategory(events: List<LiveEventUi>): Map<String, List<LiveEventUi>> =
        events.groupBy { it.category }.mapValues { (_, list) ->
            list.sortedWith(
                compareByDescending<LiveEventUi> { it.status == EventStatus.LIVE }
                    .thenByDescending { it.status == EventStatus.UPCOMING }
                    .thenBy { EventStatusUtils.parseStartTime(it.event) ?: Long.MAX_VALUE }
            )
        }

    fun searchEvents(query: String): List<LiveEventUi> {
        if (query.isBlank()) return _liveEvents.value
        return _liveEvents.value.filter {
            val haystack = listOfNotNull(
                it.displayTitle,
                it.event.title,
                it.event.eventInfo?.teamA,
                it.event.eventInfo?.teamB,
                it.event.eventInfo?.eventName,
                it.category
            ).joinToString(" ")
            haystack.contains(query, ignoreCase = true)
        }
    }

    suspend fun getFavorites(): List<FavoriteEntity> = favoritesStore.getAll()

    suspend fun toggleFavorite(
        id: String,
        title: String,
        type: String,
        slug: String? = null,
        url: String? = null,
        posterUrl: String? = null
    ): Boolean {
        return if (favoritesStore.exists(id)) {
            favoritesStore.delete(id)
            false
        } else {
            favoritesStore.insert(
                FavoriteEntity(id = id, title = title, type = type, slug = slug, url = url, posterUrl = posterUrl)
            )
            true
        }
    }

    suspend fun isFavorite(id: String): Boolean = favoritesStore.exists(id)

    private fun toUi(event: LiveEvent): LiveEventUi {
        val status = EventStatusUtils.getStatus(event)
        val serverCount = event.formats?.size ?: 0
        return LiveEventUi(
            event = event,
            displayTitle = EventStatusUtils.displayTitle(event),
            status = status,
            category = EventStatusUtils.category(event),
            posterUrl = event.image?.takeIf { it.isNotBlank() },
            serverCount = serverCount,
            scheduleLabel = EventStatusUtils.formatScheduleLabel(event),
            scheduleDetail = EventStatusUtils.formatScheduleDetail(event, status)
        )
    }
}
