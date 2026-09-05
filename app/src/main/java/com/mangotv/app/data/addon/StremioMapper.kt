package com.mangotv.app.data.addon

import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.Genre

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
        year = year,
        runtimeMinutes = runtimeMinutes,
        rating = imdbRating?.toDoubleOrNull(),
        genres = genres.orEmpty().map { Genre(id = it.lowercase(), name = it) },
        providerId = providerId
    )
}
