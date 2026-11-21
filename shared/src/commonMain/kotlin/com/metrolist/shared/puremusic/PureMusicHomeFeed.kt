/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.settings.BlockedContent
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The PureMusic home page (as in PureMusic 0.x): albums by the approved artists, collected from each
 * artist's page and from an album search that must match the artist, shuffled.
 */
object PureMusicHomeFeed {
    data class State(
        val albums: List<AlbumItem> = emptyList(),
        val isLoading: Boolean = false,
        val loadedFor: String? = null,
    )

    // Sections on an artist page that hold other artists' releases.
    private val excludedSectionWords =
        listOf("featured", "appears", "מופיע", "similar", "דומים", "like", "אוהבים", "תאהבו", "other", "more from", "עוד מאת")

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    /** Loads once per approved-artist list; [force] reloads (pull to refresh). */
    fun ensureLoaded(
        scope: CoroutineScope,
        force: Boolean = false,
    ) {
        val source = PureMusicSettings.approvedArtistsJson
        val current = _state.value
        if (!force && (current.loadedFor == source || job?.isActive == true)) return
        job?.cancel()
        _state.value = current.copy(isLoading = true)
        job =
            scope.launch {
                val albums =
                    try {
                        load()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        current.albums
                    }
                _state.value = State(albums = albums, isLoading = false, loadedFor = source)
            }
    }

    private suspend fun load(): List<AlbumItem> =
        coroutineScope {
            PureMusicSettings
                .approvedArtists()
                .map { artist -> async { fromArtistPage(artist) + fromSearch(artist) } }
                .awaitAll()
                .flatten()
                .filterNot(BlockedContent::isBlocked)
                .distinctBy { it.id }
                .shuffled()
        }

    private suspend fun fromArtistPage(artist: ApprovedArtist): List<AlbumItem> =
        try {
            GlobalYouTubeRepository.instance
                .getArtist(artist.id)
                .sections
                .filterKeys { title -> title != "header" && excludedSectionWords.none { it in title.lowercase() } }
                .values
                .flatten()
                .filterIsInstance<AlbumItem>()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

    private suspend fun fromSearch(artist: ApprovedArtist): List<AlbumItem> =
        try {
            val name = artist.name.trim()
            GlobalYouTubeRepository.instance
                .searchFiltered(name, SearchFilter.ALBUM)
                .items
                .filterIsInstance<AlbumItem>()
                .filter { album -> album.artists.any { it.id == artist.id || it.name.trim().equals(name, ignoreCase = true) } }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
}
