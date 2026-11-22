/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.metrolist.shared.puremusic.PureMusicPolicy

/** Artist pages of artists that are not approved are not opened at all (as in PureMusic 0.x). */
object PureMusicArtistGate {
    /** Returns true when the page is blocked; the caller then draws nothing and is navigated back. */
    @Composable
    fun blocks(
        artistId: String,
        onBack: () -> Unit,
    ): Boolean {
        if (!PureMusicPolicy.isArtistFilterActive || PureMusicPolicy.isArtistApproved(artistId, null)) return false
        LaunchedEffect(artistId) {
            PureMusic.notifyContentBlocked()
            onBack()
        }
        return true
    }
}

/** Podcast pages of podcasts that are not approved are not opened at all. */
object PureMusicPodcastGate {
    @Composable
    fun blocks(
        podcastId: String,
        onBack: () -> Unit,
    ): Boolean {
        if (PureMusicPolicy.isPodcastPageAllowed(podcastId)) return false
        LaunchedEffect(podcastId) {
            PureMusic.notifyContentBlocked()
            onBack()
        }
        return true
    }
}
