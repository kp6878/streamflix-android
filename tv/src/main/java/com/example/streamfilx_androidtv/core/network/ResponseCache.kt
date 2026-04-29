package com.example.streamfilx_androidtv.core.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseCache @Inject constructor() {

    private val mutex = Mutex()
    private val store = HashMap<String, CacheEntry>()

    private data class CacheEntry(val value: Any, val expiresAt: Long)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(key: String): T? = mutex.withLock {
        val entry = store[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key)
            return null
        }
        entry.value as T
    }

    suspend fun put(key: String, value: Any, ttlMs: Long) = mutex.withLock {
        store[key] = CacheEntry(value, System.currentTimeMillis() + ttlMs)
    }

    suspend fun invalidate(key: String) { mutex.withLock { store.remove(key) } }

    suspend fun invalidatePrefix(prefix: String) = mutex.withLock {
        store.keys.filter { it.startsWith(prefix) }.forEach { store.remove(it) }
    }

    suspend fun clear() = mutex.withLock { store.clear() }

    val size: Int get() = store.size

    companion object {
        val MANIFEST_TTL: Long = TimeUnit.DAYS.toMillis(7)
        val CATALOG_TTL: Long = TimeUnit.HOURS.toMillis(24)
        val META_TTL: Long = TimeUnit.HOURS.toMillis(24)
        val SEARCH_TTL: Long = TimeUnit.HOURS.toMillis(1)
        val STREAM_TTL: Long = TimeUnit.MINUTES.toMillis(30)
    }
}
