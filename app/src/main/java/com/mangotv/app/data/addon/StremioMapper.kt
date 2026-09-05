package com.mangotv.app.data.addon

import com.mangotv.app.data.model.CastMember
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.Episode
import com.mangotv.app.data.model.Genre
import com.mangotv.app.data.model.Season

fun StremioMetaPreview.toContent(providerId: String): Content {
    val year = releaseInfo
        ?.takeWhile { it.isDigit() }
        ?.takeIf { it.length == 4 }
        ?.toIntOrNull()

    val runtimeMinutes = runtime
        ?.filter { it.isDigit() }
        ?.toIntOrNull()

    return Content(
        id = id,
        type = if (type == "series") ContentType.TV_SHOW else ContentType.MOVIE,
        title = name,
        description = description.orEmpty(),
        posterUrl = poster,
        backdropUrl = background ?: poster,
        logoUrl = logo,
        year = year,
        runtimeMinutes = runtimeMinutes,
        rating = imdbRating?.toDoubleOrNull(),
        genres = genres.orEmpty().map { Genre(id = it.lowercase(), name = it) },
        providerId = providerId
    )
}

fun StremioMeta.toContent(providerId: String): Content {
    val year = releaseInfo
        ?.takeWhile { it.isDigit() }
        ?.takeIf { it.length == 4 }
        ?.toIntOrNull()

    val runtimeMinutes = runtime
        ?.filter { it.isDigit() }
        ?.toIntOrNull()

    return Content(
        id = id,
        type = if (type == "series") ContentType.TV_SHOW else ContentType.MOVIE,
        title = name,
        description = description.orEmpty(),
        posterUrl = poster,
        backdropUrl = background ?: poster,
        logoUrl = logo,
        year = year,
        runtimeMinutes = runtimeMinutes,
        rating = imdbRating?.toDoubleOrNull(),
        genres = genres.orEmpty().map { Genre(id = it.lowercase(), name = it) },
        cast = cast.orEmpty().map { CastMember(name = it) },
        director = director?.takeIf { it.isNotEmpty() }?.joinToString(", "),
        providerId = providerId,
        seasons = videos.orEmpty().toSeasons()
    )
}

/** Groups a series meta's flat video list into per-season, episode-ordered [Season]s. */
private fun List<StremioVideo>.toSeasons(): List<Season> =
    filter { it.season != null && it.episode != null }
        .groupBy { it.season!! }
        .toSortedMap()
        .map { (seasonNumber, videos) ->
            Season(
                seasonNumber = seasonNumber,
                name = if (seasonNumber == 0) "Specials" else "Season $seasonNumber",
                episodes = videos.sortedBy { it.episode }.map { it.toEpisode() }
            )
        }

private fun StremioVideo.toEpisode(): Episode = Episode(
    id = id,
    seasonNumber = season ?: 0,
    episodeNumber = episode ?: 0,
    title = title ?: name ?: "Episode ${episode ?: 0}",
    description = overview ?: description.orEmpty(),
    thumbnailUrl = thumbnail,
    runtimeMinutes = null
)
