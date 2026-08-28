package com.charlztech.tv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.charlztech.tv.resources.Strings
import com.charlztech.tv.data.model.LiveEventUi
import com.charlztech.tv.ui.components.EmptyState
import com.charlztech.tv.ui.components.LiveEventsSection
import com.charlztech.tv.ui.components.LoadingBox
import com.charlztech.tv.ui.components.LoadingShimmerList
import com.charlztech.tv.ui.components.ProviderList
import androidx.compose.runtime.remember
import com.charlztech.tv.ui.components.SportFilterBar
import com.charlztech.tv.ui.util.applySportFilter
import com.charlztech.tv.ui.util.orderedSportCategories
import com.charlztech.tv.ui.components.StatChip
import com.charlztech.tv.ui.theme.AppColors
import com.charlztech.tv.ui.util.Responsive
import com.charlztech.tv.ui.util.SportFilter
import com.charlztech.tv.ui.util.displayLabel
import com.charlztech.tv.ui.util.ScrollableLazyColumn
import com.charlztech.tv.ui.util.ScrollableColumn
import com.charlztech.tv.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onEventClick: (LiveEventUi) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val padding = Responsive.contentPadding()
    val categories = remember(state.groupedEvents) { orderedSportCategories(state.groupedEvents) }
    val filteredEvents = remember(state.groupedEvents, state.selectedFilter) {
        applySportFilter(state.groupedEvents, state.selectedFilter)
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        when {
            state.isLoading && state.groupedEvents.isEmpty() -> LoadingBox(Modifier.weight(1f))
            state.groupedEvents.isEmpty() -> EmptyState(
                Strings.noEvents,
                icon = Icons.Outlined.Event,
                modifier = Modifier.weight(1f)
            )
            filteredEvents.isEmpty() -> FilterEmptyContent(
                categories = categories,
                selectedFilter = state.selectedFilter,
                onFilterSelected = viewModel::selectFilter,
                onBack = { viewModel.selectFilter(SportFilter.All) }
            )
            else -> LiveEventsSection(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                grouped = filteredEvents,
                onEventClick = onEventClick,
                contentPadding = PaddingValues(vertical = 12.dp),
                headerContent = {
                        SportFilterBar(
                            categories = categories,
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = viewModel::selectFilter
                        )
                        Spacer(Modifier.height(12.dp))
                        HeroStatsBanner(
                            liveCount = state.liveCount,
                            upcomingCount = state.upcomingCount
                        )
                    }
                )
        }
    }
}

