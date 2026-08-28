package com.charlztech.tv.player

import com.charlztech.tv.data.model.PlaybackRequest
import com.charlztech.tv.data.model.StreamType
import com.charlztech.tv.data.remote.StreamApiService
import com.charlztech.tv.util.NewsChannels
import com.charlztech.tv.util.RegionalStreamResolver
import com.charlztech.tv.util.SabcChannels
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Color
import java.awt.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

/**
 * Tuned for continuous live playback on weak networks:
 * - bigger network buffer (Android-like min ~10s ahead)
 * - HLS picks a lower adaptive bitrate so streams don't stall
 * - never restart mid-buffer (that causes "frequent stopping")
 * - reconnect only on real VLC errors
 *
 * Avoids options that previously broke start (http-continuous, 20s cache, clock-synchro).
 */
class VlcPlayerManager private constructor(
    val videoComponent: EmbeddedMediaPlayerComponent
) {
    private var lastRequest: PlaybackRequest? = null
    private var errorRetries = 0
    private var onError: (() -> Unit)? = null
    private var onPrimingChanged: ((Boolean) -> Unit)? = null
    private var onStatusChanged: ((String) -> Unit)? = null
    private val priming = AtomicBoolean(false)
    private var hasStartedPlayback = false
    private var userStopped = false
    private var primingTimeout: ScheduledFuture<*>? = null
    private var reconnectTask: ScheduledFuture<*>? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "vlc-player-scheduler").apply { isDaemon = true }
    }

    init {
        styleVideoComponent()
        videoComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun opening(mediaPlayer: MediaPlayer) {
                onStatusChanged?.invoke("Opening stream…")
            }

            override fun playing(mediaPlayer: MediaPlayer) {
                hasStartedPlayback = true
                errorRetries = 0
                finishPriming()
                onStatusChanged?.invoke("Playing")
                refreshSurfaceBurst()
            }

            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                // Important: do NOT restart playback while buffering — just wait and refill.
                if (!hasStartedPlayback) {
                    onStatusChanged?.invoke("Buffering ${newCache.toInt()}%")
                    if (priming.get() && newCache >= PRIME_TARGET_CACHE_PERCENT) {
                        finishPriming()
                    }
                    return
                }
                if (newCache < REBUFFER_WARNING_PERCENT) {
                    onStatusChanged?.invoke("Buffering…")
                } else {
                    onStatusChanged?.invoke("Playing")
                }
            }

            override fun error(mediaPlayer: MediaPlayer) {
                log("vlc error for ${lastRequest?.title}")
                scheduleErrorReconnect(lastRequest)
            }
        })
    }

    fun swingComponent(): Component = videoComponent

    fun setOnError(listener: () -> Unit) {
        onError = listener
    }

    fun setOnPrimingChanged(listener: (Boolean) -> Unit) {
        onPrimingChanged = listener
    }

    fun setOnStatusChanged(listener: (String) -> Unit) {
        onStatusChanged = listener
    }

    fun onSurfaceResized() {
        SwingUtilities.invokeLater {
            runCatching {
                val player = videoComponent.mediaPlayer()
                player.video().setAspectRatio(null)
                player.video().setScale(0f)
                videoComponent.revalidate()
                videoComponent.repaint()
            }
        }
    }

    fun refreshSurfaceBurst() {
        onSurfaceResized()
        listOf(50L, 150L).forEach { delayMs ->
            scheduler.schedule({ onSurfaceResized() }, delayMs, TimeUnit.MILLISECONDS)
        }
    }

    fun play(request: PlaybackRequest, resetRetries: Boolean = true) {
        userStopped = false
        reconnectTask?.cancel(false)
        lastRequest = request
        playInternal(request, resetRetries)
    }

    private fun playInternal(request: PlaybackRequest, resetRetries: Boolean) {
        val url = request.url?.trim().orEmpty()
        if (url.isBlank()) {
            onStatusChanged?.invoke("No stream URL")
            onError?.invoke()
            return
        }
        if (resetRetries) errorRetries = 0
        hasStartedPlayback = false
        lastRequest = request

        val isLive = request.isLiveBroadcast || isLikelyLiveStream(request, url)
        val options = buildVlcOptions(request.headers, url, isLive)
        log("play ${request.title} live=$isLive cache=$LIVE_NETWORK_CACHE_MS options=${options.size}")

        if (isLive) {
            beginPriming()
            onStatusChanged?.invoke("Buffering live stream…")
        } else {
            priming.set(false)
            onPrimingChanged?.invoke(false)
            onStatusChanged?.invoke("Loading…")
        }

        SwingUtilities.invokeLater {
            runCatching {
                val player = videoComponent.mediaPlayer()
                if (player.status().isPlaying) {
                    player.controls().stop()
                }
                player.media().play(url, *options)
                onSurfaceResized()
            }.onFailure {
                log("play failed: ${it.message}")
                scheduleErrorReconnect(request)
            }
        }
    }

    private fun scheduleErrorReconnect(request: PlaybackRequest?) {
        if (request == null || userStopped) {
            onStatusChanged?.invoke("Playback error")
            onError?.invoke()
            return
        }
        reconnectTask?.cancel(false)
        if (errorRetries < MAX_ERROR_RETRIES) {
            errorRetries++
            val delayMs = (errorRetries * 2_000L).coerceAtMost(8_000L)
            onStatusChanged?.invoke("Reconnecting…")
            reconnectTask = scheduler.schedule(
                { if (!userStopped) playInternal(request, resetRetries = false) },
                delayMs,
                TimeUnit.MILLISECONDS
            )
        } else {
            errorRetries = 0
            onStatusChanged?.invoke("Playback error")
            onError?.invoke()
        }
    }

    fun stop() {
        userStopped = true
        reconnectTask?.cancel(false)
        primingTimeout?.cancel(false)
        priming.set(false)
        hasStartedPlayback = false
        onPrimingChanged?.invoke(false)
        SwingUtilities.invokeLater {
            videoComponent.mediaPlayer().controls().stop()
        }
        onStatusChanged?.invoke("Stopped")
    }

    fun release() {
        userStopped = true
        reconnectTask?.cancel(false)
        primingTimeout?.cancel(false)
        scheduler.shutdownNow()
        SwingUtilities.invokeLater {
            runCatching { videoComponent.release() }
        }
        lastRequest = null
        errorRetries = 0
        hasStartedPlayback = false
    }

    private fun styleVideoComponent() {
        videoComponent.background = Color.BLACK
        videoComponent.isOpaque = true
        videoComponent.isDoubleBuffered = false
        // Keep keyboard focus in Compose so Escape / F11 exit fullscreen reliably
        videoComponent.isFocusable = false
    }

    private fun beginPriming() {
        priming.set(true)
        onPrimingChanged?.invoke(true)
        primingTimeout?.cancel(false)
        primingTimeout = scheduler.schedule(
            { finishPriming() },
            PRIME_TIMEOUT_MS,
            TimeUnit.MILLISECONDS
        )
    }

    private fun finishPriming() {
        if (!priming.getAndSet(false)) return
        primingTimeout?.cancel(false)
        onPrimingChanged?.invoke(false)
    }

    private fun isLikelyLiveStream(request: PlaybackRequest, url: String): Boolean {
        if (request.streamType == StreamType.HLS || request.streamType == StreamType.DASH) return true
        return url.contains(".m3u8", ignoreCase = true) ||
            url.contains(".mpd", ignoreCase = true) ||
            url.contains("/live", ignoreCase = true) ||
            url.contains("live.", ignoreCase = true)
    }

    private fun buildVlcOptions(
        extra: Map<String, String>,
        url: String,
        isLive: Boolean
    ): Array<String> {
        val headers = extra.toMutableMap()
        RegionalStreamResolver.headersForUrl(url).forEach { (k, v) -> headers.putIfAbsent(k, v) }
        NewsChannels.headersForUrl(url).forEach { (k, v) -> headers.putIfAbsent(k, v) }
        SabcChannels.headersForUrl(url).forEach { (k, v) -> headers.putIfAbsent(k, v) }
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = StreamApiService.USER_AGENT
        }
        if (!headers.containsKey("Referer") && url.startsWith("http")) {
            headers["Referer"] = url.substringBeforeLast("/")
        }

        val userAgent = headers["User-Agent"] ?: StreamApiService.USER_AGENT
        val referer = headers["Referer"]
        val extraHeaders = headers.filterKeys {
            !it.equals("User-Agent", ignoreCase = true) &&
                !it.equals("Referer", ignoreCase = true) &&
                !it.equals("Connection", ignoreCase = true)
        }
        val headerBlock = buildString {
            referer?.let { append("Referer: $it\r\n") }
            extraHeaders.forEach { (key, value) -> append("$key: $value\r\n") }
        }.trimEnd('\r', '\n')

        val options = mutableListOf<String>()
        // ~10s ahead buffer (Android minBufferMs) — enough for weak wifi without stalling start
        options += ":network-caching=${if (isLive) LIVE_NETWORK_CACHE_MS else VOD_NETWORK_CACHE_MS}"
        options += ":live-caching=$LIVE_CACHE_MS"
        options += ":http-reconnect"
        options += ":http-user-agent=$userAgent"
        referer?.let { options += ":http-referrer=$it" }
        if (headerBlock.isNotBlank()) {
            options += ":http-header=$headerBlock"
        }
        if (url.contains(".m3u8", ignoreCase = true) || url.contains(".mpd", ignoreCase = true)) {
            // Weak-network continuity: prefer lowest playable quality over stuttering HD
            options += ":adaptive-logic=lowest"
            options += ":preferred-resolution=$PREFERRED_LIVE_HEIGHT"
            // Fewer concurrent downloads = less contention on slow links
            options += ":hls-segment-threads=2"
        }
        return options.toTypedArray()
    }

    private fun log(message: String) {
        val line = "${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)} $message"
        runCatching {
            val dir = Path.of(
                System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
                "CharlzTechTV"
            )
            Files.createDirectories(dir)
            Files.writeString(
                dir.resolve("player.log"),
                "$line\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )
        }
    }

    companion object {
        private const val MAX_ERROR_RETRIES = 6
        /** Android MIN_BUFFER_DURING_PLAYBACK ≈ 10s */
        private const val LIVE_NETWORK_CACHE_MS = 10_000
        private const val LIVE_CACHE_MS = 6_000
        private const val VOD_NETWORK_CACHE_MS = 5_000
        private const val PREFERRED_LIVE_HEIGHT = 720
        private const val PRIME_TARGET_CACHE_PERCENT = 10f
        private const val REBUFFER_WARNING_PERCENT = 10f
        private const val PRIME_TIMEOUT_MS = 12_000L

        val isVlcInstalled: Boolean by lazy {
            val bundled = System.getProperty("compose.application.resources.dir")
                ?.let { Path.of(it, "libvlc.dll") }
                ?.let { Files.isRegularFile(it) } == true
            bundled || runCatching { NativeDiscovery().discover() }.getOrDefault(false)
        }

        fun create(): VlcPlayerManager {
            VlcBootstrap.setup()
            // Prefer bundled natives; otherwise discover a system VLC install.
            if (System.getProperty("jna.library.path").isNullOrBlank()) {
                runCatching { NativeDiscovery().discover() }
            }
            val args = mutableListOf(
                "--network-caching=$LIVE_NETWORK_CACHE_MS",
                "--live-caching=$LIVE_CACHE_MS",
                "--http-reconnect",
                "--no-video-title-show",
                "--adaptive-logic=lowest"
            )
            VlcBootstrap.pluginPathArg()?.let { args += it }
            val component = EmbeddedMediaPlayerComponent(*args.toTypedArray())
            return VlcPlayerManager(component)
        }
    }
}
