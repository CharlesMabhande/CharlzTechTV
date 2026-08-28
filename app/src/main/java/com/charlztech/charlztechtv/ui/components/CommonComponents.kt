@file:OptIn(ExperimentalMaterial3Api::class)

package com.charlztech.charlztechtv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.charlztech.charlztechtv.data.model.EventStatus
import com.charlztech.charlztechtv.data.model.LiveEventUi
import com.charlztech.charlztechtv.data.model.M3uChannel
import com.charlztech.charlztechtv.data.model.Provider
import com.charlztech.charlztechtv.ui.theme.AppColors
import com.charlztech.charlztechtv.ui.util.Responsive
import com.charlztech.charlztechtv.ui.util.SportFilter
import com.charlztech.charlztechtv.ui.util.isSportFilterSelected
import com.charlztech.charlztechtv.ui.util.sportCategoryIcon

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            AppColors.SurfaceElevated,
            AppColors.SurfaceHighlight,
            AppColors.SurfaceElevated
        ),
        start = Offset(offset, 0f),
        end = Offset(offset + 300f, 300f)
    )
    Box(modifier = modifier.background(brush))
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Loading streams…",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
fun LoadingShimmerList(modifier: Modifier = Modifier, count: Int = 8) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Responsive.contentPadding()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(count) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.SearchOff,
    subtitle: String? = "Pull down to refresh",
    backLabel: String? = null,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = AppColors.SurfaceElevated,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        if (onBack != null && !backLabel.isNullOrBlank()) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(backLabel)
            }
        }
    }
}

@Composable
fun StatusBadge(status: EventStatus, modifier: Modifier = Modifier) {
    val (text, color, showDot) = when (status) {
        EventStatus.LIVE -> Triple("LIVE", AppColors.Live, true)
        EventStatus.UPCOMING -> Triple("UP NEXT", AppColors.Upcoming, false)
        EventStatus.ENDED -> Triple("ENDED", AppColors.Ended, false)
        EventStatus.UNKNOWN -> Triple("", Color.Transparent, false)
    }
    if (text.isBlank()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LiveEventListItem(
    event: LiveEventUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (!event.posterUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = event.posterUrl,
                        contentDescription = event.displayTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { ShimmerBox(Modifier.fillMaxSize()) },
                        error = { EventThumbFallback(event.category) }
                    )
                } else {
                    EventThumbFallback(event.category)
                }
                StatusBadge(
                    status = event.status,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    event.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                if (!event.scheduleLabel.isNullOrBlank()) {
                    EventScheduleRow(
                        label = event.scheduleLabel,
                        detail = event.scheduleDetail,
                        emphasize = event.status == EventStatus.UPCOMING
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.Secondary
                    )
                    if (event.serverCount > 0) {
                        Surface(shape = RoundedCornerShape(6.dp), color = AppColors.Primary.copy(alpha = 0.12f)) {
                            Text(
                                "${event.serverCount} sources",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Surface(shape = CircleShape, color = AppColors.Primary) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun LiveEventCard(
    event: LiveEventUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardWidth = Responsive.eventCardWidth()
    Card(
        onClick = onClick,
        modifier = modifier.width(cardWidth),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceElevated),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            Box {
                SubcomposeAsyncImage(
                    model = event.posterUrl,
                    contentDescription = event.displayTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                    loading = {
                        ShimmerBox(Modifier.fillMaxSize())
                    },
                    error = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(AppColors.SurfaceHighlight, AppColors.Surface)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                sportCategoryIcon(event.category),
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, AppColors.CardOverlay)
                            )
                        )
                )
                StatusBadge(
                    status = event.status,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    shape = CircleShape,
                    color = AppColors.Primary
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(
                    event.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                if (!event.scheduleLabel.isNullOrBlank()) {
                    EventScheduleRow(
                        label = event.scheduleLabel,
                        detail = event.scheduleDetail,
                        emphasize = event.status == EventStatus.UPCOMING
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    event.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Secondary
                )
            }
        }
    }
}

@Composable
fun ProviderCard(provider: Provider, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = provider.image,
                contentDescription = provider.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerBox(Modifier.fillMaxSize())
                },
                error = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(AppColors.SurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = AppColors.Primary)
                    }
                }
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    provider.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Tap to browse channels",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppColors.Primary)
        }
    }
}

@Composable
fun ChannelRow(channel: M3uChannel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceHighlight,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!channel.logo.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.LiveTv, contentDescription = null, tint = AppColors.Primary)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                channel.group?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                }
            }
            Surface(shape = CircleShape, color = AppColors.Primary.copy(alpha = 0.15f)) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SportFilterBar(
    categories: List<String>,
    selectedFilter: SportFilter,
    onFilterSelected: (SportFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item(key = "filter-live") {
            SportFilterChip(
                label = "Live",
                icon = Icons.Default.LiveTv,
                selected = isSportFilterSelected(selectedFilter, SportFilter.AllLive),
                accent = AppColors.Live,
                onClick = { onFilterSelected(SportFilter.AllLive) }
            )
        }
        item(key = "filter-all") {
            SportFilterChip(
                label = "All",
                icon = Icons.Default.Apps,
                selected = isSportFilterSelected(selectedFilter, SportFilter.All),
                accent = AppColors.Primary,
                onClick = { onFilterSelected(SportFilter.All) }
            )
        }
        items(categories, key = { "filter-$it" }) { category ->
            SportFilterChip(
                label = category,
                icon = sportCategoryIcon(category),
                selected = isSportFilterSelected(selectedFilter, SportFilter.Sport(category)),
                accent = AppColors.Primary,
                onClick = { onFilterSelected(SportFilter.Sport(category)) }
            )
        }
    }
}

@Composable
private fun SportFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val background = if (selected) accent.copy(alpha = 0.22f) else AppColors.SurfaceElevated
    val border = if (selected) accent else AppColors.SurfaceHighlight
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) accent else AppColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun CategoryHeader(category: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            sportCategoryIcon(category),
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Text(category, style = MaterialTheme.typography.headlineMedium, color = AppColors.TextPrimary)
    }
}

@Composable
private fun EventScheduleRow(
    label: String,
    detail: String?,
    emphasize: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = if (emphasize) AppColors.Upcoming else AppColors.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (emphasize) AppColors.Upcoming else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!detail.isNullOrBlank()) {
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = if (emphasize) AppColors.Upcoming.copy(alpha = 0.85f) else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EventThumbFallback(category: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceHighlight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            sportCategoryIcon(category),
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun LiveEventsSection(
    grouped: Map<String, List<LiveEventUi>>,
    onEventClick: (LiveEventUi) -> Unit,
    contentPadding: PaddingValues = PaddingValues(Responsive.contentPadding()),
    headerContent: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (headerContent != null) {
            item(key = "header-banner") { headerContent() }
            item(key = "header-spacer") { Spacer(Modifier.height(8.dp)) }
        }
        grouped.forEach { (category, events) ->
            item(key = "header-$category") {
                CategoryHeader(category)
                Spacer(Modifier.height(6.dp))
            }
            items(events, key = { "${category}-${it.event.id}" }) { event ->
                LiveEventListItem(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
            item(key = "spacer-$category") { Spacer(Modifier.height(10.dp)) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun ProviderList(
    providers: List<Provider>,
    onProviderClick: (Provider) -> Unit,
    contentPadding: PaddingValues = PaddingValues(Responsive.contentPadding())
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(providers, key = { it.id }) { provider ->
            ProviderCard(provider = provider, onClick = { onProviderClick(provider) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
