package com.mangotv.app.data.provider

import com.mangotv.app.data.addon.StremioAddonClient
import com.mangotv.app.data.addon.toContent
import com.mangotv.app.data.model.AddonManifest
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.HomeSection

private val SUPPORTED_CATALOG_TYPES = setOf("movie", "series")

/**
 * A [CatalogProvider] backed by a real, user-installed Stremio-protocol
 * addon. It normalizes whatever the addon returns into Mango TV's own
 * [Content]/[HomeSection] models — the rest of the app never touches the
 * addon's wire format directly.
 */
class StremioAddonProvider(
    private val manifestUrl: String,
    private val manifest: AddonManifest,
    private val client: StremioAddonClient
) : CatalogProvider {

    override val id: String = manifest.id
    override val name: String = manifest.name

    private val supportedCatalogs = manifest.catalogs.filter { it.type in SUPPORTED_CATALOG_TYPES }

    override suspend fun getFeatured(): List<Content> {
        val firstCatalog = supportedCatalogs.firstOrNull() ?: return emptyList()
        return runCatching { client.fetchCatalog(manifestUrl, firstCatalog.type, firstCatalog.id) }
            .getOrDefault(emptyList())
            .take(3)
            .map { it.toContent(providerId = id) }
    }

    override suspend fun getHomeSections(): List<HomeSection> {
        return supportedCatalogs.mapNotNull { catalogDef ->
            val metas = runCatching { client.fetchCatalog(manifestUrl, catalogDef.type, catalogDef.id) }
                .getOrNull()
                ?: return@mapNotNull null
            if (metas.isEmpty()) return@mapNotNull null

            HomeSection(
                id = "${manifest.id}_${catalogDef.type}_${catalogDef.id}",
                title = catalogDef.name ?: "${manifest.name} · ${catalogDef.type.replaceFirstChar { it.uppercase() }}",
                items = metas.map { it.toContent(providerId = id) }
            )
        }
    }

    override suspend fun getDetails(type: ContentType, id: String): Content? {
        val stremioType = if (type == ContentType.TV_SHOW) "series" else "movie"
        return runCatching { client.fetchMeta(manifestUrl, stremioType, id) }
            .getOrNull()
            ?.toContent(providerId = this.id)
    }
}
