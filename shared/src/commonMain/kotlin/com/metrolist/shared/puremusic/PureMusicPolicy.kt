/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.settings.BlockedContent
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * The single place that decides what PureMusic allows. Metrolist routes every list, page, queue and
 * Android Auto browse through [BlockedContent]; PureMusic hooks into it (see `PUREMUSIC-HOOK` markers).
 */
object PureMusicPolicy {
    private class Approved(
        val source: String,
        val ids: Set<String>,
        val names: Set<String>,
    )

    private val lock = SynchronizedObject()
    private var cached: Approved? = null

    private fun approved(): Approved {
        val source = PureMusicSettings.approvedArtistsJson
        return synchronized(lock) {
            cached?.takeIf { it.source == source } ?: PureMusicSettings.approvedArtists().let { artists ->
                Approved(
                    source,
                    artists.mapTo(mutableSetOf()) { it.id },
                    artists.map { it.name.normalized() }.filterTo(mutableSetOf(), String::isNotBlank),
                )
            }.also { cached = it }
        }
    }

    val isLocked: Boolean get() = PureMusicSettings.lockedUntilConfigured

    val isArtistFilterActive: Boolean get() = isLocked || PureMusicSettings.artistFilterEnabled

    val imagesAllowed: Boolean get() = !isLocked && PureMusicSettings.imagesEnabled

    fun isFeatureAllowed(feature: PureMusicFeature): Boolean = !isLocked && PureMusicSettings.isFeatureAllowed(feature)

    fun isArtistApproved(
        id: String?,
        name: String?,
    ): Boolean {
        if (isLocked) return false
        val approved = approved()
        if (!id.isNullOrBlank() && id in approved.ids) return true
        return !name.isNullOrBlank() && name.normalized() in approved.names
    }

    private fun List<ArtistTiny>.anyApproved(): Boolean = any { isArtistApproved(it.id, it.name) }

    /** True when PureMusic hides this song: none of its artists is approved. */
    fun restricts(song: SongItem): Boolean = isArtistFilterActive && !song.artists.anyApproved()

    /** True when PureMusic hides this item. Playlists, podcasts and other containers are hidden while filtering. */
    fun restricts(item: YTItem): Boolean {
        if (!isArtistFilterActive) return false
        return when (item) {
            is SongItem -> restricts(item)
            is AlbumItem -> !item.artists.anyApproved()
            is ArtistItem -> !isArtistApproved(item.id, item.title)
            else -> true
        }
    }

    /** Call after the approved list or the filter switch changes so open screens reload. */
    fun notifyChanged() {
        BlockedContent.contentFiltersChanged()
    }

    private fun String.normalized(): String = trim().lowercase()
}
