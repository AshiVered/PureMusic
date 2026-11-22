/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.EpisodeItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.YTItem

/**
 * Some YouTube pages belong to one artist or channel but list items without saying so: "Single • 2026",
 * episodes without author. PureMusic matches content by artist, so such items would all be hidden even for
 * approved artists. On those pages the owner is attached from the page id.
 *
 * An "artist" without an id is not a real artist: Metrolist only recognises the English type words, so a
 * Hebrew "סינגל" ends up as an artist name. Such entries are replaced by the page owner.
 */
object PureMusicPageContext {
    private const val DISCOGRAPHY_PREFIX = "MPAD"

    private fun List<ArtistTiny>.hasRealArtist(): Boolean = any { !it.id.isNullOrBlank() }

    private fun attribute(
        item: YTItem,
        owner: ArtistTiny,
        ownPlaylists: Boolean,
    ): YTItem =
        when (item) {
            is AlbumItem -> if (item.artists.hasRealArtist()) item else item.copy(artists = listOf(owner))
            is EpisodeItem -> if (item.author?.id.isNullOrBlank()) item.copy(author = owner) else item
            is PodcastItem -> if (item.channelId.isNullOrBlank()) item.copy(channelId = owner.id) else item
            is PlaylistItem -> if (ownPlaylists && item.authorId.isNullOrBlank()) item.copy(authorId = owner.id) else item
            else -> item
        }

    /** An artist's or channel's own page: items without a real owner belong to the page. */
    fun attributeArtistPage(
        sections: MutableMap<String, List<YTItem>>,
        channelId: String,
        channelName: String,
    ) {
        val owner = ArtistTiny(channelId, channelName)
        sections.keys.toList().forEach { title ->
            if (title == "header") return@forEach
            // Playlists are only the owner's in sections titled with the owner's name ("Playlists by …");
            // other playlist shelves ("Featured on") belong to other people.
            val ownPlaylists = channelName.isNotBlank() && channelName in title
            sections[title] = sections.getValue(title).map { attribute(it, owner, ownPlaylists) }
        }
    }

    /** "See all" pages of an artist's albums/singles (browse id MPAD<channel>): albums belong to that artist. */
    fun attributeBrowse(
        browseId: String,
        sections: List<HomeSection>,
    ): List<HomeSection> {
        if (!browseId.startsWith(DISCOGRAPHY_PREFIX)) return sections
        val channelId = browseId.removePrefix(DISCOGRAPHY_PREFIX)
        val name = PureMusicSettings.approvedArtists().firstOrNull { it.id == channelId }?.name.orEmpty()
        val owner = ArtistTiny(channelId, name)
        return sections.map { section -> section.copy(items = section.items.map { attribute(it, owner, ownPlaylists = false) }) }
    }
}
