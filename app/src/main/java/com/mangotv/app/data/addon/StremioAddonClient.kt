package com.mangotv.app.data.addon

import com.mangotv.app.data.model.AddonManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Talks the Stremio addon protocol: fetch a manifest, then use it to pull
 * catalogs, streams and subtitles. This is the one place in the app that
 * knows about the wire format — everything above it works with normalized
 * models.
 */
class StremioAddonClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchManifest(manifestUrl: String): AddonManifest = withContext(Dispatchers.IO) {
        val body = get(manifestUrl)
        json.decodeFromString(AddonManifest.serializer(), body)
    }

    suspend fun fetchCatalog(manifestUrl: String, type: String, catalogId: String): List<StremioMetaPreview> =
        withContext(Dispatchers.IO) {
            val base = AddonUrl.resourceBase(manifestUrl)
            val url = "$base/catalog/$type/${encode(catalogId)}.json"
            val body = get(url)
            json.decodeFromString(StremioCatalogResponse.serializer(), body).metas
        }

    suspend fun fetchStreams(manifestUrl: String, type: String, id: String): List<StremioStream> =
        withContext(Dispatchers.IO) {
            val base = AddonUrl.resourceBase(manifestUrl)
            // Series stream ids are "imdbId:season:episode" — encoding the
            // whole id in one pass would percent-encode the colons
            // themselves, breaking the literal path segment addons expect.
            // Encoding each part separately preserves them; a plain
            // colon-free movie id is unaffected (single-element split).
            val encodedId = id.split(":").joinToString(":") { encode(it) }
            val url = "$base/stream/$type/$encodedId.json"
            val body = get(url)
            json.decodeFromString(StremioStreamResponse.serializer(), body).streams
        }

    suspend fun fetchMeta(manifestUrl: String, type: String, id: String): StremioMeta? =
        withContext(Dispatchers.IO) {
            val base = AddonUrl.resourceBase(manifestUrl)
            val url = "$base/meta/$type/${encode(id)}.json"
            val body = get(url)
            json.decodeFromString(StremioMetaResponse.serializer(), body).meta
        }

    private fun encode(segment: String): String = URLEncoder.encode(segment, "UTF-8")

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} from $url" }
            return response.body?.string() ?: error("Empty response from $url")
        }
    }
}
