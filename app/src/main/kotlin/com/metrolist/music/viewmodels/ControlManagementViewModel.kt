/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.constants.PureMusicArtistFilterEnabledKey
import com.metrolist.music.constants.PureMusicImagesEnabledKey
import com.metrolist.music.constants.PureMusicPasswordHashKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ApprovedArtist
import com.metrolist.music.puremusic.PasswordUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlManagementViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    // Password
    val passwordHash: StateFlow<String?> = context.dataStore.data
        .map { it[PureMusicPasswordHashKey] }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            context.dataStore[PureMusicPasswordHashKey],
        )

    val isPasswordSet: Boolean get() = passwordHash.value != null

    suspend fun setPassword(newPassword: String) {
        context.safeDataStoreEdit { prefs ->
            prefs[PureMusicPasswordHashKey] = PasswordUtils.hashPassword(newPassword)
        }
    }

    fun verifyPassword(input: String): Boolean {
        val hash = passwordHash.value ?: return false
        return PasswordUtils.verifyPassword(input, hash)
    }

    // Images
    val imagesEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[PureMusicImagesEnabledKey] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setImagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.safeDataStoreEdit { it[PureMusicImagesEnabledKey] = enabled }
        }
    }

    // Artist filter
    val artistFilterEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[PureMusicArtistFilterEnabledKey] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setArtistFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.safeDataStoreEdit { it[PureMusicArtistFilterEnabledKey] = enabled }
        }
    }

    // Approved artists
    val approvedArtists: StateFlow<List<ApprovedArtist>> =
        database.approvedArtists()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addArtist(artist: ApprovedArtist) {
        viewModelScope.launch(Dispatchers.IO) {
            database.insertApprovedArtist(artist)
        }
    }

    fun removeArtist(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.deleteApprovedArtist(artistId)
        }
    }

    // Artist search
    val artistSearchResults = MutableStateFlow<List<ArtistItem>>(emptyList())
    val isSearching = MutableStateFlow(false)

    fun searchArtists(query: String) {
        if (query.isBlank()) {
            artistSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            isSearching.value = true
            YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST)
                .onSuccess { result ->
                    artistSearchResults.value = result.items
                        .filterIsInstance<ArtistItem>()
                }
                .onFailure { artistSearchResults.value = emptyList() }
            isSearching.value = false
        }
    }
}

