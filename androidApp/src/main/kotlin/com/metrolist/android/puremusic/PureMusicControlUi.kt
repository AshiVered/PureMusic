/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.puremusic_add
import com.metrolist.shared.generated.resources.puremusic_added
import com.metrolist.shared.generated.resources.puremusic_approved_artists
import com.metrolist.shared.generated.resources.puremusic_approved_artists_count
import com.metrolist.shared.generated.resources.puremusic_artist_filter
import com.metrolist.shared.generated.resources.puremusic_artist_filter_description
import com.metrolist.shared.generated.resources.puremusic_confirm_password
import com.metrolist.shared.generated.resources.puremusic_disable_canvas
import com.metrolist.shared.generated.resources.puremusic_disable_cast
import com.metrolist.shared.generated.resources.puremusic_disable_listen_together
import com.metrolist.shared.generated.resources.puremusic_disable_video
import com.metrolist.shared.generated.resources.puremusic_feature_canvas_description
import com.metrolist.shared.generated.resources.puremusic_feature_cast_description
import com.metrolist.shared.generated.resources.puremusic_feature_listen_together_description
import com.metrolist.shared.generated.resources.puremusic_feature_video_description
import com.metrolist.shared.generated.resources.puremusic_hide_explicit
import com.metrolist.shared.generated.resources.puremusic_hide_explicit_description
import com.metrolist.shared.generated.resources.puremusic_hide_non_musical
import com.metrolist.shared.generated.resources.puremusic_hide_non_musical_description
import com.metrolist.shared.generated.resources.puremusic_hide_video_songs
import com.metrolist.shared.generated.resources.puremusic_hide_video_songs_description
import com.metrolist.shared.generated.resources.puremusic_kind_artist
import com.metrolist.shared.generated.resources.puremusic_kind_channel
import com.metrolist.shared.generated.resources.puremusic_kind_podcast
import com.metrolist.shared.generated.resources.puremusic_videos_as_audio
import com.metrolist.shared.generated.resources.puremusic_videos_as_audio_description
import com.metrolist.shared.generated.resources.puremusic_locked_confirm
import com.metrolist.shared.generated.resources.puremusic_locked_description
import com.metrolist.shared.generated.resources.puremusic_locked_title
import com.metrolist.shared.generated.resources.puremusic_new_password
import com.metrolist.shared.generated.resources.puremusic_no_approved_artists
import com.metrolist.shared.generated.resources.puremusic_password
import com.metrolist.shared.generated.resources.puremusic_password_changed
import com.metrolist.shared.generated.resources.puremusic_password_too_short
import com.metrolist.shared.generated.resources.puremusic_passwords_mismatch
import com.metrolist.shared.generated.resources.puremusic_remove
import com.metrolist.shared.generated.resources.puremusic_save_password
import com.metrolist.shared.generated.resources.puremusic_search_artists
import com.metrolist.shared.generated.resources.puremusic_search_failed
import com.metrolist.shared.generated.resources.puremusic_section_content
import com.metrolist.shared.generated.resources.puremusic_section_features
import com.metrolist.shared.generated.resources.puremusic_show_images
import com.metrolist.shared.generated.resources.puremusic_show_images_description
import com.metrolist.shared.puremusic.ApprovedArtist
import com.metrolist.shared.puremusic.PureMusicArtistSearch
import com.metrolist.shared.puremusic.PureMusicContentRule
import com.metrolist.shared.puremusic.PureMusicFeature
import com.metrolist.shared.puremusic.PureMusicPolicy
import com.metrolist.shared.puremusic.PureMusicSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal const val MIN_PASSWORD_LENGTH = 4

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Password + confirmation with validation. Calls [onSaved] after the password is stored. */
@Composable
internal fun PasswordSetupForm(
    onSaved: () -> Unit,
    newPasswordLabel: Boolean = false,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    var saved by remember { mutableStateOf(false) }
    val save = {
        error =
            when {
                password.length < MIN_PASSWORD_LENGTH -> Res.string.puremusic_password_too_short
                password != confirm -> Res.string.puremusic_passwords_mismatch
                else -> null
            }
        if (error == null) {
            PureMusicSettings.setPassword(password)
            saved = true
            password = ""
            confirm = ""
            onSaved()
        } else {
            confirm = ""
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PasswordField(
            password,
            { password = it },
            stringResource(if (newPasswordLabel) Res.string.puremusic_new_password else Res.string.puremusic_password),
            ImeAction.Next,
        )
        PasswordField(confirm, { confirm = it }, stringResource(Res.string.puremusic_confirm_password), ImeAction.Done, save)
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (saved && newPasswordLabel) Text(stringResource(Res.string.puremusic_password_changed))
        Button(onClick = save, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.puremusic_save_password)) }
    }
}

