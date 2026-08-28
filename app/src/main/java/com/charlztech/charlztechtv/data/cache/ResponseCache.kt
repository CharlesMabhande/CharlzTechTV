package com.charlztech.charlztechtv.data.cache

import java.util.concurrent.ConcurrentHashMap

data class CacheEntry<T>(val data: T, val timestampMs: Long)

class ResponseCache(private val ttlMs: Long) {
    private val store = ConcurrentHashMap<String, CacheEntry<Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = store[key] ?: return null
        if (System.currentTimeMillis() - entry.timestampMs > ttlMs) {
            store.remove(key)
            return null
        }
        return entry.data as? T
    }

    fun <T> put(key: String, data: T) {
        store[key] = CacheEntry(data as Any, System.currentTimeMillis())
    }

    fun invalidate(key: String) = store.remove(key)

    fun clear() = store.clear()
}
