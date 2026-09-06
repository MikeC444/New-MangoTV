package com.mangotv.app.navigation

import com.mangotv.app.data.model.ContentType
import java.net.URLEncoder

object MangoRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_ADDONS = "settings/addons"
    const val SETTINGS_ADD_ADDON = "settings/addons/add"
    const val DETAIL_PATTERN = "detail/{providerId}/{type}/{id}"
    const val SOURCES_PATTERN = "sources/{providerId}/{type}/{id}/{season}/{episode}"
    const val PLAYER_PATTERN = "player/{providerId}/{type}/{id}/{season}/{episode}/{streamId}"

    fun detail(providerId: String, type: ContentType, id: String): String {
        val encodedProviderId = URLEncoder.encode(providerId, "UTF-8")
        val encodedId = URLEncoder.encode(id, "UTF-8")
        return "detail/$encodedProviderId/${type.name}/$encodedId"
    }

    fun sources(providerId: String, type: ContentType, id: String, season: Int? = null, episode: Int? = null): String {
        val encodedProviderId = URLEncoder.encode(providerId, "UTF-8")
        val encodedId = URLEncoder.encode(id, "UTF-8")
        return "sources/$encodedProviderId/${type.name}/$encodedId/${season ?: -1}/${episode ?: -1}"
    }

    fun player(
        providerId: String,
        type: ContentType,
        id: String,
        season: Int? = null,
        episode: Int? = null,
        streamId: String
    ): String {
        val encodedProviderId = URLEncoder.encode(providerId, "UTF-8")
        val encodedId = URLEncoder.encode(id, "UTF-8")
        val encodedStreamId = URLEncoder.encode(streamId, "UTF-8")
        return "player/$encodedProviderId/${type.name}/$encodedId/${season ?: -1}/${episode ?: -1}/$encodedStreamId"
    }
}

/** Maps a top-nav label to the route it should navigate to, or null if that section isn't built yet. */
fun routeForNavLabel(label: String): String? = when (label) {
    "Home" -> MangoRoutes.HOME
    "Settings" -> MangoRoutes.SETTINGS
    else -> null
}
