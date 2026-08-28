package com.charlztech.tv.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Serializable
data class FavoriteEntity(
    val id: String,
    val title: String,
    val type: String,
    val slug: String? = null,
    val url: String? = null,
    val posterUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Serializable
private data class FavoritesFile(val items: List<FavoriteEntity> = emptyList())

class FavoritesStore(private val storagePath: Path) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun getAll(): List<FavoriteEntity> = withContext(Dispatchers.IO) {
        mutex.withLock { readFile().items.sortedByDescending { it.addedAt } }
    }

    suspend fun insert(favorite: FavoriteEntity) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readFile().items.toMutableList()
            current.removeAll { it.id == favorite.id }
            current.add(favorite)
            writeFile(FavoritesFile(current))
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = readFile().items.filterNot { it.id == id }
            writeFile(FavoritesFile(current))
        }
    }

    suspend fun exists(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock { readFile().items.any { it.id == id } }
    }

    private fun readFile(): FavoritesFile {
        if (!Files.exists(storagePath)) return FavoritesFile()
        return try {
            val text = Files.readString(storagePath)
            if (text.isBlank()) FavoritesFile() else json.decodeFromString(text)
        } catch (_: Exception) {
            FavoritesFile()
        }
    }

    private fun writeFile(data: FavoritesFile) {
        Files.createDirectories(storagePath.parent)
        Files.writeString(
            storagePath,
            json.encodeToString(data),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }
}
