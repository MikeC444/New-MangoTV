package com.mangotv.app.data.provider

import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.HomeSection

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
}
