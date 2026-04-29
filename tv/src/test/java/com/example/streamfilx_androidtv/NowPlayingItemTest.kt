package com.example.streamfilx_androidtv

import com.example.streamfilx_androidtv.core.models.EpisodeInfo
import com.example.streamfilx_androidtv.core.models.NowPlayingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingItemTest {

    private fun movieItem() = NowPlayingItem(
        id = "tt1234567",
        type = "movie",
        title = "Inception",
        streamUrl = "http://example.com/video.mp4",
    )

    private fun seriesItem(
        season: Int = 2,
        episode: Int = 5,
        episodeName: String? = "The Swedes",
    ) = NowPlayingItem(
        id = "tt0903747",
        type = "series",
        title = "Breaking Bad",
        streamUrl = "http://example.com/video.mp4",
        season = season,
        episode = episode,
        episodeName = episodeName,
    )

    // ── episodeInfo ───────────────────────────────────────────────────────────

    @Test fun `episodeInfo null for movie`() {
        assertNull(movieItem().episodeInfo)
    }

    @Test fun `episodeInfo formatted correctly`() {
        assertEquals("S02E05", seriesItem().episodeInfo)
    }

    @Test fun `episodeInfo pads single digits`() {
        assertEquals("S01E01", seriesItem(season = 1, episode = 1).episodeInfo)
    }

    // ── qualityBadge ──────────────────────────────────────────────────────────

    @Test fun `qualityBadge 4K from 2160`() {
        assertEquals("4K", movieItem().copy(streamQuality = "2160p").qualityBadge)
    }

    @Test fun `qualityBadge FHD from 1080p`() {
        assertEquals("FHD", movieItem().copy(streamQuality = "1080p").qualityBadge)
    }

    @Test fun `qualityBadge HD from 720p`() {
        assertEquals("HD", movieItem().copy(streamQuality = "720p").qualityBadge)
    }

    @Test fun `qualityBadge SD from 480p`() {
        assertEquals("SD", movieItem().copy(streamQuality = "480p").qualityBadge)
    }

    @Test fun `qualityBadge null when no quality`() {
        assertNull(movieItem().qualityBadge)
    }

    // ── playerDisplayTitle ────────────────────────────────────────────────────

    @Test fun `playerDisplayTitle for movie is just title`() {
        assertEquals("Inception", movieItem().playerDisplayTitle)
    }

    @Test fun `playerDisplayTitle for series includes episode info and name`() {
        val title = seriesItem().playerDisplayTitle
        assertTrue(title.contains("Breaking Bad"))
        assertTrue(title.contains("S02E05"))
        assertTrue(title.contains("The Swedes"))
    }

    @Test fun `playerDisplayTitle for series without episode name omits name`() {
        val title = seriesItem(episodeName = null).playerDisplayTitle
        assertTrue(title.contains("S02E05"))
        assertFalse(title.contains("null"))
    }

    // ── hasNextEpisode ────────────────────────────────────────────────────────

    @Test fun `hasNextEpisode false when no nextEpisodeId`() {
        assertFalse(seriesItem().hasNextEpisode)
    }

    @Test fun `hasNextEpisode true when nextEpisodeId set`() {
        assertTrue(seriesItem().copy(nextEpisodeId = "tt0903747:2:6").hasNextEpisode)
    }

    // ── availableSeasons ──────────────────────────────────────────────────────

    @Test fun `availableSeasons empty without allEpisodes`() {
        assertTrue(seriesItem().availableSeasons.isEmpty())
    }

    @Test fun `availableSeasons sorted with season 0 last`() {
        val episodes = listOf(
            EpisodeInfo("id1", 2, 1),
            EpisodeInfo("id2", 1, 1),
            EpisodeInfo("id3", 0, 1),  // special
        )
        val item = seriesItem().copy(allEpisodes = episodes)
        assertEquals(listOf(1, 2, 0), item.availableSeasons)
    }

    // ── episodesForSeason ─────────────────────────────────────────────────────

    @Test fun `episodesForSeason returns correct episodes sorted`() {
        val episodes = listOf(
            EpisodeInfo("id2", 1, 2),
            EpisodeInfo("id1", 1, 1),
            EpisodeInfo("id3", 2, 1),
        )
        val item = seriesItem().copy(allEpisodes = episodes)
        val s1 = item.episodesForSeason(1)
        assertEquals(2, s1.size)
        assertEquals(1, s1[0].episode)
        assertEquals(2, s1[1].episode)
    }
}
