/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.metrolist.music.puremusic.ArtistFilterRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val artistFilterRepository: ArtistFilterRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    val result =
        combine(
            combine(
                query,
                filter,
                context.dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged()
            ) { q, f, hideVideoSongs ->
                Triple(q, f, hideVideoSongs)
            }.flatMapLatest { (q, f, hideVideoSongs) ->
                if (q.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    when (f) {
                        LocalFilter.ALL ->
                            combine(
                                database.searchSongs(q, PREVIEW_SIZE),
                                database.searchAlbums(q, PREVIEW_SIZE),
                                database.searchArtists(q, PREVIEW_SIZE),
                                database.searchPlaylists(q, PREVIEW_SIZE),
                            ) { songs, albums, artists, playlists ->
                                val filteredSongs = if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                                filteredSongs + albums + artists + playlists
                            }

                        LocalFilter.SONG -> database.searchSongs(q).map { songs ->
                            if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                        }
                        LocalFilter.ALBUM -> database.searchAlbums(q)
                        LocalFilter.ARTIST -> database.searchArtists(q)
                        LocalFilter.PLAYLIST -> database.searchPlaylists(q)
                    }
                }
            },
            query,
            filter,
            artistFilterRepository.approvedArtistIdsFlow,
        ) { list, currentQuery, currentFilter, (filterEnabled, approvedIds, approvedNames) ->
            val filteredList = if (filterEnabled) {
                list.filter { item ->
                    when (item) {
                        is Song -> item.artists.isNotEmpty() && item.artists.any {
                            it.id in approvedIds || it.name.trim().lowercase() in approvedNames
                        }
                        is Album -> item.artists.isNotEmpty() && item.artists.any {
                            it.id in approvedIds || it.name.trim().lowercase() in approvedNames
                        }
                        is Artist -> item.id in approvedIds || item.artist.name.trim().lowercase() in approvedNames
                        else -> true
                    }
                }
            } else list

            LocalSearchResult(
                query = currentQuery,
                filter = currentFilter,
                map = filteredList.groupBy {
                    when (it) {
                        is Song -> LocalFilter.SONG
                        is Album -> LocalFilter.ALBUM
                        is Artist -> LocalFilter.ARTIST
                        is Playlist -> LocalFilter.PLAYLIST
                    }
                },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    companion object {
        const val PREVIEW_SIZE = 3
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
