/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import androidx.compose.runtime.snapshotFlow
import com.metrolist.shared.settings.BlockedContent
import com.metrolist.shared.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps Metrolist's own feature switches off while PureMusic disallows the feature. Metrolist already
 * stops each feature when its switch turns off, so forcing the switch is enough and needs no hooks in
 * the feature code. Any attempt to turn a switch back on (settings screen, backup restore) is reverted.
 */
object PureMusicEnforcer {
    private const val NO_CANVAS_SOURCES = "NONE"

    private fun Settings.ruleValue(rule: PureMusicContentRule): Boolean =
        when (rule) {
            PureMusicContentRule.HIDE_EXPLICIT -> hideExplicit
            PureMusicContentRule.HIDE_VIDEO_SONGS -> hideVideoSongs
            PureMusicContentRule.HIDE_NON_MUSICAL_VIDEOS -> hideNonMusicalVideos
        }

    private fun Settings.setRuleValue(
        rule: PureMusicContentRule,
        value: Boolean,
    ) {
        when (rule) {
            PureMusicContentRule.HIDE_EXPLICIT -> hideExplicit = value
            PureMusicContentRule.HIDE_VIDEO_SONGS -> hideVideoSongs = value
            PureMusicContentRule.HIDE_NON_MUSICAL_VIDEOS -> hideNonMusicalVideos = value
        }
    }

    private val PureMusicContentRule.isVideoRule: Boolean
        get() = this == PureMusicContentRule.HIDE_VIDEO_SONGS || this == PureMusicContentRule.HIDE_NON_MUSICAL_VIDEOS

    /** Videos the parent chose to hide are shown instead, but only their sound plays. */
    private val audioOnlyVideos: Boolean
        get() =
            PureMusicSettings.videosAsAudio &&
                PureMusicContentRule.entries.any { it.isVideoRule && PureMusicSettings.isContentRuleOn(it) }

    fun enforceNow() {
        val s = Settings.shared
        var contentChanged = false
        PureMusicContentRule.entries.forEach { rule ->
            // First run (also PureMusic 0.x upgrades): adopt the value already in use.
            if (!PureMusicSettings.hasContentRule(rule)) PureMusicSettings.setContentRule(rule, s.ruleValue(rule))
            val wanted = PureMusicSettings.isContentRuleOn(rule) && !(rule.isVideoRule && audioOnlyVideos)
            if (s.ruleValue(rule) != wanted) {
                s.setRuleValue(rule, wanted)
                contentChanged = true
            }
        }
        if (contentChanged) BlockedContent.contentFiltersChanged()
        if (!PureMusicPolicy.isFeatureAllowed(PureMusicFeature.VIDEO) || audioOnlyVideos) {
            if (s.musicVideoPlaybackEnabled) s.musicVideoPlaybackEnabled = false
            if (s.videoViewerVisible) s.videoViewerVisible = false
        }
        if (!PureMusicPolicy.isFeatureAllowed(PureMusicFeature.CANVAS)) {
            if (s.playerCanvasEnabled) s.playerCanvasEnabled = false
            if (s.enableAnimatedCovers) s.enableAnimatedCovers = false
            if (s.enableAppleMusicArtistVideos) s.enableAppleMusicArtistVideos = false
            if (s.canvasEnabledSources != NO_CANVAS_SOURCES) s.canvasEnabledSources = NO_CANVAS_SOURCES
        } else if (s.canvasEnabledSources == NO_CANVAS_SOURCES) {
            s.canvasEnabledSources = ""
        }
        if (!PureMusicPolicy.isFeatureAllowed(PureMusicFeature.LISTEN_TOGETHER)) {
            if (s.listenTogetherEnabled) s.listenTogetherEnabled = false
        }
        if (!PureMusicPolicy.isFeatureAllowed(PureMusicFeature.CAST)) {
            if (s.castEnabled) s.castEnabled = false
            if (s.deviceConnectEnabled) s.deviceConnectEnabled = false
        }
        // Crash reports go to the Metrolist developers; PureMusic never sends them.
        if (s.sentryEnabled) s.sentryEnabled = false
    }

    /** Re-applies [enforceNow] whenever any watched switch changes. */
    fun start(scope: CoroutineScope) {
        enforceNow()
        scope.launch {
            snapshotFlow {
                val s = Settings.shared
                listOf(
                    PureMusicPolicy.isLocked,
                    PureMusicFeature.entries.map(PureMusicSettings::isFeatureAllowed),
                    PureMusicContentRule.entries.map(PureMusicSettings::isContentRuleOn),
                    PureMusicSettings.videosAsAudio,
                    s.hideExplicit,
                    s.hideVideoSongs,
                    s.hideNonMusicalVideos,
                    s.musicVideoPlaybackEnabled,
                    s.videoViewerVisible,
                    s.playerCanvasEnabled,
                    s.enableAnimatedCovers,
                    s.enableAppleMusicArtistVideos,
                    s.canvasEnabledSources,
                    s.listenTogetherEnabled,
                    s.castEnabled,
                    s.deviceConnectEnabled,
                    s.sentryEnabled,
                )
            }.collect { enforceNow() }
        }
    }
}
