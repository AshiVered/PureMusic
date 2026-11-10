/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.HistoryPage
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HistorySource
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.puremusic.ArtistFilterRepository
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    private val artistFilterRepository: ArtistFilterRepository,
) : ViewModel() {
    var historySource = MutableStateFlow(HistorySource.LOCAL)

    private val today = LocalDate.now()
    private val thisMonday = today.with(DayOfWeek.MONDAY)
    private val lastMonday = thisMonday.minusDays(7)

    private val rawHistoryPage = MutableStateFlow<HistoryPage?>(null)
    val historyPage: StateFlow<HistoryPage?> =
        combine(rawHistoryPage, artistFilterRepository.approvedArtistIdsFlow) { page, (filterEnabled, approvedIds, approvedNames) ->
            val sections = page?.sections
            if (!filterEnabled || sections == null) page
            else {
                val filteredSections = sections.mapNotNull { section ->
                    val filteredSongs = section.songs.filter { song ->
                        song.artists.isNotEmpty() && song.artists.any {
                            (it.id != null && it.id in approvedIds) ||
                            (it.name.isNotBlank() && it.name.trim().lowercase() in approvedNames)
                        }
                    }
                    if (filteredSongs.isEmpty()) null
                    else section.copy(songs = filteredSongs)
                }
                page.copy(sections = filteredSections)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val events =
        combine(
            context.dataStore.data
                .map { it[HideVideoSongsKey] ?: false }
                .distinctUntilChanged()
                .flatMapLatest { hideVideoSongs ->
                    database
                        .historyEvents(
                            tomorrowStart = today.plusDays(1).atStartOfDay(),
                            todayStart = today.atStartOfDay(),
                            yesterdayStart = today.minusDays(1).atStartOfDay(),
                            thisMondayStart = thisMonday.atStartOfDay(),
                            lastMondayStart = lastMonday.atStartOfDay(),
                        )
                        .map { events ->
                            events.filter { !hideVideoSongs || !it.song.song.isVideo }
                        }
                },
            artistFilterRepository.approvedArtistIdsFlow,
        ) { rawEvents, (filterEnabled, approvedIds, approvedNames) ->
            val filteredEvents = if (filterEnabled) {
                rawEvents.filter { event ->
                    event.song.artists.isNotEmpty() && event.song.artists.any {
                        it.id in approvedIds || it.name.trim().lowercase() in approvedNames
                    }
                }
            } else rawEvents

            filteredEvents
                .groupBy {
                    val date = it.event.timestamp.toLocalDate()
                    val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
                    when {
                        daysAgo == 0 -> DateAgo.Today
                        daysAgo == 1 -> DateAgo.Yesterday
                        date >= thisMonday -> DateAgo.ThisWeek
                        date >= lastMonday -> DateAgo.LastWeek
                        else -> DateAgo.Other(date.withDayOfMonth(1))
                    }
                }.toSortedMap(
                    compareBy { dateAgo ->
                        when (dateAgo) {
                            DateAgo.Today -> 0L
                            DateAgo.Yesterday -> 1L
                            DateAgo.ThisWeek -> 2L
                            DateAgo.LastWeek -> 3L
                            is DateAgo.Other -> ChronoUnit.DAYS.between(dateAgo.date, today)
                        }
                    },
                ).mapValues { entry ->
                    entry.value.distinctBy { it.song.id }
                }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        fetchRemoteHistory()
    }

    fun fetchRemoteHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.musicHistory().onSuccess {
                rawHistoryPage.value = it
            }.onFailure {
                reportException(it)
            }
        }
    }
}

sealed class DateAgo {
    data object Today : DateAgo()

    data object Yesterday : DateAgo()

    data object ThisWeek : DateAgo()

    data object LastWeek : DateAgo()

    class Other(
        val date: LocalDate,
    ) : DateAgo() {
        override fun equals(other: Any?): Boolean {
            if (other is Other) return date == other.date
            return super.equals(other)
        }

        override fun hashCode(): Int = date.hashCode()
    }
}
