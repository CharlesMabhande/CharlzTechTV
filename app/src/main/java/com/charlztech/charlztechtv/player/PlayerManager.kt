package com.charlztech.charlztechtv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.charlztech.charlztechtv.data.model.PlaybackRequest
import com.charlztech.charlztechtv.data.model.StreamType
import com.charlztech.charlztechtv.data.remote.StreamApiService
import com.charlztech.charlztechtv.util.NewsChannels
import com.charlztech.charlztechtv.util.RegionalStreamResolver
import com.charlztech.charlztechtv.util.SabcChannels
import com.charlztech.charlztechtv.util.StreamLinkParser

/**
 * Live playback:
 * 1. Fill 30 seconds of buffer before the first frame plays.
 * 2. Keep buffering up to 3 minutes ahead during playback.
 */
@OptIn(UnstableApi::class)
class PlayerManager(private val context: Context) {

    var player: ExoPlayer? = null
        private set

    var onPlaybackError: ((PlaybackException) -> Unit)? = null
    var onPlayerUpdated: (() -> Unit)? = null
    var onPrimingChanged: ((Boolean) -> Unit)? = null

    private var isLiveBroadcast = false
    private var lastRequest: PlaybackRequest? = null
    private var errorRetries = 0
    private var isPriming = false
    private var playbackStarted = false
    private var primeStartedAtMs = 0L
    private var seekedForPrime = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val primeBufferCheck = object : Runnable {
        override fun run() {
            val exo = player
            if (exo == null || !isPriming) return

            val target = resolvePrimeTargetMs(exo)
            val ahead = bufferedAheadMs(exo)

            if (ahead >= target) {
                finishPriming()
                return
            }

            if (elapsedPrimeMs() >= PRIME_TIMEOUT_MS) {
                finishPriming()
                return
            }

            mainHandler.postDelayed(this, PRIME_CHECK_INTERVAL_MS)
        }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val exo = player ?: return

            if (isPriming) {
                if (state == Player.STATE_READY && isLiveBroadcast && !seekedForPrime) {
                    seekedForPrime = true
                    try {
                        exo.seekTo(0)
                    } catch (_: Exception) {
                    }
                }
                if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
                    schedulePrimeCheck()
                }
                if (state == Player.STATE_READY && playbackStarted && exo.isPlaying) {
                    isPriming = false
                    onPrimingChanged?.invoke(false)
                }
                return
            }