@Composable
private fun FilterEmptyContent(
    categories: List<String>,
    selectedFilter: SportFilter,
    onFilterSelected: (SportFilter) -> Unit,
    onBack: () -> Unit
) {
    val padding = Responsive.contentPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = padding, vertical = 12.dp)
    ) {
        SportFilterBar(
            categories = categories,
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )
        EmptyState(
            message = String.format(Strings.noEventsFilterNamed, selectedFilter.displayLabel()),
            icon = Icons.Outlined.Event,
            subtitle = Strings.autoRefresh,
            backLabel = Strings.backToAllEvents,
            onBack = onBack,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeroStatsBanner(
    liveCount: Int,
    upcomingCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.SurfaceElevated
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppColors.GradientStart.copy(alpha = 0.18f),
                            AppColors.GradientEnd.copy(alpha = 0.10f)
                        )
                    )
                )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Live Sports Hub", style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    Strings.autoRefresh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(
                        label = Strings.liveNow,
                        value = liveCount.toString(),
                        accent = AppColors.Live,
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        label = Strings.upcoming,
                        value = upcomingCount.toString(),
                        accent = AppColors.Upcoming,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProvidersScreen(
    providers: List<com.charlztech.tv.data.model.Provider>,
    loading: Boolean,
    onProviderClick: (com.charlztech.tv.data.model.Provider) -> Unit,
    onRefresh: () -> Unit = {}
) {
    val padding = Responsive.contentPadding()
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading && providers.isEmpty() -> LoadingShimmerList()
            providers.isEmpty() -> EmptyState(Strings.noChannels)
            else -> Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = padding, vertical = 16.dp)) {
                    Text(Strings.allProviders, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${providers.size} streaming sources",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
                ProviderList(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    providers = providers,
                    onProviderClick = onProviderClick,
                    contentPadding = PaddingValues(horizontal = padding, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    title: String,
    channels: List<com.charlztech.tv.data.model.M3uChannel>,
    loading: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onChannelClick: (com.charlztech.tv.data.model.M3uChannel) -> Unit
) {
    val padding = Responsive.contentPadding()
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.padding(bottom = 8.dp)) {
            Text(title, maxLines = 1, style = MaterialTheme.typography.headlineSmall)
            if (!loading) {
                Text(
                    "${channels.size} channels",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        when {
            loading -> LoadingBox(Modifier.weight(1f))
            channels.isEmpty() -> EmptyState(Strings.noChannels, Modifier.weight(1f))
            else -> ScrollableLazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels, key = { it.name + it.url }) { channel ->
                    com.charlztech.tv.ui.components.ChannelRow(
                        channel = channel,
                        onClick = { onChannelClick(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    query: String,
    results: List<LiveEventUi>,
    onQueryChange: (String) -> Unit,
    onEventClick: (LiveEventUi) -> Unit
) {
    val padding = Responsive.contentPadding()
    Column(Modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(horizontal = padding, vertical = 16.dp)
        )
        when {
            results.isEmpty() && query.isNotBlank() -> EmptyState(Strings.noEvents, Modifier.weight(1f))
            query.isBlank() -> EmptyState("Search teams, sports, or events", icon = Icons.Default.Search, modifier = Modifier.weight(1f))
            else -> LiveEventsSection(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                grouped = results.groupBy { it.category },
                onEventClick = onEventClick,
                contentPadding = PaddingValues(horizontal = padding)
            )
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<com.charlztech.tv.data.local.FavoriteEntity>,
    onFavoriteClick: (com.charlztech.tv.data.local.FavoriteEntity) -> Unit
) {
    val padding = Responsive.contentPadding()
    if (favorites.isEmpty()) {
        EmptyState("No favorites yet", modifier = Modifier.fillMaxSize())
    } else {
        ScrollableLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Your Favorites", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(favorites, key = { it.id }) { fav ->
                com.charlztech.tv.ui.components.ChannelRow(
                    channel = com.charlztech.tv.data.model.M3uChannel(
                        name = fav.title,
                        url = fav.url.orEmpty(),
                        logo = fav.posterUrl
                    ),
                    onClick = { onFavoriteClick(fav) }
                )
            }
        }
    }
}

@Composable
fun StreamLoadingOverlay(message: String = "Preparing stream…") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = AppColors.TextPrimary)
        }
    }
}

@Composable
fun AboutScreen() {
    val padding = Responsive.contentPadding()
    val phone = Strings.aboutPhoneValue
    val email = Strings.aboutEmailValue

    ScrollableColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = padding, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource("images/charlztech_logo.png"),
                    contentDescription = Strings.aboutDeveloperLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "CharlzTechTV",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.aboutTagline,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Spacer(Modifier.height(32.dp))

            AboutDetailCard(
                label = Strings.aboutDeveloperLabel,
                value = Strings.aboutDeveloperValue,
                icon = Icons.Default.Info
            )
            Spacer(Modifier.height(12.dp))
            AboutDetailCard(
                label = Strings.aboutPhoneLabel,
                value = phone,
                icon = Icons.Default.Phone,
                onClick = {
                    openDesktopUri("tel:$phone")
                }
            )
            Spacer(Modifier.height(12.dp))
            AboutDetailCard(
                label = Strings.aboutEmailLabel,
                value = email,
                icon = Icons.Default.Email,
                onClick = {
                    openDesktopUri("mailto:$email")
                }
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} CharlzTech Software Developers",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun AboutDetailCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.SurfaceElevated
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.Primary.copy(alpha = 0.15f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.padding(10.dp).size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge, color = AppColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(Strings.searchHint, color = AppColors.TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.Primary) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = AppColors.SurfaceHighlight,
            focusedContainerColor = AppColors.SurfaceElevated,
            unfocusedContainerColor = AppColors.SurfaceElevated,
            cursorColor = AppColors.Primary
        )
    )
}

private fun openDesktopUri(uri: String) {
    runCatching {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(uri))
        }
    }
}
