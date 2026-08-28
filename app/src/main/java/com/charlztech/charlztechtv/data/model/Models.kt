package com.charlztech.charlztechtv.data.model

import java.io.Serializable as JavaSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Provider(
    val id: Int,
    val title: String,
    val image: String = "",
    @SerialName("catLink") val catLink: String? = null
)

@Serializable
data class LiveEvent(
    val id: Int,
    val title: String,
    val image: String? = null,
    val slug: String,
    val cat: String? = null,
    val publish: Int = 1,
    val eventInfo: EventInfo? = null,
    val formats: List<EventFormat>? = null
)

@Serializable
data class EventInfo(
    val teamA: String? = null,
    val teamB: String? = null,
    val teamAFlag: String? = null,
    val teamBFlag: String? = null,
    val eventCat: String? = null,
    val eventName: String? = null,
    val eventLogo: String? = null,
    val isHot: String? = null,
    val eventType: String? = null,
    val startTime: String? = null,
    val endTime: String? = null
)

@Serializable
data class EventFormat(
    val title: String? = null,
    val webLink: String? = null
)

@Serializable
data class ChannelStreamResponse(
    val streamUrls: List<StreamServer>? = null,
    val related: List<RelatedChannel>? = null,
    val prevChannel: String? = null,
    val nextChannel: String? = null
)

@Serializable
data class StreamServer(
    val api: String? = null,
    val id: Int? = null,
    val link: String? = null,
    val title: String? = null,
    val type: String? = null,
    val webLink: String? = null
) : JavaSerializable

@Serializable
data class RelatedChannel(
    val title: String? = null,
    val slug: String? = null,
    val image: String? = null
)

data class M3uChannel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String? = null,
    val referer: String? = null,
    val cookie: String? = null,
    val licenseString: String? = null,
    val isDrm: Boolean = false
)

enum class EventStatus {
    LIVE, UPCOMING, ENDED, UNKNOWN
}

data class LiveEventUi(
    val event: LiveEvent,
    val displayTitle: String,
    val status: EventStatus,
    val category: String,
    val posterUrl: String?,
    val serverCount: Int = 0,
    val scheduleLabel: String? = null,
    val scheduleDetail: String? = null
)

data class PlaybackRequest(
    val title: String,
    val slug: String? = null,
    val url: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val drmKey: String? = null,
    val drmKid: String? = null,
    val streamType: StreamType = StreamType.HLS,
    val servers: List<StreamServer> = emptyList(),
    val prevSlug: String? = null,
    val nextSlug: String? = null,
    val posterUrl: String? = null,
    val isLiveBroadcast: Boolean = false
) : JavaSerializable

enum class StreamType : java.io.Serializable {
    HLS, DASH, MP4, EMBED
}
