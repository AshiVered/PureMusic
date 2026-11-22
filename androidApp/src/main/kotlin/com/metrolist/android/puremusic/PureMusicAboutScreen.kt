/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.metrolist.android.puremusic

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.android.BuildConfig
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.app_name
import com.metrolist.shared.generated.resources.logo
import com.metrolist.shared.generated.resources.puremusic_about
import com.metrolist.shared.generated.resources.puremusic_about_community
import com.metrolist.shared.generated.resources.puremusic_about_lead_developer
import com.metrolist.shared.generated.resources.puremusic_about_license
import com.metrolist.shared.generated.resources.puremusic_about_love
import com.metrolist.shared.generated.resources.puremusic_about_original
import com.metrolist.shared.generated.resources.puremusic_about_original_description
import com.metrolist.shared.generated.resources.puremusic_about_repository
import com.metrolist.shared.generated.resources.puremusic_back
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.icons.MetrolistIconsBrand
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DEVELOPER_NAME = "Ashi Vered"
private const val DEVELOPER_GITHUB = "https://github.com/AshiVered"
/** Shown even while images are hidden (see [PureMusicImageInterceptor]). */
internal const val DEVELOPER_AVATAR = "https://github.com/AshiVered.png"
private const val REPOSITORY_URL = "https://github.com/AshiVered/PureMusic"
private const val LICENSE_URL = "https://github.com/AshiVered/PureMusic/blob/main/LICENSE"
private const val ORIGINAL_URL = "https://github.com/MetrolistGroup/Metrolist"

private data class AboutLink(
    val title: StringResource,
    val description: String?,
    val icon: ImageVector,
    val url: String,
)

/** PureMusic's About page (replaces Metrolist's, as in PureMusic 0.x). */
@Composable
fun PureMusicAboutScreen(
    onNavigateBack: (() -> Unit)?,
    bottomInset: Dp = 0.dp,
) {
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }
    val links =
        listOf(
            AboutLink(Res.string.puremusic_about_repository, null, MetrolistIconsBrand.Github, REPOSITORY_URL),
            AboutLink(Res.string.puremusic_about_license, "GNU General Public License v3.0", MetrolistIcons.Outlined.Info, LICENSE_URL),
            AboutLink(
                Res.string.puremusic_about_original,
                stringResource(Res.string.puremusic_about_original_description),
                MetrolistIconsBrand.Github,
                ORIGINAL_URL,
            ),
        )

    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.puremusic_about)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(MetrolistIcons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.puremusic_back))
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomInset)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ElevatedCard(shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = stringResource(Res.string.app_name),
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Badge("v${BuildConfig.VERSION_NAME}", MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            if (BuildConfig.DEBUG) Badge("DEBUG", MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            ElevatedCard(shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Surface(
                            onClick = { open(DEVELOPER_GITHUB) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(90.dp),
                        ) {
                            AsyncImage(
                                model = DEVELOPER_AVATAR,
                                contentDescription = DEVELOPER_NAME,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column {
                            Text(DEVELOPER_NAME, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text(
                                stringResource(Res.string.puremusic_about_lead_developer),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    FilledTonalButton(onClick = { open(DEVELOPER_GITHUB) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(MetrolistIconsBrand.Github, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("GitHub")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                stringResource(Res.string.puremusic_about_community),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 8.dp),
            )
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                links.forEach { link ->
                    ListItem(
                        leadingContent = { Icon(link.icon, contentDescription = null) },
                        headlineContent = { Text(stringResource(link.title), fontWeight = FontWeight.SemiBold) },
                        supportingContent = link.description?.let { { Text(it) } },
                        modifier = Modifier.clickable { open(link.url) },
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
            Text(
                stringResource(Res.string.puremusic_about_love),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun Badge(
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(shape = RoundedCornerShape(8.dp), color = color) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
