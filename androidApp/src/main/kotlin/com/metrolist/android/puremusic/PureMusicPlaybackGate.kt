/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import android.content.Context
import com.metrolist.android.playback.AndroidPlaybackPlayer
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.puremusic.PureMusicPolicy
import com.metrolist.shared.settings.BlockedContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Last line of defence in the player service. Queues are already filtered through BlockedContent; this
 * catches anything that reaches the player another way (remote control, restored state, a song whose
 * artist was removed from the approved list while it plays).
 */
internal object PureMusicPlaybackGate {
    fun attach(
        scope: CoroutineScope,
        @Suppress("UNUSED_PARAMETER") context: Context,
        player: AndroidPlaybackPlayer,
    ) {
        scope.launch {
            combine(player.currentSong, BlockedContent.version) { song, _ -> song }.collect { song ->
                if (song != null && PureMusicPolicy.restricts(song)) {
                    player.stop()
                    PureMusic.notifyContentBlocked()
                }
            }
        }
    }

    /** Called when a play request is filtered; tells the user if PureMusic removed the requested song. */
    fun onQueueFiltered(requested: SongItem?) {
        if (requested != null && PureMusicPolicy.restricts(requested)) PureMusic.notifyContentBlocked()
    }
}
