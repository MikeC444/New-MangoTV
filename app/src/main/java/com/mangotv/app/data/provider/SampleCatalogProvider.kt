package com.mangotv.app.data.provider

import com.mangotv.app.data.model.CastMember
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.Genre
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.model.RowStyle
import com.mangotv.app.data.model.WatchProgress
import kotlinx.coroutines.delay

/**
 * Built-in sample provider. It fabricates a catalog of original, fictional
 * titles so the app has rich, realistic-looking content to render before
 * any real provider/addon is installed. Artwork URLs point at a stable
 * placeholder image service purely as a stand-in for real provider
 * artwork — [Content.posterUrl] / [Content.backdropUrl] are plain strings
 * so swapping in a real metadata source later requires no UI changes.
 */
class SampleCatalogProvider : CatalogProvider {

    override val id: String = "mango.sample"
    override val name: String = "Mango Sample Catalog"

    private fun poster(seed: String) = "https://picsum.photos/seed/mango-poster-$seed/400/600"
    private fun backdrop(seed: String) = "https://picsum.photos/seed/mango-backdrop-$seed/1280/720"

    private val genrePool = listOf(
        Genre("action", "Action"),
        Genre("drama", "Drama"),
        Genre("scifi", "Sci-Fi"),
        Genre("comedy", "Comedy"),
        Genre("horror", "Horror"),
        Genre("thriller", "Thriller"),
        Genre("animation", "Animation"),
        Genre("crime", "Crime"),
        Genre("fantasy", "Fantasy"),
        Genre("romance", "Romance")
    )

    private fun genres(vararg keys: String) = keys.map { key -> genrePool.first { it.id == key } }

    private val castPool = listOf(
        "Mara Ellison", "Devon Cole", "Priya Anand", "Jonas Riker", "Talia Brandt",
        "Marcus Odell", "Elena Voss", "Sam Whitaker", "Nadia Reyes", "Theo Marchetti"
    )

    private fun cast(count: Int, offset: Int) =
        (0 until count).map { i -> CastMember(castPool[(offset + i) % castPool.size]) }

    private val titleBank = listOf(
        Triple("Crimson Horizon", ContentType.MOVIE, listOf("action", "thriller")),
        Triple("The Last Signal", ContentType.MOVIE, listOf("scifi", "drama")),
        Triple("Neon Requiem", ContentType.TV_SHOW, listOf("scifi", "crime")),
        Triple("Paper Moons", ContentType.MOVIE, listOf("drama", "romance")),
        Triple("Static & Silence", ContentType.TV_SHOW, listOf("horror", "thriller")),
        Triple("Glasshouse", ContentType.TV_SHOW, listOf("drama", "crime")),
        Triple("Ember Road", ContentType.MOVIE, listOf("action", "drama")),
        Triple("The Cartographer", ContentType.MOVIE, listOf("fantasy", "action")),
        Triple("Low Tide", ContentType.TV_SHOW, listOf("crime", "thriller")),
        Triple("Wildfire Season", ContentType.MOVIE, listOf("drama", "action")),
        Triple("Nightshade Prep", ContentType.TV_SHOW, listOf("comedy", "drama")),
        Triple("Ashen Court", ContentType.TV_SHOW, listOf("fantasy", "drama")),
        Triple("Departure Lane", ContentType.MOVIE, listOf("thriller", "drama")),
        Triple("Hollow Orbit", ContentType.MOVIE, listOf("scifi", "action")),
        Triple("The Understudy", ContentType.MOVIE, listOf("drama", "comedy")),
        Triple("Marigold & Rust", ContentType.TV_SHOW, listOf("romance", "drama")),
        Triple("Blackwater Ridge", ContentType.TV_SHOW, listOf("horror", "crime")),
        Triple("Second Wind", ContentType.MOVIE, listOf("comedy", "romance")),
        Triple("The Quiet Ones", ContentType.MOVIE, listOf("horror", "thriller")),
        Triple("Faultline", ContentType.TV_SHOW, listOf("action", "crime")),
        Triple("Copper Sky", ContentType.MOVIE, listOf("scifi", "fantasy")),
        Triple("Midnight Freight", ContentType.TV_SHOW, listOf("thriller", "action")),
        Triple("Sundown Motel", ContentType.MOVIE, listOf("horror", "comedy")),
        Triple("The Archivist", ContentType.TV_SHOW, listOf("drama", "scifi")),
        Triple("Vantage Point Zero", ContentType.MOVIE, listOf("action", "scifi")),
        Triple("Little Static", ContentType.TV_SHOW, listOf("animation", "comedy")),
        Triple("Foxglove Lane", ContentType.MOVIE, listOf("drama", "romance")),
        Triple("The Long Shift", ContentType.TV_SHOW, listOf("drama", "crime")),
        Triple("Aftermarket", ContentType.MOVIE, listOf("comedy", "crime")),
        Triple("Cloudbreak", ContentType.TV_SHOW, listOf("fantasy", "action")),
        Triple("Origami Heart", ContentType.MOVIE, listOf("romance", "drama")),
        Triple("The Nightshift Kings", ContentType.TV_SHOW, listOf("comedy", "crime")),
        Triple("Radiant Static", ContentType.MOVIE, listOf("scifi", "thriller")),
        Triple("Winter Ledger", ContentType.TV_SHOW, listOf("drama", "thriller")),
        Triple("Sable & Smoke", ContentType.MOVIE, listOf("action", "crime")),
        Triple("Paperclip Empire", ContentType.TV_SHOW, listOf("comedy", "drama")),
    )

