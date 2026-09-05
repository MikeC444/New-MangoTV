package com.mangotv.app.data.addon

import kotlinx.serialization.Serializable

/**
 * Wire-format models for the Stremio addon protocol's catalog/meta/stream
 * responses. These are intentionally kept separate from Mango TV's internal
 * [com.mangotv.app.data.model.Content] model — [StremioMetaPreview.toContent]
 * is the single place that normalizes provider data into our own shape.
 */

@Serializable
data class StremioCatalogResponse(
    val metas: List<StremioMetaPreview> = emptyList()
)

@Serializable
data class StremioMetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String>? = null,
    val runtime: String? = null
)

@Serializable
data class StremioStreamResponse(
    val streams: List<StremioStream> = emptyList()
)

@Serializable
data class StremioStream(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null
)
