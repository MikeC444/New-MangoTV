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
private const val MAX_GENRE_ROWS_PER_FAMILY = 6

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

    // Addons like Cinemeta declare a separate catalog per content type
    // (movie/top, series/top) that otherwise mirror each other exactly --
    // same catalog id, same genre options. Grouping by catalog id merges
    // those into one family so "Action" becomes a single row containing
    // both movies and TV shows instead of two near-identical rows, one per
    // type. An addon with genuinely distinct catalogs sharing a type (e.g.
    // separate "popular" and "new" movie catalogs) still gets its own
    // family per id, so unrelated catalogs never get merged together.
    private val catalogFamilies: List<List<AddonCatalogDef>> =
        supportedCatalogs.groupBy { it.id }.values.toList()

    override suspend fun getFeatured(): List<Content> {
        val firstCatalog = supportedCatalogs.firstOrNull() ?: return emptyList()
        return runCatching { client.fetchCatalog(manifestUrl, firstCatalog.type, firstCatalog.id) }
            .getOrDefault(emptyList())
            .take(3)
            .map { it.toContent(providerId = id) }
    }

    // One row per catalog family, PLUS one additional row per genre the
    // family declares (via its members' "genre" extra options) -- this is
    // how addons like Cinemeta, which only expose a single base "top"
    // catalog per type, actually produce Action/Comedy/Horror/etc. variety.
    // All fetches (base + every genre, across every family) run in
    // parallel so the genre fan-out doesn't multiply Home's real load time.
    override suspend fun getHomeSections(): List<HomeSection> = coroutineScope {
        catalogFamilies.flatMap { family ->
            val genres = family
                .flatMap { it.extra.firstOrNull { extra -> extra.name == "genre" }?.options.orEmpty() }
                .distinct()
                .take(MAX_GENRE_ROWS_PER_FAMILY)

            // Family members whose genre extra is required can't answer an
            // unfiltered request -- only the rest contribute to the base row.
            val baseCatalogs = family.filter { catalogDef ->
                catalogDef.extra.firstOrNull { extra -> extra.name == "genre" }?.isRequired != true
            }
            val baseRowDeferred = if (baseCatalogs.isNotEmpty()) {
                val title = family.firstNotNullOfOrNull { it.name } ?: manifest.name
                listOf(async { fetchMergedSection(baseCatalogs, title = title, extra = emptyMap(), idSuffix = null) })
            } else {
                emptyList()
            }

            val genreRowDeferreds = genres.map { genre ->
                val catalogsForGenre = family.filter { catalogDef ->
                    genre in catalogDef.extra.firstOrNull { extra -> extra.name == "genre" }?.options.orEmpty()
                }
                async { fetchMergedSection(catalogsForGenre, title = genre, extra = mapOf("genre" to genre), idSuffix = genre) }
            }

            baseRowDeferred + genreRowDeferreds
        }.awaitAll().filterNotNull()
    }

    // Fetches every member of one catalog family in parallel and interleaves
    // their results into a single row (movie[0], series[0], movie[1],
    // series[1], ...) rather than concatenating, so a merged row actually
    // reads as mixed content instead of "all the movies, then all the
    // shows".
    private suspend fun fetchMergedSection(
        catalogDefs: List<AddonCatalogDef>,
        title: String,
        extra: Map<String, String>,
        idSuffix: String?
    ): HomeSection? = coroutineScope {
        val familyId = catalogDefs.firstOrNull()?.id ?: return@coroutineScope null
        val perCatalogItems = catalogDefs.map { catalogDef ->
            async {
                runCatching { client.fetchCatalog(manifestUrl, catalogDef.type, catalogDef.id, extra) }
                    .getOrNull()
                    ?.map { it.toContent(providerId = id) }
                    .orEmpty()
            }
        }.awaitAll()

        val items = interleave(perCatalogItems)
        if (items.isEmpty()) return@coroutineScope null

        HomeSection(
            id = listOfNotNull(manifest.id, familyId, idSuffix).joinToString("_"),
            title = title,
            items = items
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

private fun <T> interleave(lists: List<List<T>>): List<T> {
    if (lists.size == 1) return lists[0]
    val result = mutableListOf<T>()
    val maxSize = lists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until maxSize) {
        for (list in lists) {
            if (i < list.size) result += list[i]
        }
    }
    return result
}