            if (!isLiveBroadcast) return
            if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
                exo.playWhenReady = true
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPriming && isPlaying) {
                finishPriming()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isPriming || !isLiveBroadcast || playWhenReady) return
            player?.playWhenReady = true
        }

        override fun onPlayerError(error: PlaybackException) {
            if (isLiveBroadcast && isBehindLiveWindow(error)) {
                recoverBehindLiveWindow()
                return
            }
            val request = lastRequest
            if (request != null && errorRetries < MAX_ERROR_RETRIES) {
                errorRetries++
                val delayMs = (errorRetries * 2_000L).coerceAtMost(8_000L)
                mainHandler.postDelayed({ replayCurrent(request) }, delayMs)
            } else {
                errorRetries = 0
                onPlaybackError?.invoke(error)
            }
        }
    }

    private fun elapsedPrimeMs(): Long {
        return SystemClock.elapsedRealtime() - primeStartedAtMs
    }

    private fun bufferedAheadMs(exo: ExoPlayer): Long {
        val buffered = exo.bufferedPosition
        val current = exo.currentPosition
        if (buffered == C.TIME_UNSET || current == C.TIME_UNSET) return 0L
        return (buffered - current).coerceAtLeast(0L)
    }

    private fun resolvePrimeTargetMs(exo: ExoPlayer): Long {
        val duration = exo.duration
        return if (duration != C.TIME_UNSET && duration in 1..PRIME_BUFFER_MS) {
            (duration - 2_000).coerceAtLeast(5_000)
        } else {
            PRIME_BUFFER_MS
        }
    }

    private fun schedulePrimeCheck() {
        mainHandler.removeCallbacks(primeBufferCheck)
        mainHandler.post(primeBufferCheck)
    }

    private fun beginPriming() {
        isPriming = true
        playbackStarted = false
        seekedForPrime = false
        primeStartedAtMs = SystemClock.elapsedRealtime()
        onPrimingChanged?.invoke(true)
        schedulePrimeCheck()
    }

    private fun finishPriming() {
        if (!isPriming) return
        isPriming = false
        playbackStarted = true
        mainHandler.removeCallbacks(primeBufferCheck)
        onPrimingChanged?.invoke(false)
        player?.playWhenReady = true
    }

    private fun isBehindLiveWindow(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
    }

    private fun recoverBehindLiveWindow() {
        val exo = player ?: return
        try {
            exo.seekToDefaultPosition()
            if (exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED) {
                exo.prepare()
            }
            exo.playWhenReady = true
        } catch (_: Exception) {
        }
    }

    private fun replayCurrent(request: PlaybackRequest) {
        startPlaybackInternal(request, resetRetries = false)
    }

    private fun ensurePlayer(): ExoPlayer {
        if (player != null) return player!!

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ MIN_BUFFER_DURING_PLAYBACK,
                /* maxBufferMs */ MAX_BUFFER_MS.toInt(),
                /* bufferForPlaybackMs */ 500,
                /* bufferForPlaybackAfterRebufferMs */ REBUFFER_RESUME_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true)
            .build()

        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .also { exo ->
                exo.addListener(listener)
                exo.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false
                )
                exo.setWakeMode(C.WAKE_MODE_NETWORK)
                exo.playWhenReady = true
            }
        onPlayerUpdated?.invoke()
        return player!!
    }

    fun play(request: PlaybackRequest) {
        errorRetries = 0
        startPlaybackInternal(request, resetRetries = true)
    }

    private fun startPlaybackInternal(request: PlaybackRequest, resetRetries: Boolean) {
        if (resetRetries) errorRetries = 0
        lastRequest = request
        isLiveBroadcast = request.isLiveBroadcast
        val exoPlayer = ensurePlayer()
        val url = request.url ?: return
        val headers = buildHeaders(url, request.headers)

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(60_000)
            .setDefaultRequestProperties(headers)

        val mediaItem = buildMediaItem(url, request)
        val mediaSource = buildMediaSource(request, mediaItem, dataSourceFactory)

        mainHandler.removeCallbacks(primeBufferCheck)
        exoPlayer.setMediaSource(mediaSource, true)

        if (isLiveBroadcast) {
            beginPriming()
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        } else {
            isPriming = false
            playbackStarted = true
            onPrimingChanged?.invoke(false)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    private fun buildMediaSource(
        request: PlaybackRequest,
        mediaItem: MediaItem,
        dataSourceFactory: DefaultHttpDataSource.Factory
    ): MediaSource {
        return when (request.streamType) {
            StreamType.DASH -> buildDashSource(mediaItem, dataSourceFactory, request)
            StreamType.MP4 -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            else -> HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        }
    }

    private fun buildMediaItem(url: String, request: PlaybackRequest): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (request.isLiveBroadcast && request.streamType != StreamType.MP4) {
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(TARGET_LIVE_OFFSET_MS)
                    .setMinOffsetMs(5_000)
                    .setMaxOffsetMs(MAX_BUFFER_MS)
                    .setMinPlaybackSpeed(1.0f)
                    .setMaxPlaybackSpeed(1.0f)
                    .build()
            )
        }
        return builder.build()
    }

    private fun buildHeaders(url: String, extra: Map<String, String>): Map<String, String> {
        val headers = extra.toMutableMap()
        headers.putAll(RegionalStreamResolver.headersForUrl(url))
        headers.putAll(NewsChannels.headersForUrl(url))
        headers.putAll(SabcChannels.headersForUrl(url))
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = StreamApiService.USER_AGENT
        }
        if (!headers.containsKey("Referer") && url.startsWith("http")) {
            headers["Referer"] = url.substringBeforeLast("/")
        }
        headers["Connection"] = "keep-alive"
        return headers
    }

    private fun buildDashSource(
        mediaItem: MediaItem,
        factory: DefaultHttpDataSource.Factory,
        request: PlaybackRequest
    ): MediaSource {
        val builder = mediaItem.buildUpon().setMimeType(MimeTypes.APPLICATION_MPD)
        if (!request.drmKey.isNullOrBlank() && !request.drmKid.isNullOrBlank()) {
            val kid = toBase64Uuid(request.drmKid)
            val key = toBase64Uuid(request.drmKey)
            builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(
                        "data:application/json,{\"keys\":[{\"kty\":\"oct\",\"k\":\"$key\",\"kid\":\"$kid\"}],\"type\":\"temporary\"}"
                    )
                    .build()
            )
        }
        return DashMediaSource.Factory(factory).createMediaSource(builder.build())
    }

    private fun toBase64Uuid(hexOrBase64: String): String {
        return try {
            if (hexOrBase64.length == 32) {
                val bytes = hexOrBase64.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            } else {
                hexOrBase64
            }
        } catch (_: Exception) {
            hexOrBase64
        }
    }

    fun resumePlayback() {
        player?.playWhenReady = true
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        isPriming = false
        playbackStarted = false
        player?.removeListener(listener)
        player?.release()
        player = null
        lastRequest = null
        errorRetries = 0
        onPlayerUpdated?.invoke()
    }

    fun switchServer(request: PlaybackRequest, serverLink: String, serverType: String?, drmApi: String?) {
        val (url, headers) = StreamLinkParser.parse(serverLink)
        val streamType = when (serverType) {
            "7" -> StreamType.DASH
            else -> when {
                url.contains(".mpd", ignoreCase = true) -> StreamType.DASH
                url.contains(".mp4", ignoreCase = true) -> StreamType.MP4
                StreamLinkParser.isDirectStream(url) -> StreamType.HLS
                else -> StreamType.EMBED
            }
        }
        val drm = drmApi?.split(":")
        play(
            request.copy(
                url = url,
                headers = request.headers + headers,
                streamType = streamType,
                drmKid = drm?.getOrNull(0),
                drmKey = drm?.getOrNull(1),
                isLiveBroadcast = request.isLiveBroadcast ||
                    streamType == StreamType.HLS || streamType == StreamType.DASH
            )
        )
    }

    companion object {
        private const val PRIME_BUFFER_MS = 30_000L
        private const val PRIME_TIMEOUT_MS = 45_000L
        private const val PRIME_CHECK_INTERVAL_MS = 300L
        private const val MIN_BUFFER_DURING_PLAYBACK = 10_000
        private const val REBUFFER_RESUME_MS = 5_000
        private const val MAX_BUFFER_MS = 180_000L
        private const val TARGET_LIVE_OFFSET_MS = 30_000L
        private const val MAX_ERROR_RETRIES = 6
    }
}
