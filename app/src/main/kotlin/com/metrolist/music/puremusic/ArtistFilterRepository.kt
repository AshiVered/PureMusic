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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistFilterRepository @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) {
    val isFilterEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[PureMusicArtistFilterEnabledKey] ?: false }

    suspend fun isFilterCurrentlyEnabled(): Boolean =
        isFilterEnabled.first()

    suspend fun filterYTItems(items: List<YTItem>): List<YTItem> {
        if (!isFilterCurrentlyEnabled()) return items
        val approvedIds = database.approvedArtistIds().toSet()
        return items.filter { item ->
            when (item) {
                is SongItem -> item.artists.any { it.id != null && it.id in approvedIds }
                is AlbumItem -> item.artists.orEmpty().any { it.id != null && it.id in approvedIds }
                is ArtistItem -> item.id in approvedIds
                else -> true
            }
        }
    }

    suspend fun filterSongs(songs: List<Song>): List<Song> {
        if (!isFilterCurrentlyEnabled()) return songs
        val approvedIds = database.approvedArtistIds().toSet()
        return songs.filter { song -> song.artists.any { it.id in approvedIds } }
    }

    suspend fun filterSongItems(songs: List<SongItem>): List<SongItem> {
        if (!isFilterCurrentlyEnabled()) return songs
        val approvedIds = database.approvedArtistIds().toSet()
        return songs.filter { song ->
            song.artists.any { it.id != null && it.id in approvedIds }
        }
    }

    suspend fun isSongItemAllowed(song: SongItem): Boolean {
        if (!isFilterCurrentlyEnabled()) return true
        val approvedIds = database.approvedArtistIds().toSet()
        return song.artists.any { it.id != null && it.id in approvedIds }
    }

    suspend fun isArtistAllowed(artistId: String): Boolean {
        if (!isFilterCurrentlyEnabled()) return true
        return database.isArtistApproved(artistId)
    }
}

