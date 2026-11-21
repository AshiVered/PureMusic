/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.android.LocalStartupPresentation
import com.metrolist.android.playback.LocalPlayerConnection
import com.metrolist.android.ui.component.tabContentTopPadding
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.puremusic_home_empty
import com.metrolist.shared.generated.resources.puremusic_home_title
import com.metrolist.shared.generated.resources.puremusic_refresh
import com.metrolist.shared.generated.resources.puremusic_shuffle
import com.metrolist.shared.puremusic.PureMusicHomeFeed
import com.metrolist.shared.puremusic.PureMusicSettings
import com.metrolist.shared.ui.components.YTListItem
import com.metrolist.shared.ui.icons.MetrolistIcons
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

/** Home page while the artist filter is on: albums by the approved artists and a shuffle button. */
@Composable
fun PureMusicHomeScreen(
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    floatingActionsBottomInset: State<Dp>,
    scrollToTopEvents: Flow<Long>,
) {
    val scope = rememberCoroutineScope()
    val state by PureMusicHomeFeed.state.collectAsState()
    val approvedSource = PureMusicSettings.approvedArtistsJson
    val playerConnection = LocalPlayerConnection.current
    val listState = rememberLazyListState()
    // Metrolist keeps the splash screen until its home page reports it is laid out.
    val startupPresentation = LocalStartupPresentation.current
    SideEffect {
        startupPresentation?.homeCacheReady = true
        startupPresentation?.homeLaidOut = true
    }
    LaunchedEffect(approvedSource) { PureMusicHomeFeed.ensureLoaded(scope) }
    LaunchedEffect(scrollToTopEvents) { scrollToTopEvents.collect { listState.animateScrollToItem(0) } }
    val shuffleLabel = stringResource(Res.string.puremusic_shuffle)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = tabContentTopPadding(), bottom = floatingActionsBottomInset.value + 88.dp),
        ) {
            item(key = "title") {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.puremusic_home_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { PureMusicHomeFeed.ensureLoaded(scope, force = true) }, enabled = !state.isLoading) {
                        Icon(MetrolistIcons.Filled.Refresh, contentDescription = stringResource(Res.string.puremusic_refresh))
                    }
                }
            }
            if (state.isLoading && state.albums.isEmpty()) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            } else if (state.albums.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(Res.string.puremusic_home_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp, start = 32.dp, end = 32.dp),
                    )
                }
            }
            items(state.albums, key = { "album:${it.id}" }) { album ->
                YTListItem(
                    item = album,
                    onClick = { onNavigateToAlbum(album.id) },
                    onArtistClick = onNavigateToArtist,
                    onAlbumClick = onNavigateToAlbum,
                    onAlbumPlay = { playerConnection?.playAlbum(album.id, album.title) },
                )
            }
        }
        if (state.albums.isNotEmpty() && playerConnection != null) {
            ExtendedFloatingActionButton(
                onClick = {
                    state.albums.randomOrNull()?.let { playerConnection.playAlbum(it.id, it.title, shuffle = true) }
                },
                icon = { Icon(MetrolistIcons.Filled.Shuffle, contentDescription = null) },
                text = { Text(shuffleLabel) },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = floatingActionsBottomInset.value + 16.dp),
            )
        }
    }
}
