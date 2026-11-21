/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.innertubex.models.YouTubeClient
import com.metrolist.shared.api.FEED_RESPONSE_LIMIT
import com.metrolist.shared.api.YouTubeSearchParser.parseSearchResponse
import com.metrolist.shared.api.bodyLimited
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.state.GlobalYouTube
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/**
 * Artist search for the approval screen. Metrolist's search runs through [com.metrolist.shared.settings.BlockedContent],
 * which would hide every artist that is not approved yet, so this calls InnerTube directly.
 */
object PureMusicArtistSearch {
    suspend fun search(query: String): List<ArtistItem> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val response =
                GlobalYouTube.innerTube
                    .search(YouTubeClient.WEB_REMIX, query = query.trim(), params = SearchFilter.ARTIST.value, setLogin = false)
                    .also { check(it.status.isSuccess()) { "Search failed with HTTP ${it.status.value}" } }
                    .bodyLimited<JsonObject>(FEED_RESPONSE_LIMIT)
            parseSearchResponse(response).filterIsInstance<ArtistItem>().distinctBy { it.id }
        }
}
