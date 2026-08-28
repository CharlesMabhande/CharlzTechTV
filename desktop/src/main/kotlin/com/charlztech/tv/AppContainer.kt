package com.charlztech.tv

import com.charlztech.tv.data.local.FavoritesStore
import com.charlztech.tv.data.remote.StreamApiService
import com.charlztech.tv.data.repository.StreamRepository
import com.charlztech.tv.player.VlcPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path

class AppContainer(appDataDir: Path) {
    private val favoritesStore = FavoritesStore(appDataDir.resolve("favorites.json"))
    val api = StreamApiService()
    val repository = StreamRepository(api, favoritesStore)
    /**
     * Created after [com.charlztech.tv.player.VlcBootstrap.setup]. Failure here used to
     * abort the process as "Failed to launch JVM" on PCs without system VLC.
     */
    val playerManager: VlcPlayerManager = runCatching { VlcPlayerManager.create() }
        .getOrElse { error ->
            throw IllegalStateException(
                "Unable to initialize the bundled video engine. Reinstall CharlzTechTV.",
                error
            )
        }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun shutdown() {
        playerManager.release()
    }

    fun start() {
        scope.launch { repository.warmUp() }
        scope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                runCatching { repository.refreshAll(force = true) }
            }
        }
        scope.launch {
            while (isActive) {
                delay(3 * 60 * 1000L)
                runCatching { repository.refreshAll() }
            }
        }
    }

    companion object {
        fun defaultDataDir(): Path {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?: System.getProperty("user.home")
            val dir = Path.of(localAppData, "CharlzTechTV")
            Files.createDirectories(dir)
            return dir
        }
    }
}
