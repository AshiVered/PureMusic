/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.puremusic_back
import com.metrolist.shared.generated.resources.puremusic_continue
import com.metrolist.shared.generated.resources.puremusic_onboarding_description
import com.metrolist.shared.generated.resources.puremusic_onboarding_need_password
import com.metrolist.shared.generated.resources.puremusic_onboarding_title
import com.metrolist.shared.generated.resources.onboarding_step_of
import com.metrolist.shared.generated.resources.puremusic_cancel
import com.metrolist.shared.generated.resources.puremusic_skip
import com.metrolist.shared.generated.resources.puremusic_skip_confirm
import com.metrolist.shared.generated.resources.puremusic_skip_message
import com.metrolist.shared.generated.resources.puremusic_skip_title
import com.metrolist.shared.puremusic.PureMusicContentRule
import com.metrolist.shared.puremusic.PureMusicFeature
import com.metrolist.shared.puremusic.PureMusicPolicy
import com.metrolist.shared.puremusic.PureMusicSettings
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

/**
 * The content-filtering step of the first-run wizard, shown right after Metrolist's welcome page. It is
 * drawn by PureMusic instead of being a Metrolist onboarding page, so Metrolist's page list stays as is.
 */
object PureMusicOnboarding {
    /**
     * Whether this wizard run includes the step. Decided once per process: upgrades from PureMusic 0.x
     * already have a password and settings, so they never see it.
     */
    private val included: Boolean by lazy { !PureMusicSettings.hasPassword }

    /** The step sits right after the welcome page (position 2). */
    fun step(pageIndex: Int): Int = if (included && pageIndex >= 1) pageIndex + 2 else pageIndex + 1

    fun stepCount(pageCount: Int): Int = if (included) pageCount + 1 else pageCount

    /** Returns true while the step is on screen; the onboarding screen then draws nothing else. */
    @Composable
    fun Step(
        isWelcomePage: Boolean,
        pageCount: Int,
        onBack: () -> Unit,
    ): Boolean {
        var pending by rememberSaveable { mutableStateOf(included) }
        if (!pending || isWelcomePage) return false
        StepScreen(stepCount = stepCount(pageCount), onBack = onBack, onDone = { pending = false })
        return true
    }

    /** Skipping means no filtering at all: everything shown, every feature allowed. */
    private fun disableAllFiltering() {
        PureMusicSettings.artistFilterEnabled = false
        PureMusicSettings.imagesEnabled = true
        PureMusicFeature.entries.forEach { PureMusicSettings.setFeatureAllowed(it, true) }
        PureMusicContentRule.entries.forEach { PureMusicSettings.setContentRule(it, false) }
        PureMusicSettings.videosAsAudio = false
        PureMusicPolicy.notifyChanged()
    }

    @Composable
    private fun StepScreen(
        stepCount: Int,
        onBack: () -> Unit,
        onDone: () -> Unit,
    ) {
        BackHandler(onBack = onBack)
        var showPasswordError by rememberSaveable { mutableStateOf(false) }
        var confirmSkip by rememberSaveable { mutableStateOf(false) }
        if (confirmSkip) {
            AlertDialog(
                onDismissRequest = { confirmSkip = false },
                title = { Text(stringResource(Res.string.puremusic_skip_title)) },
                text = { Text(stringResource(Res.string.puremusic_skip_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmSkip = false
                        disableAllFiltering()
                        onDone()
                    }) { Text(stringResource(Res.string.puremusic_skip_confirm)) }
                },
                dismissButton = { TextButton(onClick = { confirmSkip = false }) { Text(stringResource(Res.string.puremusic_cancel)) } },
            )
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(MetrolistIcons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text(
                            stringResource(Res.string.onboarding_step_of, 2, stepCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(stringResource(Res.string.puremusic_onboarding_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(Res.string.puremusic_onboarding_description), style = MaterialTheme.typography.bodyLarge)
                        if (!PureMusicSettings.hasPassword) {
                            PasswordSetupForm(onSaved = { showPasswordError = false })
                        }
                    }
                    if (PureMusicSettings.hasPassword) PureMusicSettingsList(onOpenArtists = null)
                    if (showPasswordError) {
                        Text(
                            stringResource(Res.string.puremusic_onboarding_need_password),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onBack) { Text(stringResource(Res.string.puremusic_back)) }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { confirmSkip = true }) { Text(stringResource(Res.string.puremusic_skip)) }
                        Button(onClick = {
                            if (PureMusicSettings.hasPassword) onDone() else showPasswordError = true
                        }) { Text(stringResource(Res.string.puremusic_continue)) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
