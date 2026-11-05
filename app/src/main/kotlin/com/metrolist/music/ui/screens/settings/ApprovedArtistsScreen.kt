/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.R
import com.metrolist.music.db.entities.ApprovedArtist
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.ControlManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovedArtistsScreen(navController: NavController) {
    val viewModel: ControlManagementViewModel = hiltViewModel()
    val approvedArtists by viewModel.approvedArtists.collectAsStateWithLifecycle()
    val searchResults by viewModel.artistSearchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("אמנים מאושרים") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchArtists(it)
                },
                label = { Text("חפש אמן ב-YouTube Music") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Search results
                if (query.isNotBlank() && searchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "תוצאות חיפוש",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(searchResults, key = { "search_${it.id}" }) { artist ->
                        val isApproved = approvedArtists.any { it.artistId == artist.id }
                        ArtistSearchResultItem(
                            artist = artist,
                            isApproved = isApproved,
                            onToggle = { approved ->
                                if (approved) {
                                    viewModel.addArtist(
                                        ApprovedArtist(
                                            artistId = artist.id,
                                            name = artist.title,
                                            thumbnailUrl = artist.thumbnail,
                                        ),
                                    )
                                } else {
                                    viewModel.removeArtist(artist.id)
                                }
                            },
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Approved artists
                item {
                    Text(
                        text = "אמנים מאושרים (${approvedArtists.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }

                if (approvedArtists.isEmpty()) {
                    item {
                        Text(
                            text = "אין אמנים מאושרים עדיין.\nחפש אמן והוסף אותו לרשימה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(approvedArtists, key = { it.artistId }) { artist ->
                        ApprovedArtistListItem(
                            artist = artist,
                            onRemove = { viewModel.removeArtist(artist.artistId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistSearchResultItem(
    artist: ArtistItem,
    isApproved: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(artist.title) },
        leadingContent = {
            if (artist.thumbnail != null) {
                AsyncImage(
                    model = artist.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(modifier = Modifier.size(48.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.artist),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
        trailingContent = {
            Checkbox(
                checked = isApproved,
                onCheckedChange = onToggle,
            )
        },
    )
}

@Composable
private fun ApprovedArtistListItem(
    artist: ApprovedArtist,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(artist.name) },
        leadingContent = {
            if (artist.thumbnailUrl != null) {
                AsyncImage(
                    model = artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(modifier = Modifier.size(48.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.artist),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove, onLongClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "הסר",
                )
            }
        },
    )
}

