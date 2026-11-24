package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.EqPreset
import com.example.data.local.YaPulseDatabase
import com.example.data.repository.HistoryRepository
import com.example.ui.components.formatDurationMs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("YaPulse", appName)
    }

    @Test
    fun `formatDurationMs formats minutes and seconds`() {
        assertEquals("0:00", formatDurationMs(0L))
        assertEquals("3:05", formatDurationMs(185_000L))
    }

    @Test
    fun `eq preset resolves fallback safely`() {
        assertEquals(EqPreset.BASS_BOOST, EqPreset.fromName("BASS_BOOST"))
        assertEquals(EqPreset.FLAT, EqPreset.fromName("UNKNOWN"))
    }

    @Test
    fun `history repository deduplicates consecutive identical tracks`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, YaPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = HistoryRepository(db.trackHistoryDao())
            repo.recordTrackIfChanged("Track A", "Artist 1", "Album", "ru.yandex.music", 180_000L)
            repo.recordTrackIfChanged("Track A", "Artist 1", "Album", "ru.yandex.music", 180_000L)
            repo.recordTrackIfChanged("Track B", "Artist 2", "Album", "ru.yandex.music", 210_000L)

            val total = repo.totalPlays.first()
            assertEquals(2, total)
        } finally {
            db.close()
        }
    }
}
