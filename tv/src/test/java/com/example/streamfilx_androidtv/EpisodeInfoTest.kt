package com.example.streamfilx_androidtv

import com.example.streamfilx_androidtv.core.models.EpisodeInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeInfoTest {

    // ── episodeString ─────────────────────────────────────────────────────────

    @Test fun `episodeString formats correctly`() {
        assertEquals("S02E05", EpisodeInfo("id", season = 2, episode = 5).episodeString)
    }

    @Test fun `episodeString pads single digits`() {
        assertEquals("S01E01", EpisodeInfo("id", season = 1, episode = 1).episodeString)
    }

    @Test fun `episodeString double digit season and episode`() {
        assertEquals("S12E24", EpisodeInfo("id", season = 12, episode = 24).episodeString)
    }

    // ── displayTitle ──────────────────────────────────────────────────────────

    @Test fun `displayTitle uses title when present`() {
        assertEquals("The Swedes", EpisodeInfo("id", 2, 5, title = "The Swedes").displayTitle)
    }

    @Test fun `displayTitle falls back to Episode N`() {
        assertEquals("Episode 5", EpisodeInfo("id", 2, 5, title = null).displayTitle)
    }

    @Test fun `displayTitle ignores empty string title`() {
        assertEquals("Episode 5", EpisodeInfo("id", 2, 5, title = "").displayTitle)
    }
}
