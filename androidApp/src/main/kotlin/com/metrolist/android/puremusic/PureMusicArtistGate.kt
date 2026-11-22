/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.metrolist.shared.puremusic.PureMusicPolicy
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

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

/**
 * Podcast pages of podcasts that are not approved are not opened at all. A podcast published by an
 * approved channel is allowed; when the app has not seen that podcast in a list yet (opened from a link),
 * its channel is looked up first while a loading indicator is shown.
 */
object PureMusicPodcastGate {
    @Composable
    fun blocks(
        podcastId: String,
        onBack: () -> Unit,
    ): Boolean {
        if (PureMusicPolicy.isPodcastPageAllowed(podcastId)) return false
        var allowed by remember(podcastId) { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(podcastId) {
            val podcast =
                try {
                    withContext(Dispatchers.IO) { GlobalYouTubeRepository.instance.getPodcastPages(podcastId).firstOrNull()?.podcast }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            podcast?.let(PureMusicPolicy::rememberPodcastChannel)
            val ok = PureMusicPolicy.isPodcastPageAllowed(podcastId)
            allowed = ok
            if (!ok) {
                PureMusic.notifyContentBlocked()
                onBack()
            }
        }
        if (allowed == true) return false
        if (allowed == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        return true
    }
}
