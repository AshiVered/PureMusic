/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.puremusic

import android.content.Context
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.constants.PureMusicArtistFilterEnabledKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ApprovedArtistsState(
    val filterEnabled: Boolean = false,
    val approvedIds: Set<String> = emptySet(),
    val approvedNames: Set<String> = emptySet(),
)

@Singleton
class ArtistFilterRepository @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) {
    val isFilterEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PureMusicArtistFilterEnabledKey] ?: false }

    /** Triple<filterEnabled, approvedArtistIds, approvedArtistNames> — reactive, for ViewModels */
    val approvedArtistIdsFlow: Flow<Triple<Boolean, Set<String>, Set<String>>> =
        combine(isFilterEnabled, database.approvedArtists()) { enabled, artists ->
            Triple(
                enabled,
                artists.map { it.artistId }.toSet(),
                artists.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
            )
        }

    suspend fun isFilterCurrentlyEnabled(): Boolean =
        isFilterEnabled.first()

    suspend fun filterYTItems(items: List<YTItem>): List<YTItem> {
        if (!isFilterCurrentlyEnabled()) return items
        val approved = database.approvedArtists().first()
        val approvedIds = approved.map { it.artistId }.toSet()
        val approvedNames = approved.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        return items.filter { item ->
            when (item) {
                is SongItem -> item.artists.isNotEmpty() && item.artists.any {
                    (it.id != null && it.id in approvedIds) ||
                    (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
                }
                is AlbumItem -> item.artists.orEmpty().isNotEmpty() && item.artists.orEmpty().any {
                    (it.id != null && it.id in approvedIds) ||
                    (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
                }
                is ArtistItem -> item.id in approvedIds || item.title.trim().lowercase() in approvedNames
                else -> false
            }
        }
    }

    /**
     * PureMusic: In search results, when filtering is enabled, show ONLY albums and songs by approved artists.
     */
    suspend fun filterSearchYTItems(items: List<YTItem>): List<YTItem> {
        val candidates = items.filter { it is SongItem || it is AlbumItem }
        if (!isFilterCurrentlyEnabled()) return candidates
        val approved = database.approvedArtists().first()
        val approvedIds = approved.map { it.artistId }.toSet()
        val approvedNames = approved.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        return candidates.filter { item ->
            when (item) {
                is SongItem -> item.artists.isNotEmpty() && item.artists.any {
                    (it.id != null && it.id in approvedIds) ||
                    (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
                }
                is AlbumItem -> item.artists.orEmpty().isNotEmpty() && item.artists.orEmpty().any {
                    (it.id != null && it.id in approvedIds) ||
                    (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
                }
                else -> false
            }
        }
    }

    suspend fun filterSongs(songs: List<Song>): List<Song> {
        if (!isFilterCurrentlyEnabled()) return songs
        val approved = database.approvedArtists().first()
        val approvedIds = approved.map { it.artistId }.toSet()
        val approvedNames = approved.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        return songs.filter { song ->
            song.artists.isNotEmpty() && song.artists.any {
                it.id in approvedIds || it.name.trim().lowercase() in approvedNames
            }
        }
    }

    suspend fun filterSongItems(songs: List<SongItem>): List<SongItem> {
        if (!isFilterCurrentlyEnabled()) return songs
        val approved = database.approvedArtists().first()
        val approvedIds = approved.map { it.artistId }.toSet()
        val approvedNames = approved.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        return songs.filter { song ->
            song.artists.isNotEmpty() && song.artists.any {
                (it.id != null && it.id in approvedIds) ||
                (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
            }
        }
    }

    suspend fun isSongItemAllowed(song: SongItem): Boolean {
        if (!isFilterCurrentlyEnabled()) return true
        val approved = database.approvedArtists().first()
        val approvedIds = approved.map { it.artistId }.toSet()
        val approvedNames = approved.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        return song.artists.isNotEmpty() && song.artists.any {
            (it.id != null && it.id in approvedIds) ||
            (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
        }
    }

    suspend fun isArtistAllowed(artistId: String): Boolean {
        if (!isFilterCurrentlyEnabled()) return true
        return database.isArtistApproved(artistId)
    }
}
