/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.EpisodeItem
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.model.isPodcast
import com.metrolist.shared.settings.BlockedContent
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * The single place that decides what PureMusic allows. Metrolist routes every list, page, queue and
 * Android Auto browse through [BlockedContent]; PureMusic hooks into it (see `PUREMUSIC-HOOK` markers).
 *
 * Approved sources are artists and channels (matched by id or name on the item's artists) and podcasts
 * (matched by podcast id, or by the publishing channel for episodes).
 */
object PureMusicPolicy {
    private class Approved(
        val source: String,
        val ids: Set<String>,
        val names: Set<String>,
        val podcastIds: Set<String>,
        val podcastChannelIds: Set<String>,
    )

    private val lock = SynchronizedObject()
    private var cached: Approved? = null

    private fun approved(): Approved {
        val source = PureMusicSettings.approvedArtistsJson
        return synchronized(lock) {
            cached?.takeIf { it.source == source } ?: PureMusicSettings.approvedArtists().let { all ->
                val (podcasts, artists) = all.partition { it.kind == ApprovedArtist.KIND_PODCAST }
                Approved(
                    source = source,
                    ids = artists.mapTo(mutableSetOf()) { it.id },
                    names = artists.map { it.name.normalized() }.filterTo(mutableSetOf(), String::isNotBlank),
                    podcastIds = podcasts.mapTo(mutableSetOf()) { it.id.podcastKey() },
                    podcastChannelIds = podcasts.mapNotNullTo(mutableSetOf()) { it.channelId?.takeIf(String::isNotBlank) },
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

    /** True when the podcast itself, or the channel that publishes it, was approved as a podcast. */
    fun isPodcastApproved(
        podcastId: String?,
        channelId: String? = null,
    ): Boolean {
        if (isLocked) return false
        val approved = approved()
        if (!podcastId.isNullOrBlank() && podcastId.podcastKey() in approved.podcastIds) return true
        return !channelId.isNullOrBlank() && channelId in approved.podcastChannelIds
    }

    // Podcast id -> publishing channel, learned from podcast items seen in lists, so a podcast page opened
    // from a list can be matched to an approved channel (the page itself only knows the podcast id).
    private val podcastChannels = mutableMapOf<String, String>()

    /** Podcast pages: allowed when the podcast, or the channel that publishes it, is approved. */
    fun isPodcastPageAllowed(podcastId: String): Boolean {
        if (!isArtistFilterActive) return true
        if (isPodcastApproved(podcastId)) return true
        val channel = synchronized(lock) { podcastChannels[podcastId.podcastKey()] }
        return channel != null && isArtistApproved(channel, null)
    }

    /** Records which channel publishes a podcast (used by podcast pages opened from links). */
    fun rememberPodcastChannel(item: PodcastItem) {
        val channel = item.channelId?.takeIf(String::isNotBlank) ?: return
        synchronized(lock) { podcastChannels[item.id.podcastKey()] = channel }
    }

    private fun List<ArtistTiny>.anyApproved(): Boolean = any { isArtistApproved(it.id, it.name) }

    private fun SongItem.isApprovedEpisode(): Boolean =
        isPodcast && (isPodcastApproved(podcastId) || artists.any { isPodcastApproved(null, it.id) })

    /** True when PureMusic hides this song: none of its artists is approved and it is no approved podcast's episode. */
    fun restricts(song: SongItem): Boolean = isArtistFilterActive && !song.artists.anyApproved() && !song.isApprovedEpisode()

    /** True when PureMusic hides this item. Playlists and other containers are hidden while filtering. */
    fun restricts(item: YTItem): Boolean {
        if (!isArtistFilterActive) return false
        return when (item) {
            is SongItem -> restricts(item)
            is AlbumItem -> !item.artists.anyApproved()
            is ArtistItem -> !isArtistApproved(item.id, item.title)
            // An approved channel (or artist) also covers the podcasts and playlists it publishes.
            is PodcastItem -> {
                rememberPodcastChannel(item)
                !isPodcastApproved(item.id) && !isArtistApproved(item.channelId, null)
            }
            is PlaylistItem -> item.authorId.isNullOrBlank() || !isArtistApproved(item.authorId, null)
            is EpisodeItem -> {
                val author = item.author
                !isPodcastApproved(item.podcastId, author?.id) && (author == null || !isArtistApproved(author.id, author.name))
            }
            else -> true
        }
    }

    /** Call after the approved list or the filter switch changes so open screens reload. */
    fun notifyChanged() {
        BlockedContent.contentFiltersChanged()
    }

    private fun String.normalized(): String = trim().lowercase()

    /** Podcast ids appear both as "MPSP<playlist>" (show pages) and as the bare playlist id (episodes). */
    private fun String.podcastKey(): String = removePrefix("MPSP")
}
