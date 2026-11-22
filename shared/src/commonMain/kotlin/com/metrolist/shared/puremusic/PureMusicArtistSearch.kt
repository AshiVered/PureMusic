/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.innertubex.models.YouTubeClient
import com.metrolist.shared.api.FEED_RESPONSE_LIMIT
import com.metrolist.shared.api.YouTubeItemParser
import com.metrolist.shared.api.YouTubeSearchParser.parseSearchResponse
import com.metrolist.shared.api.bodyLimited
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.state.GlobalYouTube
import com.metrolist.shared.state.GlobalYouTubeRepository
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Search for the approval screen: music artists, podcasts and YouTube channels. Metrolist's search runs
 * through [com.metrolist.shared.settings.BlockedContent], which would hide everything not approved yet, so
 * this calls InnerTube directly.
 */
object PureMusicArtistSearch {
    /** Artists first, then podcasts, then other channels. Fails only if every category failed. */
    suspend fun search(query: String): List<ApprovedArtist> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val filters = listOf(SearchFilter.ARTIST, SearchFilter.PODCAST, SearchFilter.PROFILE)
            val results =
                coroutineScope {
                    filters.map { filter -> async { runCatching { searchFilter(query.trim(), filter) } } }.awaitAll()
                }
            results.forEach { result -> result.exceptionOrNull()?.let { if (it is CancellationException) throw it } }
            if (results.all { it.isFailure }) throw results.first().exceptionOrNull()!!
            val artists = results[0].getOrDefault(emptyList()).filterIsInstance<ArtistItem>()
            val podcasts = results[1].getOrDefault(emptyList()).filterIsInstance<PodcastItem>()
            val channels = results[2].getOrDefault(emptyList()).filterIsInstance<ArtistItem>()
            buildList {
                artists.forEach { add(ApprovedArtist(it.id, it.title, it.thumbnail, ApprovedArtist.KIND_ARTIST)) }
                podcasts.forEach {
                    add(ApprovedArtist(it.id, it.title, it.thumbnail, ApprovedArtist.KIND_PODCAST, channelId = it.channelId))
                }
                channels.forEach { add(ApprovedArtist(it.id, it.title, it.thumbnail, ApprovedArtist.KIND_CHANNEL)) }
            }.distinctBy { it.id }
        }

    /**
     * When [input] is a link or id instead of a search term, resolves it to a channel or podcast.
     * Accepts YouTube / YouTube Music links (also wrapped in Google redirect links), "@handle",
     * channel ids (UC…) and podcast ids (MPSP…). Returns null for ordinary search text.
     */
    suspend fun resolveLink(input: String): ApprovedArtist? =
        withContext(Dispatchers.IO) {
            val target = linkTarget(input.trim()) ?: return@withContext null
            val browseId =
                when {
                    CHANNEL_ID.matches(target) || target.startsWith("MPSP") -> target
                    else -> resolveUrl(target) ?: return@withContext null
                }
            when {
                browseId.startsWith("UC") -> channel(browseId)
                browseId.startsWith("MPSP") -> podcast(browseId)
                else -> null
            }
        }

    private val CHANNEL_ID = Regex("^UC[\\w-]{22}$")

    private fun linkTarget(input: String): String? {
        if (CHANNEL_ID.matches(input) || input.matches(Regex("^MPSP[\\w-]+$"))) return input
        if (input.startsWith("@") && !input.contains(' ')) return "https://music.youtube.com/$input"
        if (!input.startsWith("http://") && !input.startsWith("https://")) return null
        // Google search results link through google.com/url?…&url=<target>.
        val wrapped =
            Regex("[?&](?:url|q)=([^&]+)").find(input)?.groupValues?.get(1)?.let(::percentDecode)
        val url = wrapped?.takeIf { it.startsWith("http") && "youtu" in it } ?: input
        return url.takeIf { "youtube.com" in it || "youtu.be" in it }
    }

    private fun percentDecode(value: String): String =
        buildString {
            val bytes = mutableListOf<Byte>()
            var i = 0
            fun flush() {
                if (bytes.isNotEmpty()) append(bytes.toByteArray().decodeToString()).also { bytes.clear() }
            }
            while (i < value.length) {
                val c = value[i]
                if (c == '%' && i + 2 <= value.lastIndex) {
                    bytes += value.substring(i + 1, i + 3).toInt(16).toByte()
                    i += 3
                } else {
                    flush()
                    append(if (c == '+') ' ' else c)
                    i++
                }
            }
            flush()
        }

    /** YouTube's navigation/resolve_url: link -> browse id (channel UC…, podcast MPSP…, playlist VL…). */
    private suspend fun resolveUrl(url: String): String? {
        val response =
            GlobalYouTube.client.post("https://music.youtube.com/youtubei/v1/navigation/resolve_url?prettyPrint=false") {
                contentType(ContentType.Application.Json)
                header("Origin", "https://music.youtube.com")
                setBody(
                    buildJsonObject {
                        putJsonObject("context") {
                            putJsonObject("client") {
                                put("clientName", "WEB_REMIX")
                                put("clientVersion", "1.20250101.01.00")
                            }
                        }
                        put("url", url)
                    },
                )
            }
        if (!response.status.isSuccess()) return null
        val endpoint = response.bodyLimited<JsonObject>(FEED_RESPONSE_LIMIT)["endpoint"]?.jsonObject ?: return null
        val browse = endpoint["browseEndpoint"]?.jsonObject ?: return null
        val browseId = browse["browseId"]?.jsonPrimitive?.contentOrNull ?: return null
        val pageType =
            browse["browseEndpointContextSupportedConfigs"]
                ?.jsonObject
                ?.get("browseEndpointContextMusicConfig")
                ?.jsonObject
                ?.get("pageType")
                ?.jsonPrimitive
                ?.contentOrNull
                .orEmpty()
        // Podcasts are also reachable as playlists (VL<id>); their show page is MPSP<id>.
        if (browseId.startsWith("VL") && "PODCAST" in pageType) return "MPSP" + browseId.removePrefix("VL")
        return browseId
    }

    private suspend fun channel(channelId: String): ApprovedArtist? {
        val header =
            GlobalYouTubeRepository.instance
                .getArtist(channelId)
                .sections["header"]
                ?.firstOrNull() as? ArtistItem
        val name = header?.title?.takeIf(String::isNotBlank) ?: return null
        return ApprovedArtist(channelId, name, header.thumbnail, ApprovedArtist.KIND_CHANNEL)
    }

    private suspend fun podcast(podcastId: String): ApprovedArtist? {
        val podcast =
            GlobalYouTubeRepository.instance
                .getPodcastPages(podcastId)
                .firstOrNull()
                ?.podcast ?: return null
        return ApprovedArtist(podcastId, podcast.title, podcast.thumbnail, ApprovedArtist.KIND_PODCAST, channelId = podcast.channelId)
    }

    private suspend fun searchFilter(
        query: String,
        filter: SearchFilter,
    ): List<YTItem> {
        val response =
            GlobalYouTube.innerTube
                .search(YouTubeClient.WEB_REMIX, query = query, params = filter.value, setLogin = false)
                .also { check(it.status.isSuccess()) { "Search failed with HTTP ${it.status.value}" } }
                .bodyLimited<JsonObject>(FEED_RESPONSE_LIMIT)
        val items = parseSearchResponse(response)
        if (items.isNotEmpty()) return items
        // Podcasts and profiles can nest their result renderers under extra wrappers.
        return buildList {
            fun collect(element: JsonElement) {
                when (element) {
                    is JsonObject -> {
                        if (element.keys.any { it.endsWith("Renderer") }) YouTubeItemParser.parseItem(element)?.let(::add)
                        element.values.forEach(::collect)
                    }

                    is JsonArray -> element.forEach(::collect)
                    else -> Unit
                }
            }
            collect(response)
        }
    }
}
