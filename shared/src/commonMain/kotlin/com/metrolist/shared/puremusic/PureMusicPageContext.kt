/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.EpisodeItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.YTItem

/**
 * Some YouTube pages belong to one artist or channel but list items without saying so ("Single • 2026",
 * episodes without author). PureMusic matches content by artist, so such items would all be hidden even for
 * approved artists. On those pages the owner is attached from the page id, like Metrolist already does for
 * albums on artist pages.
 */
object PureMusicPageContext {
    private const val DISCOGRAPHY_PREFIX = "MPAD"

    /** Items on a channel's own page: episodes and podcasts without an owner belong to the channel. */
    fun attribute(
        item: YTItem,
        channelId: String,
        channelName: String,
    ): YTItem =
        when (item) {
            is EpisodeItem -> if (item.author == null) item.copy(author = ArtistTiny(channelId, channelName)) else item
            is PodcastItem -> if (item.channelId.isNullOrBlank()) item.copy(channelId = channelId) else item
            else -> item
        }

    /** "See all" pages of an artist's albums/singles (browse id MPAD<channel>): albums belong to that artist. */
    fun attributeBrowse(
        browseId: String,
        sections: List<HomeSection>,
    ): List<HomeSection> {
        if (!browseId.startsWith(DISCOGRAPHY_PREFIX)) return sections
        val channelId = browseId.removePrefix(DISCOGRAPHY_PREFIX)
        val name = PureMusicSettings.approvedArtists().firstOrNull { it.id == channelId }?.name.orEmpty()
        val owner = listOf(ArtistTiny(channelId, name))
        return sections.map { section ->
            section.copy(items = section.items.map { if (it is AlbumItem && it.artists.isEmpty()) it.copy(artists = owner) else it })
        }
    }
}
