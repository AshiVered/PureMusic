/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.puremusic_control_description
import com.metrolist.shared.generated.resources.puremusic_control_title
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.settings.SettingsNavigationRow
import org.jetbrains.compose.resources.stringResource

/**
 * The Control & Management row shown first on the settings home. The platform sets [onOpenControl];
 * while it is null (desktop, iOS) nothing is shown.
 */
object PureMusicSettingsHome {
    var onOpenControl: (() -> Unit)? = null

    @Composable
    fun Entry(horizontalPadding: Dp = 16.dp) {
        val open = onOpenControl ?: return
        SettingsNavigationRow(
            title = stringResource(Res.string.puremusic_control_title),
            subtitle = stringResource(Res.string.puremusic_control_description),
            icon = MetrolistIcons.Outlined.Shield,
            first = true,
            last = true,
            onClick = open,
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )
    }
}
