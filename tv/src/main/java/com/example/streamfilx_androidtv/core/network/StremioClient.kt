package com.example.streamfilx_androidtv.core.network

import com.example.streamfilx_androidtv.core.models.AddonManifest
import com.example.streamfilx_androidtv.core.models.CatalogResponse
import com.example.streamfilx_androidtv.core.models.MetaResponse
import com.example.streamfilx_androidtv.core.models.StreamResponse
import com.example.streamfilx_androidtv.core.models.SubtitleResponse
import com.example.streamfilx_androidtv.core.models.WellKnownAddon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioClient @Inject constructor(
    private val apiClient: ApiClient,
    private val cache: ResponseCache,
) {

    // ── Manifest ──────────────────────────────────────────────────────────────

    suspend fun fetchManifest(baseUrl: String): AddonManifest {
        val url = "${baseUrl.trimBase()}/manifest.json"
        return cache.get<AddonManifest>(url) ?: run {
            val result = apiClient.get(url, AddonManifest::class.java)
            cache.put(url, result, ResponseCache.MANIFEST_TTL)
            result
        }
    }

    // ── Catalog ───────────────────────────────────────────────────────────────

    suspend fun fetchCatalog(
        baseUrl: String,
        type: String,
        catalogId: String,
        extra: Map<String, String>? = null,
    ): CatalogResponse {
        val url = buildCatalogUrl(baseUrl, type, catalogId, extra)
        val isSearch = extra?.containsKey("search") == true
        val ttl = if (isSearch) ResponseCache.SEARCH_TTL else ResponseCache.CATALOG_TTL
        return cache.get<CatalogResponse>(url) ?: run {
            val result = apiClient.get(url, CatalogResponse::class.java)
            cache.put(url, result, ttl)
            result
        }
    }

    suspend fun searchCatalog(
        baseUrl: String,
        type: String,
        catalogId: String,
        query: String,
    ): CatalogResponse = fetchCatalog(
        baseUrl = baseUrl,
        type = type,
        catalogId = catalogId,
        extra = mapOf("search" to query),
    )

    // ── Meta ──────────────────────────────────────────────────────────────────

    suspend fun fetchMeta(baseUrl: String, type: String, id: String): MetaResponse {
        val url = "${baseUrl.trimBase()}/meta/$type/$id.json"
        return cache.get<MetaResponse>(url) ?: run {
            val result = apiClient.get(url, MetaResponse::class.java)
            cache.put(url, result, ResponseCache.META_TTL)
            result
        }
    }

    // ── Streams ───────────────────────────────────────────────────────────────

    // Streams are NOT cached — RD-resolved URLs expire quickly
    suspend fun fetchStreams(baseUrl: String, type: String, id: String): StreamResponse {
        val url = "${baseUrl.trimBase()}/stream/$type/$id.json"
        return apiClient.get(url, StreamResponse::class.java)
    }

    // ── Subtitles ─────────────────────────────────────────────────────────────

    suspend fun fetchSubtitles(baseUrl: String, type: String, id: String): SubtitleResponse {
        val url = "${baseUrl.trimBase()}/subtitles/$type/$id.json"
        return apiClient.get(url, SubtitleResponse::class.java)
    }

    // ── Convenience: Cinemeta ─────────────────────────────────────────────────

    suspend fun fetchCinemetaCatalog(type: String, catalogId: String = "top"): CatalogResponse =
        fetchCatalog(WellKnownAddon.CINEMETA, type, catalogId)

    suspend fun fetchCinemetaMeta(type: String, id: String): MetaResponse =
        fetchMeta(WellKnownAddon.CINEMETA, type, id)

    suspend fun searchCinemeta(type: String, query: String): CatalogResponse =
        searchCatalog(WellKnownAddon.CINEMETA, type, "top", query)

    // ── URL construction ──────────────────────────────────────────────────────

    private fun buildCatalogUrl(
        baseUrl: String,
        type: String,
        catalogId: String,
        extra: Map<String, String>?,
    ): String {
        val base = baseUrl.trimBase()
        if (extra.isNullOrEmpty()) return "$base/catalog/$type/$catalogId.json"
        val extraStr = extra.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$base/catalog/$type/$catalogId/$extraStr.json"
    }

    private fun String.trimBase(): String = trimEnd('/')
}
