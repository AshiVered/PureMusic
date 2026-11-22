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
import io.ktor.http.isSuccess
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
