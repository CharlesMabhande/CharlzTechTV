@file:OptIn(ExperimentalMaterial3Api::class)

package com.charlztech.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.charlztech.tv.LocalAppWindowState
import com.charlztech.tv.data.model.PlaybackRequest
import com.charlztech.tv.data.model.StreamType
import com.charlztech.tv.resources.Strings
import com.charlztech.tv.ui.theme.AppColors
import com.charlztech.tv.ui.util.ScrollableColumn
import com.charlztech.tv.ui.util.ScrollableLazyRow
import com.charlztech.tv.util.EmbedUrlResolver
import com.charlztech.tv.util.NewsChannels
import com.charlztech.tv.util.RegionalStreamResolver
import com.charlztech.tv.util.SabcChannels
import com.charlztech.tv.util.StreamLinkParser
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

@Composable
fun PlayerScreen(
    request: PlaybackRequest?,
    playerManager: VlcPlayerManager,
    isVideoFullscreen: Boolean = false,
    onNavigate: (String, String) -> Unit,
    onStatusUpdate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appWindow = LocalAppWindowState.current
    val scope = rememberCoroutineScope()
    var currentRequest by remember(request) { mutableStateOf(request) }
    var currentServerIndex by remember { mutableIntStateOf(0) }
    var isPriming by remember { mutableStateOf(false) }
    var resolvingEmbed by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    val vlcMissing = remember { !VlcPlayerManager.isVlcInstalled }

    DisposableEffect(playerManager) {
        playerManager.setOnError {
            playbackError = "Could not play this stream. Try another server below."
            val req = currentRequest
            if (req != null && currentServerIndex + 1 < req.servers.size) {
                currentServerIndex++
                switchServer(req, currentServerIndex) { updated ->
                    currentRequest = updated
                    playbackError = null
                    startPlayback(updated, playerManager, scope, { resolvingEmbed = it }, { playbackError = it })
                }
            }
        }
        playerManager.setOnPrimingChanged { isPriming = it }
        playerManager.setOnStatusChanged(onStatusUpdate)
        onDispose { }
    }

    LaunchedEffect(currentRequest?.url, vlcMissing) {
        val req = currentRequest ?: return@LaunchedEffect
        if (vlcMissing) return@LaunchedEffect
        playbackError = null
        startPlayback(req, playerManager, scope, { resolvingEmbed = it }, { playbackError = it })
    }

    LaunchedEffect(isVideoFullscreen) {
        playerManager.refreshSurfaceBurst()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isVideoFullscreen) Modifier.fillMaxSize().background(Color.Black)
                else Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
    ) {
        if (!isVideoFullscreen) {
            Text(
                currentRequest?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
        }

        VideoSurface(
            currentRequest = currentRequest,
            resolvingEmbed = resolvingEmbed,
            playbackError = playbackError,
            playerManager = playerManager,
            vlcMissing = vlcMissing,
            onRetry = {
                currentRequest?.let {
                    playbackError = null
                    startPlayback(it, playerManager, scope, { resolvingEmbed = it }, { playbackError = it })
                }
            },
            onOpenBrowser = { currentRequest?.url?.let { openInBrowser(it) } },
            onToggleFullscreen = { appWindow.toggleVideoFullscreen() },
            showPrimingOverlay = isPriming && playbackError == null,
            isVideoFullscreen = isVideoFullscreen,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .fillMaxSize()
                .then(
                    if (isVideoFullscreen) Modifier
                    else Modifier.border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                )
        )

        if (!isVideoFullscreen) {
            Spacer(Modifier.height(8.dp))
            ScrollableColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                PlayerControls(
                    currentRequest = currentRequest,
                    currentServerIndex = currentServerIndex,
                    playerManager = playerManager,
                    scope = scope,
                    isVideoFullscreen = isVideoFullscreen,
                    onToggleFullscreen = { appWindow.toggleVideoFullscreen() },
                    onResolvingEmbed = { resolvingEmbed = it },
                    onPlaybackError = { playbackError = it },
                    onNavigate = onNavigate,
                    onServerSwitch = { index, updated ->
                        currentServerIndex = index
                        currentRequest = updated
                        playbackError = null
                    }
                )
            }
        }
    }
}

@Composable
private fun VideoSurface(
    currentRequest: PlaybackRequest?,
    resolvingEmbed: Boolean,
    playbackError: String?,
    playerManager: VlcPlayerManager,
    vlcMissing: Boolean,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit,
    onToggleFullscreen: () -> Unit,
    showPrimingOverlay: Boolean,
    isVideoFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggleFullscreen() })
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            vlcMissing -> VlcMissingMessage()
            currentRequest == null -> Text(Strings.errorStream, color = Color.White)
            else -> {
                SwingPanel(
                    background = Color.Black,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { playerManager.onSurfaceResized() },
                    factory = { playerManager.swingComponent() },
                    update = { playerManager.onSurfaceResized() }
                )
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        if (isVideoFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isVideoFullscreen) Strings.exitFullscreen else Strings.fullscreen,
                        tint = Color.White
                    )
                }
                when {
                    resolvingEmbed -> LoadingState(Strings.preparingStream, subtle = true)
                    playbackError != null -> ErrorState(
                        message = playbackError,
                        onOpenBrowser = onOpenBrowser,
                        onRetry = onRetry
                    )
                    showPrimingOverlay -> LoadingState(Strings.bufferingLive, subtle = true)
                }
            }
        }
    }
}

