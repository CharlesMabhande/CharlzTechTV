package com.charlztech.tv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.charlztech.tv.data.local.FavoriteEntity
import com.charlztech.tv.data.model.LiveEventUi
import com.charlztech.tv.data.model.M3uChannel
import com.charlztech.tv.data.model.PlaybackRequest
import com.charlztech.tv.data.model.Provider
import com.charlztech.tv.data.repository.StreamRepository
import com.charlztech.tv.ui.util.SportFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val groupedEvents: Map<String, List<LiveEventUi>> = emptyMap(),
    val liveCount: Int = 0,
    val upcomingCount: Int = 0,
    val selectedFilter: SportFilter = SportFilter.All
)

class HomeViewModel(private val repository: StreamRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.liveEvents.collect { events ->
                _state.value = _state.value.copy(
                    groupedEvents = repository.groupEventsByCategory(events),
                    liveCount = events.count { it.status == com.charlztech.tv.data.model.EventStatus.LIVE },
                    upcomingCount = events.count { it.status == com.charlztech.tv.data.model.EventStatus.UPCOMING }
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.refreshAll(force = true)
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectFilter(filter: SportFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    suspend fun buildPlayback(event: LiveEventUi): PlaybackRequest? =
        repository.buildPlaybackForEvent(event.event)
}

class ProvidersViewModel(private val repository: StreamRepository) : ViewModel() {
    val providers: StateFlow<List<Provider>> = repository.providers

    private val _loading = MutableStateFlow(repository.providers.value.isEmpty())
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        loadProviders()
    }

    fun refresh() {
        loadProviders(force = true)
    }

    private fun loadProviders(force: Boolean = false) {
        viewModelScope.launch {
            if (providers.value.isEmpty()) {
                _loading.value = true
            }
            try {
                repository.refreshAll(force = force || providers.value.isEmpty())
            } catch (_: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }
}

class ChannelsViewModel(
    private val repository: StreamRepository,
    private val provider: Provider
) : ViewModel() {
    private val _channels = MutableStateFlow<List<M3uChannel>>(emptyList())
    val channels: StateFlow<List<M3uChannel>> = _channels.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _channels.value = repository.getChannels(provider)
            _loading.value = false
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun filteredChannels(): List<M3uChannel> {
        val q = _query.value
        return if (q.isBlank()) _channels.value
        else _channels.value.filter { it.name.contains(q, ignoreCase = true) || it.group?.contains(q, ignoreCase = true) == true }
    }

    suspend fun buildPlayback(channel: M3uChannel): PlaybackRequest =
        repository.buildPlaybackForChannel(channel)
}

class SearchViewModel(private val repository: StreamRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _results = MutableStateFlow<List<LiveEventUi>>(emptyList())
    val results: StateFlow<List<LiveEventUi>> = _results.asStateFlow()

    fun search(text: String) {
        _query.value = text
        _results.value = repository.searchEvents(text)
    }
}

class FavoritesViewModel(private val repository: StreamRepository) : ViewModel() {
    private val _favorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())
    val favorites: StateFlow<List<FavoriteEntity>> = _favorites.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _favorites.value = repository.getFavorites()
        }
    }
}

class ViewModelFactory(private val repository: StreamRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return when {
            modelClass.java.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository) as T
            modelClass.java.isAssignableFrom(ProvidersViewModel::class.java) -> ProvidersViewModel(repository) as T
            modelClass.java.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(repository) as T
            modelClass.java.isAssignableFrom(FavoritesViewModel::class.java) -> FavoritesViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.simpleName}")
        }
    }
}

class ChannelsViewModelFactory(
    private val repository: StreamRepository,
    private val provider: Provider
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        if (modelClass.java.isAssignableFrom(ChannelsViewModel::class.java)) {
            return ChannelsViewModel(repository, provider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.simpleName}")
    }
}
