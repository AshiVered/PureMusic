/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.shared.ui.icons.MetrolistIcons

/**
 * What an artwork slot shows while images are hidden: the item's type icon on a plain background, the
 * same everywhere (instead of a mix of empty boxes and the app logo used as Metrolist's error image).
 */
@Composable
fun PureMusicHiddenArtwork(
    icon: ImageVector? = null,
    iconSize: Dp = 24.dp,
    iconModifier: Modifier = Modifier,
) {
    Icon(
        icon ?: MetrolistIcons.Outlined.MusicNote,
        contentDescription = null,
        modifier = Modifier.size(iconSize).then(iconModifier),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/**
 * Replaces any image while images are hidden. Small slots (lists, grids, mini player) get the music icon;
 * large areas such as player backdrops and banners stay plain so no giant icon appears.
 */
@Composable
fun PureMusicHiddenImage(modifier: Modifier) {
    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        if (side <= 200.dp) PureMusicHiddenArtwork(iconSize = (side * 0.45f).coerceIn(16.dp, 48.dp))
    }
}

/** A whole cover box (background + icon) for places that draw an image without a type icon. */
@Composable
fun PureMusicHiddenCover(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        PureMusicHiddenArtwork()
    }
}
