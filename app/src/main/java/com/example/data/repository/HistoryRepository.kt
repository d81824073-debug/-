package com.example.data.repository

import com.example.data.local.ArtistStatTuple
import com.example.data.local.TrackBookmarkEntity
import com.example.data.local.TrackHistoryDao
import com.example.data.local.TrackHistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: TrackHistoryDao) {
    val recentTracks: Flow<List<TrackHistoryEntity>> = dao.observeRecentTracks(60)
    val totalPlays: Flow<Int> = dao.observeTotalPlays()
    val uniqueArtists: Flow<Int> = dao.observeUniqueArtists()
    val topArtists: Flow<List<ArtistStatTuple>> = dao.observeTopArtists(5)
    val bookmarks: Flow<List<TrackBookmarkEntity>> = dao.observeBookmarks()

    suspend fun recordTrackIfChanged(
        title: String,
        artist: String,
        album: String,
        sourcePackage: String,
        durationMs: Long
    ) {
        if (title.isBlank()) return
        val latest = dao.getLatestTrack()
        if (latest != null && latest.title == title && latest.artist == artist) {
            return
        }
        dao.insertTrack(
            TrackHistoryEntity(
                title = title,
                artist = artist,
                album = album,
                sourcePackage = sourcePackage,
                durationMs = durationMs
            )
        )
    }

    suspend fun addBookmark(title: String, artist: String, positionMs: Long) {
        if (title.isBlank()) return
        dao.insertBookmark(
            TrackBookmarkEntity(
                title = title,
                artist = artist,
                positionMs = positionMs
            )
        )
    }

    suspend fun deleteBookmark(id: Long) {
        dao.deleteBookmarkById(id)
    }

    suspend fun clearAll() {
        dao.clearHistory()
        dao.clearBookmarks()
    }
}
