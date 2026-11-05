/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "approved_artist")
data class ApprovedArtist(
    @PrimaryKey val artistId: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val addedAt: LocalDateTime = LocalDateTime.now(),
)

