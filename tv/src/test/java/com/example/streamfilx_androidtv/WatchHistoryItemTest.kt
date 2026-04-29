package com.example.streamfilx_androidtv

import com.example.streamfilx_androidtv.core.models.WatchHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchHistoryItemTest {

    private fun item(progress: Double = 0.5, completed: Boolean = false) = WatchHistoryItem(
        uniqueId = "tt1234567",
        contentId = "tt1234567",
        contentType = "movie",
        contentName = "Inception",
        poster = null,
        background = null,
        thumbnail = null,
        season = null,
        episode = null,
        episodeTitle = null,
        lastPosition = progress * 3600,
        duration = 3600.0,
        progress = progress,
        isCompleted = completed,
        lastWatchedAt = System.currentTimeMillis(),
        lastStreamUrl = null,
        lastStreamInfoHash = null,
        lastStreamFileIdx = null,
    )

    // ── shouldResume ─────────────────────────────────────────────────────────

    @Test fun `shouldResume true for in-progress item`() {
        assertTrue(item(progress = 0.5).shouldResume)
    }

    @Test fun `shouldResume false when completed`() {
        assertFalse(item(progress = 0.5, completed = true).shouldResume)
    }

    @Test fun `shouldResume false when progress below threshold`() {
        assertFalse(item(progress = 0.01).shouldResume)
    }

    @Test fun `shouldResume false when progress above threshold`() {
        assertFalse(item(progress = 0.92).shouldResume)
    }

    @Test fun `shouldResume true at lower bound`() {
        assertTrue(item(progress = 0.03).shouldResume)
    }

    @Test fun `shouldResume true at upper bound`() {
        assertTrue(item(progress = 0.89).shouldResume)
    }

    // ── timeRemaining ─────────────────────────────────────────────────────────

    @Test fun `timeRemaining returns minutes left`() {
        val i = item(progress = 0.5)  // 30 minutes left of 60-minute movie
        assertEquals("30 min left", i.timeRemaining)
    }

    @Test fun `timeRemaining empty when nothing left`() {
        val i = WatchHistoryItem(
            uniqueId = "x", contentId = "x", contentType = "movie", contentName = "X",
            poster = null, background = null, thumbnail = null, season = null, episode = null,
            episodeTitle = null, lastPosition = 3600.0, duration = 3600.0, progress = 1.0,
            isCompleted = true, lastWatchedAt = 0L, lastStreamUrl = null,
            lastStreamInfoHash = null, lastStreamFileIdx = null,
        )
        assertEquals("", i.timeRemaining)
    }

    // ── episodeInfo ───────────────────────────────────────────────────────────

    @Test fun `episodeInfo null for movie`() {
        assertNull(item().episodeInfo)
    }

    @Test fun `episodeInfo formatted for series`() {
        val i = item().copy(
            uniqueId = "tt0903747:S02E05",
            contentId = "tt0903747",
            contentType = "series",
            season = 2,
            episode = 5,
        )
        assertEquals("S02E05", i.episodeInfo)
    }

    // ── buildUniqueId ─────────────────────────────────────────────────────────

    @Test fun `buildUniqueId for movie is just contentId`() {
        assertEquals("tt1234567", WatchHistoryItem.buildUniqueId("tt1234567", null, null))
    }

    @Test fun `buildUniqueId for series includes episode`() {
        assertEquals("tt0903747:S02E05", WatchHistoryItem.buildUniqueId("tt0903747", 2, 5))
    }

    @Test fun `buildUniqueId pads single digits`() {
        assertEquals("tt0903747:S01E01", WatchHistoryItem.buildUniqueId("tt0903747", 1, 1))
    }
}