    private val descriptions = listOf(
        "When old loyalties resurface, one decision sends a ripple no one can undo.",
        "A fractured family, a city on edge, and a truth someone will kill to bury.",
        "In a world running out of time, the only way out is through.",
        "Two strangers, one impossible night, and a choice that changes everything.",
        "The signal was supposed to be silence. Instead, it started answering back.",
        "Everyone in this town has a secret. Some are worth dying to protect.",
        "She thought she'd left it all behind — until it came looking for her.",
        "A gripping story of ambition, betrayal, and the price of getting exactly what you wanted.",
        "Not every ending is a beginning. Some are just the truth catching up.",
        "Set against a fading skyline, a story about what we owe the people we love."
    )

    private fun buildContent(index: Int, triple: Triple<String, ContentType, List<String>>): Content {
        val (title, type, genreKeys) = triple
        val seed = "t$index"
        val year = 2016 + (index % 9)
        return Content(
            id = "sample-$index",
            type = type,
            title = title,
            description = descriptions[index % descriptions.size],
            posterUrl = poster(seed),
            backdropUrl = backdrop(seed),
            year = year,
            ageRating = listOf("PG-13", "TV-14", "R", "TV-MA")[index % 4],
            runtimeMinutes = if (type == ContentType.MOVIE) 95 + (index % 6) * 7 else 42 + (index % 3) * 4,
            rating = 6.2 + (index % 38) / 10.0,
            genres = genres(*genreKeys.toTypedArray()),
            cast = cast(4, index),
            director = castPool[(index + 3) % castPool.size],
            providerId = id
        )
    }

    private val allContent: List<Content> by lazy {
        titleBank.mapIndexed { index, triple -> buildContent(index, triple) }
    }

    override suspend fun getFeatured(): List<Content> {
        delay(200)
        return allContent.filter { it.rating != null && it.rating!! >= 8.0 }.take(5)
            .ifEmpty { allContent.take(5) }
    }

    override suspend fun getHomeSections(): List<HomeSection> {
        delay(650)

        val continueWatching = allContent.shuffled(kotlin.random.Random(1)).take(6).mapIndexed { i, c ->
            c.copy(
                watchProgress = WatchProgress(
                    positionMs = (10 + i * 7).toLong() * 60_000L,
                    durationMs = (c.runtimeMinutes ?: 100).toLong() * 60_000L,
                    seasonNumber = if (c.type == ContentType.TV_SHOW) 1 else null,
                    episodeNumber = if (c.type == ContentType.TV_SHOW) i + 1 else null
                )
            )
        }

        fun byGenre(key: String) = allContent.filter { c -> c.genres.any { it.id == key } }

        return listOf(
            HomeSection("continue_watching", "Continue Watching", continueWatching, RowStyle.CONTINUE_WATCHING),
            HomeSection("trending", "Trending Now", allContent.shuffled(kotlin.random.Random(2)).take(12)),
            HomeSection(
                "popular_movies", "Popular Movies",
                allContent.filter { it.type == ContentType.MOVIE }.take(12)
            ),
            HomeSection(
                "popular_tv", "Popular TV Shows",
                allContent.filter { it.type == ContentType.TV_SHOW }.take(12)
            ),
            HomeSection(
                "recently_added", "Recently Added",
                allContent.sortedByDescending { it.year }.take(12)
            ),
            HomeSection(
                "recommended", "Recommended For You",
                allContent.shuffled(kotlin.random.Random(3)).take(12)
            ),
            HomeSection("action_thrillers", "Action & Thrillers", byGenre("action").take(10)),
            HomeSection("sci_fi", "Sci-Fi", byGenre("scifi").take(10)),
            HomeSection("crime_drama", "Crime & Drama", byGenre("crime").take(10)),
        ).filter { it.items.isNotEmpty() }
    }
}
