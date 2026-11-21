/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.puremusic

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.metrolist.shared.deviceconnect.DeviceCrypto
import com.metrolist.shared.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.russhwolf.settings.Settings as KmpSettings

/** Features that PureMusic can switch off from the Control & Management screen. */
enum class PureMusicFeature(
    internal val key: String,
) {
    VIDEO("pureMusicVideoAllowed"),
    CANVAS("pureMusicCanvasAllowed"),
    LISTEN_TOGETHER("pureMusicListenTogetherAllowed"),
    CAST("pureMusicCastAllowed"),
}

/**
 * Metrolist content switches that PureMusic owns: the value set in the protected screen is copied into
 * Metrolist's setting and any other change is reverted.
 */
enum class PureMusicContentRule(
    internal val key: String,
) {
    HIDE_EXPLICIT("pureMusicHideExplicit"),
    HIDE_VIDEO_SONGS("pureMusicHideVideoSongs"),
    HIDE_NON_MUSICAL_VIDEOS("pureMusicHideNonMusicalVideos"),
}

@Serializable
data class ApprovedArtist(
    val id: String,
    val name: String,
    val thumbnail: String? = null,
)

/**
 * PureMusic preferences. They live in the same store as Metrolist's settings (the v13 keys are kept so
 * PureMusic 0.x installs carry over), but are excluded from backup export/import so a restored backup
 * can never weaken the restrictions.
 */
object PureMusicSettings {
    // Keys shared with PureMusic 0.x (v13). Do not rename.
    const val PASSWORD_HASH_KEY = "pureMusicPasswordHash"
    const val IMAGES_ENABLED_KEY = "pureMusicImagesEnabled"
    const val ARTIST_FILTER_ENABLED_KEY = "pureMusicArtistFilterEnabled"

    const val APPROVED_ARTISTS_KEY = "pureMusicApprovedArtistsJson"
    const val LEGACY_MIGRATION_KEY = "pureMusicLegacyMigrationVersion"
    const val LOCKED_KEY = "pureMusicLockedUntilConfigured"

    val keys: Set<String> =
        setOf(
            PASSWORD_HASH_KEY,
            IMAGES_ENABLED_KEY,
            ARTIST_FILTER_ENABLED_KEY,
            APPROVED_ARTISTS_KEY,
            LEGACY_MIGRATION_KEY,
            LOCKED_KEY,
        ) + PureMusicFeature.entries.map { it.key } + PureMusicContentRule.entries.map { it.key }

    private val json = Json { ignoreUnknownKeys = true }
    private val store: KmpSettings get() = Settings.shared.pureMusicStore

    private class BooleanPref(
        val key: String,
        val default: Boolean,
    ) {
        val state: MutableState<Boolean> by lazy { mutableStateOf(store.getBoolean(key, default)) }

        fun set(value: Boolean) {
            store.putBoolean(key, value)
            state.value = value
        }
    }

    private class StringPref(
        val key: String,
        val default: String,
    ) {
        val state: MutableState<String> by lazy { mutableStateOf(store.getString(key, default)) }

        fun set(value: String) {
            store.putString(key, value)
            state.value = value
        }
    }

    private val passwordHashPref = StringPref(PASSWORD_HASH_KEY, "")
    private val imagesEnabledPref = BooleanPref(IMAGES_ENABLED_KEY, false)
    private val artistFilterEnabledPref = BooleanPref(ARTIST_FILTER_ENABLED_KEY, true)
    private val approvedArtistsPref = StringPref(APPROVED_ARTISTS_KEY, "[]")
    private val lockedPref = BooleanPref(LOCKED_KEY, false)
    private val featurePrefs = PureMusicFeature.entries.associateWith { BooleanPref(it.key, false) }
    private val contentRulePrefs = PureMusicContentRule.entries.associateWith { BooleanPref(it.key, false) }

    val hasPassword: Boolean get() = passwordHashPref.state.value.isNotBlank()

    var imagesEnabled: Boolean
        get() = imagesEnabledPref.state.value
        set(value) = imagesEnabledPref.set(value)

    var artistFilterEnabled: Boolean
        get() = artistFilterEnabledPref.state.value
        set(value) = artistFilterEnabledPref.set(value)

    /** Set when the 0.x data could not be migrated; everything stays blocked until the parent opens the control screen. */
    var lockedUntilConfigured: Boolean
        get() = lockedPref.state.value
        set(value) = lockedPref.set(value)

    var legacyMigrationVersion: Int
        get() = store.getInt(LEGACY_MIGRATION_KEY, 0)
        set(value) = store.putInt(LEGACY_MIGRATION_KEY, value)

    val approvedArtistsJson: String get() = approvedArtistsPref.state.value

    fun approvedArtists(): List<ApprovedArtist> =
        try {
            json.decodeFromString<List<ApprovedArtist>>(approvedArtistsJson)
        } catch (_: Exception) {
            emptyList()
        }

    fun setApprovedArtists(artists: List<ApprovedArtist>) {
        approvedArtistsPref.set(json.encodeToString(artists.distinctBy { it.id }.sortedBy { it.name.lowercase() }))
    }

    fun isFeatureAllowed(feature: PureMusicFeature): Boolean = featurePrefs.getValue(feature).state.value

    fun setFeatureAllowed(
        feature: PureMusicFeature,
        allowed: Boolean,
    ) = featurePrefs.getValue(feature).set(allowed)

    fun hasContentRule(rule: PureMusicContentRule): Boolean = store.hasKey(rule.key)

    fun isContentRuleOn(rule: PureMusicContentRule): Boolean = contentRulePrefs.getValue(rule).state.value

    fun setContentRule(
        rule: PureMusicContentRule,
        on: Boolean,
    ) = contentRulePrefs.getValue(rule).set(on)

    fun setPassword(password: String) = passwordHashPref.set(hashPassword(password))

    fun verifyPassword(password: String): Boolean {
        val stored = passwordHashPref.state.value
        return stored.isNotBlank() && constantTimeEquals(hashPassword(password), stored)
    }

    /** SHA-256 hex, identical to PureMusic 0.x so existing passwords keep working. */
    internal fun hashPassword(password: String): String =
        DeviceCrypto.sha256(password.encodeToByteArray()).joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }

    private fun constantTimeEquals(
        a: String,
        b: String,
    ): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
