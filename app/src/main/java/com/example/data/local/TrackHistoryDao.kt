package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackHistoryDao {
    @Query("SELECT * FROM track_history ORDER BY playedAtMs DESC LIMIT :limit")
    fun observeRecentTracks(limit: Int = 50): Flow<List<TrackHistoryEntity>>

    @Query("SELECT COUNT(*) FROM track_history")
    fun observeTotalPlays(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT artist) FROM track_history WHERE artist != ''")
    fun observeUniqueArtists(): Flow<Int>

    @Query(
        """
        SELECT artist, COUNT(*) AS playCount
        FROM track_history
        WHERE artist != ''
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT :limit
        """
    )
    fun observeTopArtists(limit: Int = 5): Flow<List<ArtistStatTuple>>

    @Query("SELECT * FROM track_history ORDER BY id DESC LIMIT 1")
    suspend fun getLatestTrack(): TrackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackHistoryEntity)

    @Query("DELETE FROM track_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM track_bookmarks ORDER BY createdAtMs DESC")
    fun observeBookmarks(): Flow<List<TrackBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: TrackBookmarkEntity)

    @Query("DELETE FROM track_bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM track_bookmarks")
    suspend fun clearBookmarks()
}
