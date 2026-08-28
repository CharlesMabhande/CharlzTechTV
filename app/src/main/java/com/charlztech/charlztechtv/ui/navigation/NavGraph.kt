package com.charlztech.charlztechtv.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.charlztech.charlztechtv.CharlzTechTvApp
import com.charlztech.charlztechtv.R
import com.charlztech.charlztechtv.data.model.LiveEventUi
import com.charlztech.charlztechtv.data.model.PlaybackRequest
import com.charlztech.charlztechtv.data.model.Provider
import com.charlztech.charlztechtv.player.PlaybackSession
import com.charlztech.charlztechtv.player.PlayerActivity
import com.charlztech.charlztechtv.ui.screens.AboutScreen
import com.charlztech.charlztechtv.ui.screens.ChannelsScreen
import com.charlztech.charlztechtv.ui.screens.FavoritesScreen
import com.charlztech.charlztechtv.ui.screens.HomeScreen
import com.charlztech.charlztechtv.ui.screens.ProvidersScreen
import com.charlztech.charlztechtv.ui.screens.SearchScreen
import com.charlztech.charlztechtv.ui.theme.AppColors
import com.charlztech.charlztechtv.ui.viewmodel.ChannelsViewModel
import com.charlztech.charlztechtv.ui.viewmodel.ChannelsViewModelFactory
import com.charlztech.charlztechtv.ui.viewmodel.FavoritesViewModel
import com.charlztech.charlztechtv.ui.viewmodel.HomeViewModel
import com.charlztech.charlztechtv.ui.viewmodel.ProvidersViewModel
import com.charlztech.charlztechtv.ui.viewmodel.SearchViewModel
import com.charlztech.charlztechtv.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Providers : Route("providers")
    data object Search : Route("search")
    data object Favorites : Route("favorites")
    data object About : Route("about")
    data object Channels : Route("channels/{providerJson}") {
        fun create(provider: Provider): String {
            val json = Json.encodeToString(provider)
            val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
            return "channels/$encoded"
        }
    }
}

data class BottomNavItem(
    val route: Route,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun CharlzTechNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as CharlzTechTvApp
    val factory = ViewModelFactory(app.repository)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val items = listOf(
        BottomNavItem(Route.Home, R.string.nav_home, Icons.Default.Home),
        BottomNavItem(Route.Providers, R.string.nav_providers, Icons.Default.Tv),
        BottomNavItem(Route.Search, R.string.nav_search, Icons.Default.Search),
        BottomNavItem(Route.Favorites, R.string.nav_favorites, Icons.Default.Favorite)
    )

    fun play(request: PlaybackRequest) {
        PlaybackSession.set(request)
        context.startActivity(PlayerActivity.intent(context, request.slug, request.title))
    }

    fun playEvent(homeViewModel: HomeViewModel, event: LiveEventUi) {
        scope.launch {
            val playback = homeViewModel.buildPlayback(event)
            if (playback != null) {
                play(playback)
            }
        }
    }

    fun playChannel(channel: com.charlztech.charlztechtv.data.model.M3uChannel, buildPlayback: suspend (com.charlztech.charlztechtv.data.model.M3uChannel) -> com.charlztech.charlztechtv.data.model.PlaybackRequest) {
        scope.launch {
            play(buildPlayback(channel))
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute?.startsWith("channels") != true &&
        currentRoute != Route.About.path

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = AppColors.Surface,
                    contentColor = AppColors.TextPrimary
                ) {
                    items.forEach { item ->
                        val selected = currentRoute == item.route.path
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route.path) {
                                    popUpTo(Route.Home.path) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = null,
                                    tint = if (selected) AppColors.Primary else AppColors.TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    stringResource(item.labelRes),
                                    color = if (selected) AppColors.Primary else AppColors.TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = AppColors.Primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(Route.Home.path) {
                val vm: HomeViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    onEventClick = { playEvent(vm, it) },
                    onAboutClick = { navController.navigate(Route.About.path) }
                )
            }
            composable(Route.About.path) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.Providers.path) {
                val vm: ProvidersViewModel = viewModel(factory = factory)
                val providers by vm.providers.collectAsState()
                val loading by vm.loading.collectAsState()
                ProvidersScreen(
                    providers = providers,
                    loading = loading,
                    onProviderClick = { navController.navigate(Route.Channels.create(it)) },
                    onRefresh = vm::refresh
                )
            }
            composable(
                route = Route.Channels.path,
                arguments = listOf(navArgument("providerJson") { type = NavType.StringType })
            ) { entry ->
                val encoded = entry.arguments?.getString("providerJson").orEmpty()
                val provider = Json.decodeFromString<Provider>(
                    java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                )
                val vm: ChannelsViewModel = viewModel(
                    factory = ChannelsViewModelFactory(app.repository, provider)
                )
                val channels by vm.channels.collectAsState()
                val loading by vm.loading.collectAsState()
                val query by vm.query.collectAsState()
                ChannelsScreen(
                    title = provider.title,
                    channels = vm.filteredChannels(),
                    loading = loading,
                    query = query,
                    onQueryChange = vm::setQuery,
                    onChannelClick = { channel -> playChannel(channel) { vm.buildPlayback(it) } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.Search.path) {
                val vm: SearchViewModel = viewModel(factory = factory)
                val query by vm.query.collectAsState()
                val results by vm.results.collectAsState()
                val homeVm: HomeViewModel = viewModel(factory = factory)
                SearchScreen(
                    query = query,
                    results = results,
                    onQueryChange = vm::search,
                    onEventClick = { playEvent(homeVm, it) }
                )
            }
            composable(Route.Favorites.path) {
                val vm: FavoritesViewModel = viewModel(factory = factory)
                val favorites by vm.favorites.collectAsState()
                FavoritesScreen(
                    favorites = favorites,
                    onFavoriteClick = { fav ->
                        if (!fav.slug.isNullOrBlank()) {
                            scope.launch {
                                app.repository.buildPlaybackForSlug(fav.slug, fav.title)?.let { play(it) }
                            }
                        } else if (!fav.url.isNullOrBlank()) {
                            play(
                                PlaybackRequest(
                                    title = fav.title,
                                    url = fav.url,
                                    slug = fav.slug
                                )
                            )
                        }
                    }
                )
            }
        }
    }
    }
}
