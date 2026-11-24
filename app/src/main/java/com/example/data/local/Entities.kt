package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_history")
data class TrackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val artist: String,
    val album: String,
    val sourcePackage: String,
    val durationMs: Long,
    val playedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "track_bookmarks")
data class TrackBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val artist: String,
    val positionMs: Long,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class ArtistStatTuple(
    val artist: String,
    val playCount: Int
)
