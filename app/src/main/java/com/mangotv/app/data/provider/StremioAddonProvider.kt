package com.mangotv.app.data.provider

import com.mangotv.app.data.addon.StremioAddonClient
import com.mangotv.app.data.addon.toContent
import com.mangotv.app.data.addon.toStream
import com.mangotv.app.data.model.AddonCatalogDef
import com.mangotv.app.data.model.AddonManifest
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.model.Stream
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val SUPPORTED_CATALOG_TYPES = setOf("movie", "series")

// Addons like Cinemeta expose only one base catalog per type and rely on a
// "genre" extra's declared options to produce the rest (Action, Comedy,
// ...) -- without this cap a single catalog with a long genre list could
// balloon Home into dozens of rows and requests. Kept fairly low (not just
// "not unbounded"): every never-before-scrolled-to row composes a burst of
// poster loads the instant it enters view, and with Cinemeta's ~19 movie
// genres alone a cap of 10 meant ~22 total rows for one addon -- enough to
// make routine vertical scrolling visibly stutter as each new row's images
// loaded in. 6 still gives far more variety than the single "Popular" row
// this app had before genre fan-out existed at all.
private const val MAX_GENRE_ROWS_PER_CATALOG = 6

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

    // One row per catalog def as before, PLUS one additional row per genre
    // the catalog declares (via its "genre" extra's options) -- this is how
    // addons like Cinemeta, which only expose a single base "top" catalog
    // per type, actually produce Action/Comedy/Horror/etc. variety. All
    // fetches (base + every genre) run in parallel so the genre fan-out
    // doesn't multiply Home's real load time.
    override suspend fun getHomeSections(): List<HomeSection> = coroutineScope {
        // Launch every fetch (base + every genre, across every catalog def)
        // before awaiting any of them -- awaitAll() below is what actually
        // suspends, so this flatMap itself never blocks on one row before
        // starting the next.
        supportedCatalogs.flatMap { catalogDef ->
            val genreExtra = catalogDef.extra.firstOrNull { it.name == "genre" }
            val genres = genreExtra?.options.orEmpty().take(MAX_GENRE_ROWS_PER_CATALOG)

            // A required genre extra means the addon won't answer an
            // unfiltered request at all, so there's no base row to fetch.
            val baseRowDeferred = if (genreExtra?.isRequired != true) {
                listOf(async { fetchSection(catalogDef, title = catalogDef.name, extra = emptyMap()) })
            } else {
                emptyList()
            }
            val genreRowDeferreds = genres.map { genre ->
                async { fetchSection(catalogDef, title = genre, extra = mapOf("genre" to genre), idSuffix = genre) }
            }

            baseRowDeferred + genreRowDeferreds
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchSection(
        catalogDef: AddonCatalogDef,
        title: String?,
        extra: Map<String, String>,
        idSuffix: String? = null
    ): HomeSection? {
        val metas = runCatching { client.fetchCatalog(manifestUrl, catalogDef.type, catalogDef.id, extra) }
            .getOrNull()
            ?: return null
        if (metas.isEmpty()) return null

        return HomeSection(
            id = listOfNotNull(manifest.id, catalogDef.type, catalogDef.id, idSuffix).joinToString("_"),
            title = title ?: "${manifest.name} · ${catalogDef.type.replaceFirstChar { it.uppercase() }}",
            items = metas.map { it.toContent(providerId = id) }
        )
    }

    override suspend fun getDetails(type: ContentType, id: String): Content? {
        val stremioType = if (type == ContentType.TV_SHOW) "series" else "movie"
        return runCatching { client.fetchMeta(manifestUrl, stremioType, id) }
            .getOrNull()
            ?.toContent(providerId = this.id)
    }

    override suspend fun getStreams(type: ContentType, id: String, season: Int?, episode: Int?): List<Stream> {
        val stremioType = if (type == ContentType.TV_SHOW) "series" else "movie"
        val requestId = if (season != null && episode != null) "$id:$season:$episode" else id
        return runCatching { client.fetchStreams(manifestUrl, stremioType, requestId) }
            .getOrDefault(emptyList())
            .map { it.toStream(providerId = this.id, providerLabel = this.name) }
    }
}
