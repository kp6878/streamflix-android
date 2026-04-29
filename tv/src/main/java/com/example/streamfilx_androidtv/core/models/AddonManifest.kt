package com.example.streamfilx_androidtv.core.models

import java.util.UUID

data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val resources: List<String>,        // ["catalog", "meta", "stream", "subtitles"]
    val types: List<String>,            // ["movie", "series"]
    val catalogs: List<Catalog>? = null,
    val idPrefixes: List<String>? = null,
    val behaviorHints: ManifestBehaviorHints? = null,
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<ExtraDefinition>? = null,
)

data class ExtraDefinition(
    val name: String,
    val isRequired: Boolean? = null,
    val options: List<String>? = null,
)

data class ManifestBehaviorHints(
    val adult: Boolean? = null,
    val p2p: Boolean? = null,
    val configurable: Boolean? = null,
    val configurationRequired: Boolean? = null,
)

data class ConfiguredAddon(
    val id: String = UUID.randomUUID().toString(),
    val manifestUrl: String,
    val baseUrl: String,
    val manifest: AddonManifest? = null,
    val isEnabled: Boolean = true,
) {
    fun supports(resource: String): Boolean =
        manifest?.resources?.any { it.equals(resource, ignoreCase = true) } == true

    fun supportsCatalog(type: String): Boolean =
        manifest?.catalogs?.any { it.type == type } == true
}

object WellKnownAddon {
    const val CINEMETA = "https://v3-cinemeta.strem.io"
    const val OPENSUBTITLES_STREMIO = "https://opensubtitles-v3.strem.io"
    const val TORRENTIO = "https://torrentio.strem.fun"
}
