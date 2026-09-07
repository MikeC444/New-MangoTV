package com.mangotv.app.data.provider

import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.model.Stream

/**
 * A CatalogProvider is Mango TV's equivalent of a Stremio-style addon: a
 * self-contained source of catalogs, metadata and (eventually) streams.
 *
 * The UI layer only ever talks to [ProviderRegistry] and works with the
 * normalized [Content] / [HomeSection] models below — it never knows or
 * cares which provider a given piece of content came from. Real providers
 * (backed by remote APIs) can be registered later without any UI changes.
 */
interface CatalogProvider {
    val id: String
    val name: String

    suspend fun getHomeSections(): List<HomeSection>

    suspend fun getFeatured(): List<Content>

    /**
     * Full detail lookup for a single title (cast, director, extended
     * description) — richer than what a catalog listing's preview items
     * carry. Returns null if this provider can't resolve the id.
     */
    suspend fun getDetails(type: ContentType, id: String): Content?

    /**
     * Playable sources for a title, or a specific episode when [season]/
     * [episode] are given. Different addons can each return different
     * quality options for the same title, so callers should query every
     * active provider and merge results rather than treating this like
     * [getDetails] (which only makes sense against the one owning provider).
     */
    suspend fun getStreams(type: ContentType, id: String, season: Int? = null, episode: Int? = null): List<Stream>

    /**
     * The same base+genre row set [getHomeSections] builds, restricted to
     * one content type — backs the dedicated Movies/TV Shows browse screens.
     */
    suspend fun getSectionsByType(type: ContentType): List<HomeSection>

    /** Every genre name this provider's catalogs declare, deduplicated. Backs the Genres picker screen. */
    suspend fun getAvailableGenres(): List<String>

    /**
     * One merged catalogue for a single genre, mixing every content type
     * that declares it (same merge behavior [getHomeSections]'s own genre
     * rows already use). Null if this provider has nothing for that genre.
     */
    suspend fun getGenreSection(genre: String): HomeSection?

    /**
     * Searches this provider for [query]. Implementations should prefer a
     * real server-side search where the addon supports one, falling back to
     * client-side matching over already-fetchable catalogs otherwise, so
     * Search still returns something for addons that don't declare search
     * support.
     */
    suspend fun search(query: String): List<Content>
}
