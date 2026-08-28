package com.charlztech.tv.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.charlztech.tv.LocalAppContainer
import com.charlztech.tv.LocalAppWindowState
import com.charlztech.tv.data.model.PlaybackRequest
import com.charlztech.tv.data.model.Provider
import com.charlztech.tv.player.PlayerScreen
import com.charlztech.tv.resources.Strings
import com.charlztech.tv.ui.screens.AboutScreen
import com.charlztech.tv.ui.screens.ChannelsScreen
import com.charlztech.tv.ui.screens.FavoritesScreen
import com.charlztech.tv.ui.screens.HomeScreen
import com.charlztech.tv.ui.screens.ProvidersScreen
import com.charlztech.tv.ui.screens.SearchScreen
import com.charlztech.tv.ui.shell.BrowserShell
import com.charlztech.tv.ui.shell.BrowserTab
import com.charlztech.tv.ui.util.ResponsiveWindowProvider
import com.charlztech.tv.ui.viewmodel.ChannelsViewModel
import com.charlztech.tv.ui.viewmodel.ChannelsViewModelFactory
import com.charlztech.tv.ui.viewmodel.FavoritesViewModel
import com.charlztech.tv.ui.viewmodel.HomeViewModel
import com.charlztech.tv.ui.viewmodel.ProvidersViewModel
import com.charlztech.tv.ui.viewmodel.SearchViewModel
import com.charlztech.tv.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Providers : Route("providers")
    data object Search : Route("search")
    data object Favorites : Route("favorites")
    data object About : Route("about")
    data object Player : Route("player/{requestJson}") {
        fun create(request: PlaybackRequest): String {
            val json = Json.encodeToString(request)
            val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8)
            return "player/$encoded"
        }
    }
    data object Channels : Route("channels/{providerJson}") {
        fun create(provider: Provider): String {
            val json = Json.encodeToString(provider)
            val encoded = URLEncoder.encode(json, StandardCharsets.UTF_8)
            return "channels/$encoded"
        }
    }
}

