@file:OptIn(ExperimentalMaterial3Api::class)

package com.charlztech.charlztechtv.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.charlztech.charlztechtv.CharlzTechTvApp
import com.charlztech.charlztechtv.R
import com.charlztech.charlztechtv.data.model.PlaybackRequest
import com.charlztech.charlztechtv.data.model.StreamType
import com.charlztech.charlztechtv.ui.components.StreamServerCard
import com.charlztech.charlztechtv.ui.theme.AppColors
import com.charlztech.charlztechtv.ui.theme.CharlzTechTvTheme
import com.charlztech.charlztechtv.util.EmbedUrlResolver
import com.charlztech.charlztechtv.util.NewsChannels
import com.charlztech.charlztechtv.util.RegionalStreamResolver
import com.charlztech.charlztechtv.util.SabcChannels
import com.charlztech.charlztechtv.util.StreamLinkParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerActivity : ComponentActivity() {

    private lateinit var playerManager: PlayerManager
    private val requestState = mutableStateOf<PlaybackRequest?>(null)
    private var currentServerIndex = 0
    private val serverIndexState = mutableStateOf(0)

    private val playerGeneration = mutableStateOf(0)
    private val isPrimingState = mutableStateOf(false)

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        FullscreenHelper.allowRotation(this)
        playerManager = PlayerManager(this)
        playerManager.onPlayerUpdated = { playerGeneration.value++ }
        playerManager.onPrimingChanged = { isPrimingState.value = it }
        currentServerIndex = 0
        serverIndexState.value = 0

        playerManager.onPlaybackError = { tryNextServer() }

        val initialRequest = loadPlaybackRequest()

        if (initialRequest != null) {
            startPlayback(initialRequest)
        }

        setContent {
            CharlzTechTvTheme {
                // Read generation so PlayerView re-binds when ExoPlayer is created
                val generation = playerGeneration.value
                val isPriming = isPrimingState.value
                PlayerScreen(
                    request = requestState.value,
                    playerManager = playerManager,
                    playerGeneration = generation,
                    isPriming = isPriming,
                    currentServerIndex = serverIndexState.value,
                    onBack = { finish() },
                    onNavigate = { slug, title -> navigateToSlug(slug, title) },
                    onRetry = { req -> retryPlayback(req) },
                    onServerSelected = { index -> switchToServer(index) },
                    onToggleFullscreen = { toggleFullscreen() }
                )
            }
        }

    }

    private fun loadPlaybackRequest(): PlaybackRequest? {
        val fromSession = PlaybackSession.consume()
        if (fromSession != null) {
            requestState.value = fromSession
            return fromSession
        }
        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return null
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        lifecycleScope.launch {
            val repo = (application as CharlzTechTvApp).repository
            val playback = repo.buildPlaybackForSlug(slug, title)
            if (playback != null) {
                requestState.value = playback
                startPlayback(playback)
            }
        }
        return null
    }

    private fun startPlayback(request: PlaybackRequest) {
        val resolved = request.copy(url = EmbedUrlResolver.resolve(request.url.orEmpty()))
        requestState.value = resolved
        if (resolved.streamType == StreamType.EMBED ||
            EmbedUrlResolver.isWebEmbed(resolved.url.orEmpty()) ||
            !StreamLinkParser.isDirectStream(resolved.url.orEmpty())
        ) {
            return
        }
        playerManager.play(resolved)
    }

    private fun retryPlayback(request: PlaybackRequest) {
        currentServerIndex = 0
        serverIndexState.value = 0
        startPlayback(request)
    }

    private fun switchToServer(index: Int) {
        val request = requestState.value ?: return
        val servers = request.servers
        if (index !in servers.indices) return
        currentServerIndex = index
        serverIndexState.value = index
        val server = servers[index]
        val link = server.link ?: return
        val (url, extraHeaders) = StreamLinkParser.parse(link)
        val headers = request.headers.toMutableMap()
        headers.putAll(extraHeaders)
        headers.putAll(RegionalStreamResolver.headersForUrl(url))
        headers.putAll(NewsChannels.headersForUrl(url))
        headers.putAll(SabcChannels.headersForUrl(url))
        val streamType = when {
            EmbedUrlResolver.isWebEmbed(url) -> StreamType.EMBED
            url.contains(".mpd", ignoreCase = true) -> StreamType.DASH
            url.contains(".mp4", ignoreCase = true) -> StreamType.MP4
            StreamLinkParser.isDirectStream(url) -> StreamType.HLS
            else -> StreamType.EMBED
        }
        val updated = request.copy(
            url = url,
            headers = headers,
            streamType = streamType,
            isLiveBroadcast = false
        )
        requestState.value = updated
        startPlayback(updated)
    }

    private fun tryNextServer() {
        val request = requestState.value ?: return
        val servers = request.servers
        if (servers.size <= 1) return
        val nextIndex = currentServerIndex + 1
        if (nextIndex < servers.size) {
            switchToServer(nextIndex)
        }
    }

    private fun toggleFullscreen() {
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            FullscreenHelper.exitFullscreen(this)
            FullscreenHelper.allowRotation(this)
        } else {
            FullscreenHelper.rotateToLandscape(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            FullscreenHelper.enterFullscreen(this)
        } else {
            FullscreenHelper.exitFullscreen(this)
        }
    }

    private fun navigateToSlug(slug: String, title: String) {
        lifecycleScope.launch {
            val repo = (application as CharlzTechTvApp).repository
            val playback = repo.buildPlaybackForSlug(slug, title)
            if (playback != null) {
                requestState.value = playback
                currentServerIndex = 0
                serverIndexState.value = 0
                startPlayback(playback)
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        playerManager.resumePlayback()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_SLUG = "slug"
        private const val EXTRA_TITLE = "title"

        fun intent(context: Context, slug: String?, title: String): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_SLUG, slug)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(
    request: PlaybackRequest?,
    playerManager: PlayerManager,
    playerGeneration: Int,
    isPriming: Boolean,
    currentServerIndex: Int,
    onBack: () -> Unit,
    onNavigate: (String, String) -> Unit,
    onRetry: (PlaybackRequest) -> Unit,
    onServerSelected: (Int) -> Unit,
    onToggleFullscreen: () -> Unit
) {
    var useWebView by remember { mutableStateOf(false) }
    var embedUrl by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(request, currentServerIndex) {
        if (request == null) return@LaunchedEffect
        val resolvedUrl = EmbedUrlResolver.resolve(request.url.orEmpty())
        val needsEmbed = request.streamType == StreamType.EMBED ||
            EmbedUrlResolver.isWebEmbed(resolvedUrl) ||
            !StreamLinkParser.isDirectStream(resolvedUrl)
        useWebView = needsEmbed
        embedUrl = resolvedUrl.ifBlank { request.url }
    }

    val tryNextEmbedServer = {
        val req = request
        if (req != null && currentServerIndex + 1 < req.servers.size) {
            onServerSelected(currentServerIndex + 1)
        }
    }

    LaunchedEffect(request, useWebView, currentServerIndex) {
        if (!useWebView || request == null || request.servers.size <= 1) return@LaunchedEffect
        delay(8_000)
        if (useWebView) {
            onServerSelected(currentServerIndex + 1)
        }
    }

    if (isLandscape) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PlayerContent(
                request = request,
                playerManager = playerManager,
                playerGeneration = playerGeneration,
                isPriming = isPriming,
                useWebView = useWebView,
                embedUrl = embedUrl,
                onRetry = onRetry,
                onStreamFound = { streamUrl ->
                    useWebView = false
                    request?.let { onRetry(playbackFromExtractedStream(it, streamUrl)) }
                },
                onLoadFinished = tryNextEmbedServer
            )
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = null, tint = Color.White)
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(request?.title.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFullscreen) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                    }
                    if (request != null) {
                        IconButton(onClick = { onRetry(request) }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background,
                    titleContentColor = AppColors.TextPrimary
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                PlayerContent(
                    request = request,
                    playerManager = playerManager,
                    playerGeneration = playerGeneration,
                    isPriming = isPriming,
                    useWebView = useWebView,
                    embedUrl = embedUrl,
                    onRetry = onRetry,
                    onStreamFound = { streamUrl ->
                        useWebView = false
                        request?.let { onRetry(playbackFromExtractedStream(it, streamUrl)) }
                    },
                    onLoadFinished = tryNextEmbedServer
                )
                if (!isPriming) {
                    PortraitRotateButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    )
                }
            }

            if (request != null && request.servers.isNotEmpty()) {
                Text(
                    "${stringResource(R.string.servers)} (${request.servers.size})",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((request.servers.size.coerceAtMost(4) * 72).dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(request.servers) { index, server ->
                        StreamServerCard(
                            server = server,
                            index = index,
                            isActive = index == currentServerIndex,
                            onClick = { onServerSelected(index) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NavChip(
                    enabled = !request?.prevSlug.isNullOrBlank(),
                    label = stringResource(R.string.previous),
                    icon = Icons.AutoMirrored.Filled.ArrowBack
                ) { request?.prevSlug?.let { onNavigate(it, "Previous Event") } }
                NavChip(
                    enabled = !request?.nextSlug.isNullOrBlank(),
                    label = stringResource(R.string.next),
                    icon = Icons.AutoMirrored.Filled.ArrowForward
                ) { request?.nextSlug?.let { onNavigate(it, "Next Event") } }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerContent(
    request: PlaybackRequest?,
    playerManager: PlayerManager,
    playerGeneration: Int,
    isPriming: Boolean,
    useWebView: Boolean,
    embedUrl: String?,
    onRetry: (PlaybackRequest) -> Unit,
    onStreamFound: (String) -> Unit,
    onLoadFinished: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var boundPlayer by remember(playerGeneration) { mutableStateOf(playerManager.player) }

    LaunchedEffect(playerGeneration) {
        boundPlayer = playerManager.player
    }

    when {
        request == null -> Text(stringResource(R.string.error_stream), color = Color.White)
        useWebView && !embedUrl.isNullOrBlank() -> EmbedWebPlayer(
            url = embedUrl,
            onStreamFound = onStreamFound,
            onLoadFinished = onLoadFinished
        )
        else -> Box(Modifier.fillMaxSize()) {
            AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    try {
                        setShowRewindButton(false)
                        setShowFastForwardButton(false)
                        setShowPreviousButton(false)
                        setShowNextButton(false)
                    } catch (_: Exception) {
                    }
                    keepScreenOn = true
                    controllerShowTimeoutMs = 1500
                    controllerHideOnTouch = true
                    controllerAutoShow = false
                    player = boundPlayer
                }
            },
            update = { view ->
                if (view.player !== boundPlayer) {
                    view.player = boundPlayer
                }
                view.useController = !isPriming
                view.resizeMode = if (isLandscape) {
                    AspectRatioFrameLayout.RESIZE_MODE_FILL
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.player = null }
            )
            if (isPriming) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.buffering_live),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun NavChip(
    enabled: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) Color.White else Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (enabled) Color.White else Color.Gray)
    }
}

@Composable
private fun PortraitRotateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.ScreenRotation,
                contentDescription = stringResource(R.string.rotate_screen),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                stringResource(R.string.rotate_screen),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbedWebPlayer(
    url: String,
    onStreamFound: (String) -> Unit,
    onLoadFinished: () -> Unit
) {
    val context = LocalContext.current
    val refererOrigin = remember(url) {
        EmbedUrlResolver.pageReferer(url, context.packageName)
    }
    var found by remember(url) { mutableStateOf(false) }
    val resolvedUrl = remember(url) { EmbedUrlResolver.resolve(url) }
    val useIframeHtml = remember(resolvedUrl) { EmbedUrlResolver.isIframeEmbed(resolvedUrl) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): android.webkit.WebResourceResponse? {
                        val reqUrl = request.url.toString()
                        if (!found && isPlayableStreamUrl(reqUrl)) {
                            found = true
                            post { onStreamFound(reqUrl) }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView, pageUrl: String) {
                        super.onPageFinished(view, pageUrl)
                        val scripts = listOf(
                            "(function(){if(typeof playbackURL!=='undefined'&&playbackURL){return playbackURL;}return '';})();",
                            "(function(){if(typeof file!=='undefined'&&file){return file;}return '';})();",
                            "(function(){var html=document.documentElement.innerHTML;var m=html.match(/src:\\s*\"([^\"]+\\.m3u8[^\"]*)\"/);if(m)return m[1];m=html.match(/(https:\\/\\/[^\"'\\s]+aniview[^\"'\\s]+\\.m3u8[^\"'\\s]*)/);if(m)return m[1];m=html.match(/(https:\\/\\/[^\"'\\s]+massmedia[^\"'\\s]+\\.m3u8[^\"'\\s]*)/);return m?m[1]:'';})();",
                            "(function(){var v=document.querySelector('video');if(v&&v.src)return v.src;return '';})();",
                            "(function(){var s=document.querySelector('source');if(s&&s.src)return s.src;return '';})();"
                        )
                        scripts.forEach { script ->
                            evaluateJavascript(script) { result ->
                                val cleaned = result?.trim('"').orEmpty()
                                if (!found && cleaned.isNotBlank() && isPlayableStreamUrl(cleaned)) {
                                    found = true
                                    onStreamFound(cleaned)
                                }
                            }
                        }
                        postDelayed({
                            if (!found) onLoadFinished()
                        }, 3500)
                    }
                }
                when {
                    useIframeHtml -> loadDataWithBaseURL(
                        refererOrigin,
                        EmbedUrlResolver.buildIframeHtml(resolvedUrl, refererOrigin),
                        "text/html",
                        "utf-8",
                        null
                    )
                    else -> loadUrl(
                        resolvedUrl,
                        mapOf(
                            "Referer" to refererOrigin,
                            "Referrer-Policy" to "strict-origin-when-cross-origin"
                        )
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun playbackFromExtractedStream(request: PlaybackRequest, streamUrl: String): PlaybackRequest {
    val headers = request.headers.toMutableMap()
        headers.putAll(SabcChannels.headersForUrl(streamUrl))
        headers.putAll(RegionalStreamResolver.headersForUrl(streamUrl))
        headers.putAll(NewsChannels.headersForUrl(streamUrl))
    val streamType = streamTypeForExtractedUrl(streamUrl)
    return request.copy(
        url = streamUrl,
        streamType = streamType,
        headers = headers,
        isLiveBroadcast = streamType == StreamType.HLS || streamType == StreamType.DASH
    )
}

private fun isPlayableStreamUrl(url: String): Boolean {
    val lower = url.lowercase()
    return StreamLinkParser.isDirectStream(url) ||
        lower.contains("aniview.global") ||
        lower.contains("massmedia.co.bw") ||
        lower.contains("googlevideo.com") ||
        lower.contains("videoplayback") ||
        lower.contains("mime=video")
}

private fun streamTypeForExtractedUrl(url: String): StreamType = when {
    url.contains(".mpd", ignoreCase = true) -> StreamType.DASH
    url.contains(".m3u8", ignoreCase = true) -> StreamType.HLS
    url.contains(".mp4", ignoreCase = true) ||
        url.contains("googlevideo.com", ignoreCase = true) ||
        url.contains("videoplayback", ignoreCase = true) -> StreamType.MP4
    StreamLinkParser.isDirectStream(url) -> StreamType.HLS
    else -> StreamType.MP4
}
