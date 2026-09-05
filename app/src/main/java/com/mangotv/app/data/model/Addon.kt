package com.mangotv.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors the Stremio addon manifest.json contract
 * (https://github.com/Stremio/stremio-addon-sdk) — the open protocol that
 * both Stremio and the Nuvio addon ecosystem are built on. Any field an
 * addon doesn't declare is simply left null/empty; unknown extra fields on
 * the wire are ignored by the decoder rather than failing the parse, since
 * real-world addons vary a lot in what else they include.
 */
@Serializable
data class AddonManifest(
    val id: String,
    val name: String,
    val version: String = "0.0.0",
    val description: String? = null,
    val logo: String? = null,
    val background: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val catalogs: List<AddonCatalogDef> = emptyList(),
    val idPrefixes: List<String> = emptyList()
)

@Serializable
data class AddonCatalogDef(
    val type: String,
    val id: String,
    val name: String? = null
)

/**
 * An addon as tracked by Mango TV: where it came from, its last-known
 * manifest, and whether it's currently contributing to the catalog. This is
 * both the UI-facing model (Settings > Addons) and the persisted form
 * (serialized as-is into DataStore), since the two never need to diverge.
 */
@Serializable
data class InstalledAddon(
    val manifestUrl: String,
    val manifest: AddonManifest,
    val enabled: Boolean = true
)
