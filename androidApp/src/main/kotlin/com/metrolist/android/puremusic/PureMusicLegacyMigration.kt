/*
 * PureMusic (C) 2026 - fork of Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.puremusic

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.metrolist.shared.puremusic.ApprovedArtist
import com.metrolist.shared.puremusic.PureMusicSettings
import timber.log.Timber

/**
 * Carries PureMusic 0.x (Metrolist v13) data into the v14 base.
 *
 * The password and the switches already live in the shared `settings` DataStore under the same keys, so
 * they need no conversion. The approved artists were a Room table (`approved_artist`) that Metrolist's own
 * v13 import does not know about and drops, so they are copied into settings here, before onboarding runs.
 * If the copy fails the app is locked until the parent reviews the settings.
 */
internal object PureMusicLegacyMigration {
    private const val VERSION = 1
    private const val DATABASE_NAME = "song.db"
    private const val TABLE = "approved_artist"

    fun run(context: Context) {
        if (PureMusicSettings.legacyMigrationVersion >= VERSION) return
        try {
            val legacy = readLegacyArtists(context)
            if (legacy != null) {
                val merged = PureMusicSettings.approvedArtists() + legacy
                PureMusicSettings.setApprovedArtists(merged)
                Timber.tag(TAG).i("Migrated %d approved artists", legacy.size)
            }
        } catch (error: Exception) {
            Timber.tag(TAG).e(error, "Approved artist migration failed; locking until reviewed")
            PureMusicSettings.lockedUntilConfigured = true
        }
        PureMusicSettings.legacyMigrationVersion = VERSION
    }

    /** Returns null when there is nothing to migrate (fresh install or no PureMusic table). */
    private fun readLegacyArtists(context: Context): List<ApprovedArtist>? {
        val file = context.getDatabasePath(DATABASE_NAME)
        if (!file.isFile) return null
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val hasTable =
                db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(TABLE)).use { it.moveToFirst() }
            if (!hasTable) return@use null
            db.rawQuery("SELECT artistId, name, thumbnailUrl FROM $TABLE", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0) ?: continue
                        add(
                            ApprovedArtist(
                                id = id,
                                name = cursor.getString(1).orEmpty().ifBlank { id },
                                thumbnail = if (cursor.isNull(2)) null else cursor.getString(2),
                            ),
                        )
                    }
                }
            }
        }
    }

    private const val TAG = "PureMusicMigration"
}