@Composable
fun CharlzTechNavHost() {
    val container = LocalAppContainer.current
    val appWindow = LocalAppWindowState.current
    val factory = ViewModelFactory(container.repository)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var addressBarText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
  var refreshKey by remember { mutableStateOf(0) }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val selectedTab = when {
        currentRoute == Route.Providers.path || currentRoute?.startsWith("channels") == true -> BrowserTab.Providers
        currentRoute == Route.Search.path -> BrowserTab.Search
        currentRoute == Route.Favorites.path -> BrowserTab.Favorites
        else -> BrowserTab.Home
    }

    val pageTitle = when {
        currentRoute?.startsWith("player") == true -> "Now Playing"
        currentRoute?.startsWith("channels") == true -> "Channels"
        currentRoute == Route.Providers.path -> Strings.allProviders
        currentRoute == Route.Search.path -> Strings.navSearch
        currentRoute == Route.Favorites.path -> Strings.navFavorites
        currentRoute == Route.About.path -> Strings.aboutTitle
        else -> Strings.navHome
    }

    DisposableEffect(Unit) {
        onDispose { container.shutdown() }
    }

    fun play(request: PlaybackRequest) {
        navController.navigate(Route.Player.create(request))
    }

    val isOnPlayer = currentRoute?.startsWith("player") == true

    ResponsiveWindowProvider {
        BrowserShell(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                appWindow.exitVideoFullscreen()
                val route = when (tab) {
                    BrowserTab.Home -> Route.Home.path
                    BrowserTab.Providers -> Route.Providers.path
                    BrowserTab.Search -> Route.Search.path
                    BrowserTab.Favorites -> Route.Favorites.path
                }
                navController.navigate(route) {
                    popUpTo(Route.Home.path) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            addressBarText = if (addressBarText.isBlank()) pageTitle else addressBarText,
            onAddressBarChange = { addressBarText = it },
            onAddressBarSubmit = {
                navController.navigate(Route.Search.path)
            },
            canGoBack = navController.previousBackStackEntry != null,
            canGoForward = false,
            onBack = {
                appWindow.exitVideoFullscreen()
                navController.popBackStack()
            },
            onForward = { },
            onRefresh = { refreshKey++ },
            onAbout = { navController.navigate(Route.About.path) },
            onToggleWindowFullscreen = { appWindow.toggleWindowFullscreen() },
            isWindowFullscreen = appWindow.isWindowFullscreen,
            isSidebarCollapsed = appWindow.isSidebarCollapsed,
            onToggleSidebar = { appWindow.isSidebarCollapsed = !appWindow.isSidebarCollapsed },
            statusText = statusText,
            isVideoFullscreen = appWindow.isVideoFullscreen && isOnPlayer,
            onToggleVideoFullscreen = if (isOnPlayer) ({ appWindow.toggleVideoFullscreen() }) else null,
            onExitVideoFullscreen = { appWindow.exitVideoFullscreen() }
        ) {
            NavHost(
                navController = navController,
                startDestination = Route.Home.path,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Route.Home.path) {
                    val vm: HomeViewModel = viewModel(factory = factory)
                    DisposableEffect(refreshKey) {
                        if (refreshKey > 0) vm.refresh()
                        onDispose { }
                    }
                    HomeScreen(
                        viewModel = vm,
                        onEventClick = { event ->
                            scope.launch { vm.buildPlayback(event)?.let { play(it) } }
                        }
                    )
                }
                composable(Route.About.path) {
                    AboutScreen()
                }
                composable(Route.Providers.path) {
                    val vm: ProvidersViewModel = viewModel(factory = factory)
                    val providers by vm.providers.collectAsState()
                    val loading by vm.loading.collectAsState()
                    DisposableEffect(refreshKey) {
                        if (refreshKey > 0) vm.refresh()
                        onDispose { }
                    }
                    ProvidersScreen(
                        providers = providers,
                        loading = loading,
                        onProviderClick = { navController.navigate(Route.Channels.create(it)) }
                    )
                }
                composable(
                    route = Route.Channels.path,
                    arguments = listOf(navArgument("providerJson") { type = NavType.StringType })
                ) { entry ->
                    val encoded = entry.arguments?.getString("providerJson").orEmpty()
                    val provider = Json.decodeFromString<Provider>(
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8)
                    )
                    val vm: ChannelsViewModel = viewModel(
                        factory = ChannelsViewModelFactory(container.repository, provider)
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
                        onChannelClick = { channel ->
                            scope.launch { play(vm.buildPlayback(channel)) }
                        }
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
                        onQueryChange = {
                            addressBarText = it
                            vm.search(it)
                        },
                        onEventClick = { event ->
                            scope.launch { homeVm.buildPlayback(event)?.let { play(it) } }
                        }
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
                                    container.repository.buildPlaybackForSlug(fav.slug, fav.title)?.let { play(it) }
                                }
                            } else if (!fav.url.isNullOrBlank()) {
                                play(PlaybackRequest(title = fav.title, url = fav.url, slug = fav.slug))
                            }
                        }
                    )
                }
                composable(
                    route = Route.Player.path,
                    arguments = listOf(navArgument("requestJson") { type = NavType.StringType })
                ) { entry ->
                    val encoded = entry.arguments?.getString("requestJson").orEmpty()
                    val request = Json.decodeFromString<PlaybackRequest>(
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8)
                    )
                    PlayerScreen(
                        request = request,
                        playerManager = container.playerManager,
                        isVideoFullscreen = appWindow.isVideoFullscreen,
                        onNavigate = { slug, title ->
                            scope.launch {
                                container.repository.buildPlaybackForSlug(slug, title)?.let { playback ->
                                    navController.navigate(Route.Player.create(playback)) {
                                        popUpTo(Route.Player.path) { inclusive = true }
                                    }
                                }
                            }
                        },
                        onStatusUpdate = { statusText = it }
                    )
                }
            }
        }
    }
}
