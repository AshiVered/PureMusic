/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.metrolist.android.puremusic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.metrolist.android.ui.theme.MetrolistTheme
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.puremusic_approved_artists
import com.metrolist.shared.generated.resources.puremusic_back
import com.metrolist.shared.generated.resources.puremusic_change_password
import com.metrolist.shared.generated.resources.puremusic_control_title
import com.metrolist.shared.generated.resources.puremusic_enter_password
import com.metrolist.shared.generated.resources.puremusic_password
import com.metrolist.shared.generated.resources.puremusic_setup_description
import com.metrolist.shared.generated.resources.puremusic_setup_title
import com.metrolist.shared.generated.resources.puremusic_unlock
import com.metrolist.shared.generated.resources.puremusic_wrong_password
import com.metrolist.shared.puremusic.PureMusicSettings
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

/**
 * Password-protected Control & Management screen. It is a separate activity so it needs no changes in
 * Metrolist's navigation. Authentication is never remembered: leaving the screen closes it.
 */
class PureMusicControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MetrolistTheme {
                ControlRoot(onClose = ::finish)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PureMusicControlActivity::class.java)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

private enum class Page { MAIN, ARTISTS, CHANGE_PASSWORD }

@Composable
private fun ControlRoot(onClose: () -> Unit) {
    var authenticated by rememberSaveable { mutableStateOf(false) }
    when {
        !PureMusicSettings.hasPassword -> {
            ScreenScaffold(stringResource(Res.string.puremusic_setup_title), onClose) { padding ->
                FormColumn(padding) {
                    Text(stringResource(Res.string.puremusic_setup_description))
                    PasswordSetupForm(onSaved = { authenticated = true })
                }
            }
        }

        !authenticated -> {
            LoginScreen(onClose = onClose, onSuccess = { authenticated = true })
        }

        else -> {
            var page by rememberSaveable { mutableStateOf(Page.MAIN) }
            BackHandler(enabled = page != Page.MAIN) { page = Page.MAIN }
            when (page) {
                Page.MAIN -> {
                    ScreenScaffold(stringResource(Res.string.puremusic_control_title), onClose) { padding ->
                        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                            PureMusicSettingsList(onOpenArtists = { page = Page.ARTISTS })
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                            TextButton(onClick = { page = Page.CHANGE_PASSWORD }, modifier = Modifier.padding(8.dp)) {
                                Text(stringResource(Res.string.puremusic_change_password))
                            }
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }

                Page.ARTISTS -> {
                    ScreenScaffold(stringResource(Res.string.puremusic_approved_artists), { page = Page.MAIN }) { padding ->
                        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
                            ApprovedArtistsManager()
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }

                Page.CHANGE_PASSWORD -> {
                    ScreenScaffold(stringResource(Res.string.puremusic_change_password), { page = Page.MAIN }) { padding ->
                        FormColumn(padding) {
                            PasswordSetupForm(onSaved = { page = Page.MAIN }, newPasswordLabel = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormColumn(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(padding).padding(24.dp).fillMaxWidth().widthIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
private fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MetrolistIcons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.puremusic_back))
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
private fun LoginScreen(
    onClose: () -> Unit,
    onSuccess: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val submit = {
        if (PureMusicSettings.verifyPassword(password)) {
            onSuccess()
        } else {
            wrong = true
            password = ""
        }
    }
    ScreenScaffold(stringResource(Res.string.puremusic_control_title), onClose) { padding ->
        FormColumn(padding) {
            Text(stringResource(Res.string.puremusic_enter_password))
            PasswordField(password, { password = it; wrong = false }, stringResource(Res.string.puremusic_password), ImeAction.Done, submit)
            if (wrong) Text(stringResource(Res.string.puremusic_wrong_password), color = MaterialTheme.colorScheme.error)
            Button(onClick = submit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.puremusic_unlock)) }
        }
    }
}