@Composable
private fun SwitchRow(
    title: StringResource,
    description: StringResource,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(description)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
internal fun SectionTitle(text: StringResource) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

private val contentRuleRows =
    listOf(
        Triple(PureMusicContentRule.HIDE_EXPLICIT, Res.string.puremusic_hide_explicit, Res.string.puremusic_hide_explicit_description),
        Triple(PureMusicContentRule.HIDE_VIDEO_SONGS, Res.string.puremusic_hide_video_songs, Res.string.puremusic_hide_video_songs_description),
        Triple(
            PureMusicContentRule.HIDE_NON_MUSICAL_VIDEOS,
            Res.string.puremusic_hide_non_musical,
            Res.string.puremusic_hide_non_musical_description,
        ),
    )

/** Each row is phrased as "Disable X": the switch on means the feature is off. */
private val disableRows =
    listOf(
        Triple(PureMusicFeature.VIDEO, Res.string.puremusic_disable_video, Res.string.puremusic_feature_video_description),
        Triple(PureMusicFeature.CANVAS, Res.string.puremusic_disable_canvas, Res.string.puremusic_feature_canvas_description),
        Triple(
            PureMusicFeature.LISTEN_TOGETHER,
            Res.string.puremusic_disable_listen_together,
            Res.string.puremusic_feature_listen_together_description,
        ),
        Triple(PureMusicFeature.CAST, Res.string.puremusic_disable_cast, Res.string.puremusic_feature_cast_description),
    )

/**
 * All PureMusic switches. With [onOpenArtists] the approved artists are a row that opens a separate page;
 * without it (onboarding) the artist manager is shown inline.
 */
@Composable
internal fun PureMusicSettingsList(onOpenArtists: (() -> Unit)?) {
    Column {
        if (PureMusicSettings.lockedUntilConfigured) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.puremusic_locked_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.puremusic_locked_description))
                    Button(onClick = {
                        PureMusicSettings.lockedUntilConfigured = false
                        PureMusicPolicy.notifyChanged()
                    }) { Text(stringResource(Res.string.puremusic_locked_confirm)) }
                }
            }
        }

        SectionTitle(Res.string.puremusic_section_content)
        SwitchRow(
            Res.string.puremusic_artist_filter,
            Res.string.puremusic_artist_filter_description,
            PureMusicSettings.artistFilterEnabled,
        ) {
            PureMusicSettings.artistFilterEnabled = it
            PureMusicPolicy.notifyChanged()
        }
        if (onOpenArtists != null) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.puremusic_approved_artists)) },
                supportingContent = {
                    Text(stringResource(Res.string.puremusic_approved_artists_count, PureMusicSettings.approvedArtists().size.toString()))
                },
                modifier = Modifier.clickable(onClick = onOpenArtists),
            )
        }
        SwitchRow(
            Res.string.puremusic_show_images,
            Res.string.puremusic_show_images_description,
            PureMusicSettings.imagesEnabled,
        ) { PureMusicSettings.imagesEnabled = it }
        contentRuleRows.forEach { (rule, title, description) ->
            SwitchRow(title, description, PureMusicSettings.isContentRuleOn(rule)) { PureMusicSettings.setContentRule(rule, it) }
        }
        val hidesVideos =
            PureMusicSettings.isContentRuleOn(PureMusicContentRule.HIDE_VIDEO_SONGS) ||
                PureMusicSettings.isContentRuleOn(PureMusicContentRule.HIDE_NON_MUSICAL_VIDEOS)
        if (hidesVideos) {
            SwitchRow(
                Res.string.puremusic_videos_as_audio,
                Res.string.puremusic_videos_as_audio_description,
                PureMusicSettings.videosAsAudio,
            ) { PureMusicSettings.videosAsAudio = it }
        }

        HorizontalDivider(Modifier.padding(top = 8.dp))
        SectionTitle(Res.string.puremusic_section_features)
        disableRows.forEach { (feature, title, description) ->
            SwitchRow(title, description, !PureMusicSettings.isFeatureAllowed(feature)) { disabled ->
                PureMusicSettings.setFeatureAllowed(feature, !disabled)
            }
        }

        if (onOpenArtists == null) {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            SectionTitle(Res.string.puremusic_approved_artists)
            ApprovedArtistsManager()
        }
    }
}

private fun kindLabel(kind: String): StringResource =
    when (kind) {
        ApprovedArtist.KIND_PODCAST -> Res.string.puremusic_kind_podcast
        ApprovedArtist.KIND_CHANNEL -> Res.string.puremusic_kind_channel
        else -> Res.string.puremusic_kind_artist
    }

/** Search YouTube Music for artists and manage the approved list. Plain column, safe inside scrolling parents. */
@Composable
internal fun ApprovedArtistsManager() {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ApprovedArtist>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val approved = PureMusicSettings.approvedArtists()
    val approvedIds = approved.mapTo(mutableSetOf()) { it.id }

    fun update(artists: List<ApprovedArtist>) {
        PureMusicSettings.setApprovedArtists(artists)
        PureMusicPolicy.notifyChanged()
    }

    val search = {
        scope.launch {
            searching = true
            failed = false
            try {
                // A pasted link or @handle is resolved to its channel or podcast; anything else is searched.
                results = PureMusicArtistSearch.resolveLink(query)?.let(::listOf) ?: PureMusicArtistSearch.search(query)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            } finally {
                searching = false
            }
        }
        Unit
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(Res.string.puremusic_search_artists)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
        )
        if (searching) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        }
        if (failed) {
            Text(
                stringResource(Res.string.puremusic_search_failed),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        results.forEach { artist ->
            ListItem(
                headlineContent = { Text(artist.name) },
                supportingContent = { Text(stringResource(kindLabel(artist.kind))) },
                trailingContent = {
                    if (artist.id in approvedIds) {
                        Text(stringResource(Res.string.puremusic_added), color = MaterialTheme.colorScheme.primary)
                    } else {
                        TextButton(onClick = {
                            update(approved + artist)
                        }) { Text(stringResource(Res.string.puremusic_add)) }
                    }
                },
            )
        }
        HorizontalDivider()
        if (approved.isEmpty()) {
            Text(
                stringResource(Res.string.puremusic_no_approved_artists),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        approved.forEach { artist ->
            ListItem(
                headlineContent = { Text(artist.name) },
                supportingContent = { Text(stringResource(kindLabel(artist.kind))) },
                trailingContent = {
                    TextButton(onClick = { update(approved.filterNot { it.id == artist.id }) }) {
                        Text(stringResource(Res.string.puremusic_remove), color = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }
    }
}
