package com.mangotv.app.data.provider

import com.mangotv.app.data.model.Content
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
}
