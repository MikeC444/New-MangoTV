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
// ...). Now that Settings > Home Rows lets users hide rows they don't want,
// this only needs to be a safety ceiling against a pathological addon
// declaring hundreds of genres, not a curation mechanism -- capping low used
// to mean most of an addon's real genres were simply never fetchable at all,
// which is why they didn't show up as options anywhere.
private const val MAX_GENRE_ROWS = 30

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

    // Addons like Cinemeta declare a separate catalog per content type
    // (movie/top, series/top) that otherwise mirror each other -- same
    // genre options, same intent -- but nothing in the Stremio protocol
    // guarantees they share a literal catalog id, so matching catalogs
    // across types by GENRE NAME (rather than by id, which silently failed
    // to merge anything and produced one "Action" row per type instead of
    // one combined row) is what actually merges movies and TV shows into a
    // single row per genre. All fetches (base + every genre) run in
    // parallel so the genre fan-out doesn't multiply Home's real load time.
    override suspend fun getHomeSections(): List<HomeSection> = coroutineScope {
        // Base ("no genre filter") row: every catalog that can answer an
        // unfiltered request (its genre extra, if any, isn't required)
        // merges into one row, regardless of type.
        val baseCatalogs = supportedCatalogs.filter { catalogDef ->
            catalogDef.extra.firstOrNull { it.name == "genre" }?.isRequired != true
        }
        val baseRowDeferred = if (baseCatalogs.isNotEmpty()) {
            listOf(async { fetchMergedSection(baseCatalogs, title = manifest.name, extra = emptyMap(), rowKey = "base") })
        } else {
            emptyList()
        }

        // Genre rows: the union of every genre any supported catalog
        // declares, each merging every catalog (any type) that lists it.
        val genres = supportedCatalogs
            .flatMap { it.extra.firstOrNull { extra -> extra.name == "genre" }?.options.orEmpty() }
            .distinct()
            .take(MAX_GENRE_ROWS)

        val genreRowDeferreds = genres.map { genre ->
            val catalogsForGenre = supportedCatalogs.filter { catalogDef ->
                genre in catalogDef.extra.firstOrNull { extra -> extra.name == "genre" }?.options.orEmpty()
            }
            async { fetchMergedSection(catalogsForGenre, title = genre, extra = mapOf("genre" to genre), rowKey = genre) }
        }

        (baseRowDeferred + genreRowDeferreds).awaitAll().filterNotNull()
    }

    // Fetches every catalog matched for this row in parallel and interleaves
    // their results (movie[0], series[0], movie[1], series[1], ...) rather
    // than concatenating, so a merged row actually reads as mixed content
    // instead of "all the movies, then all the shows".
    private suspend fun fetchMergedSection(
        catalogDefs: List<AddonCatalogDef>,
        title: String,
        extra: Map<String, String>,
        rowKey: String
    ): HomeSection? = coroutineScope {
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
            id = "${manifest.id}_$rowKey",
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