@Composable
private fun VlcMissingMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text("VLC Media Player Required", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            Strings.vlcRequired,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { openInBrowser("https://www.videolan.org/vlc/") }) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Download VLC")
        }
    }
}

@Composable
private fun PlayerControls(
    currentRequest: PlaybackRequest?,
    currentServerIndex: Int,
    playerManager: VlcPlayerManager,
    scope: kotlinx.coroutines.CoroutineScope,
    isVideoFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onResolvingEmbed: (Boolean) -> Unit,
    onPlaybackError: (String?) -> Unit,
    onNavigate: (String, String) -> Unit,
    onServerSwitch: (Int, PlaybackRequest) -> Unit
) {
    currentRequest?.let { req ->
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onPlaybackError(null)
                        startPlayback(req, playerManager, scope, onResolvingEmbed) { onPlaybackError(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(Strings.retry)
                }
                Button(
                    onClick = { playerManager.stop() },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceHighlight)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = AppColors.TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop", color = AppColors.TextPrimary)
                }
                Button(
                    onClick = onToggleFullscreen,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary.copy(alpha = 0.85f))
                ) {
                    Icon(
                        if (isVideoFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = Strings.fullscreen,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isVideoFullscreen) Strings.exitFullscreen else Strings.fullscreen,
                        color = Color.White
                    )
                }
                Spacer(Modifier.weight(1f))
                NavButton(
                    enabled = !req.prevSlug.isNullOrBlank(),
                    label = Strings.previous,
                    icon = Icons.AutoMirrored.Filled.ArrowBack
                ) { req.prevSlug?.let { onNavigate(it, "Previous") } }
                NavButton(
                    enabled = !req.nextSlug.isNullOrBlank(),
                    label = Strings.next,
                    icon = Icons.AutoMirrored.Filled.ArrowForward
                ) { req.nextSlug?.let { onNavigate(it, "Next") } }
            }

            if (req.servers.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${Strings.servers} (${req.servers.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                ScrollableLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(req.servers) { index, server ->
                        val active = index == currentServerIndex
                        Surface(
                            onClick = {
                                switchServer(req, index) { updated ->
                                    onServerSwitch(index, updated)
                                    startPlayback(updated, playerManager, scope, onResolvingEmbed) { onPlaybackError(it) }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) AppColors.Primary.copy(alpha = 0.15f) else AppColors.SurfaceElevated,
                            modifier = Modifier.border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) AppColors.Primary else AppColors.Border,
                                shape = RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(
                                server.title ?: "Server ${index + 1}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) AppColors.Primary else AppColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(message: String, subtle: Boolean = false) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (subtle) 0.35f else 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.Primary)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White)
        }
    }
}

@Composable
private fun ErrorState(message: String, onOpenBrowser: () -> Unit, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(message, color = Color.White, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) { Text(Strings.retry) }
            Button(onClick = onOpenBrowser) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(Strings.openInBrowser)
            }
        }
    }
}

@Composable
private fun NavButton(
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
        Icon(icon, contentDescription = label, tint = if (enabled) AppColors.Primary else AppColors.TextSecondary)
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary)
    }
}

private fun startPlayback(
    request: PlaybackRequest,
    playerManager: VlcPlayerManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onResolvingEmbed: (Boolean) -> Unit,
    onError: (String?) -> Unit
) {
    val resolved = request.copy(url = EmbedUrlResolver.resolve(request.url.orEmpty()))
    val needsEmbed = resolved.streamType == StreamType.EMBED ||
        EmbedUrlResolver.isWebEmbed(resolved.url.orEmpty()) ||
        !StreamLinkParser.isDirectStream(resolved.url.orEmpty())

    if (needsEmbed) {
        onResolvingEmbed(true)
        scope.launch {
            val streamUrl = EmbedStreamResolver.resolveStreamUrl(resolved.url.orEmpty(), resolved.headers)
            onResolvingEmbed(false)
            if (streamUrl != null) {
                playerManager.play(playbackFromExtractedStream(resolved, streamUrl))
            } else {
                runCatching { playerManager.play(resolved) }
                    .onFailure { onError("Failed to resolve embed stream") }
            }
        }
    } else {
        onResolvingEmbed(false)
        runCatching { playerManager.play(resolved) }
            .onFailure { onError("Failed to start playback") }
    }
}

private fun switchServer(request: PlaybackRequest, index: Int, onUpdated: (PlaybackRequest) -> Unit) {
    val server = request.servers[index]
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
    onUpdated(request.copy(
        url = url,
        headers = headers,
        streamType = streamType,
        isLiveBroadcast = streamType != StreamType.MP4 || request.isLiveBroadcast
    ))
}

private fun playbackFromExtractedStream(request: PlaybackRequest, streamUrl: String): PlaybackRequest {
    val headers = request.headers.toMutableMap()
    headers.putAll(SabcChannels.headersForUrl(streamUrl))
    headers.putAll(RegionalStreamResolver.headersForUrl(streamUrl))
    headers.putAll(NewsChannels.headersForUrl(streamUrl))
    val streamType = EmbedStreamResolver.streamTypeForUrl(streamUrl)
    return request.copy(
        url = streamUrl,
        streamType = streamType,
        headers = headers,
        isLiveBroadcast = streamType == StreamType.HLS || streamType == StreamType.DASH
    )
}

private fun openInBrowser(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    }
}
