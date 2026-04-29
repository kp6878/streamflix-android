package com.example.streamfilx_androidtv

import com.example.streamfilx_androidtv.features.player.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {

    private fun state(currentTime: Double = 0.0, duration: Double = 0.0) =
        PlayerState(currentTime = currentTime, duration = duration)

    // ── formatTime ────────────────────────────────────────────────────────────

    @Test fun `formatTime zero`() {
        assertEquals("0:00", state().formatTime(0.0))
    }

    @Test fun `formatTime under one minute`() {
        assertEquals("0:45", state().formatTime(45.0))
    }

    @Test fun `formatTime exactly one minute`() {
        assertEquals("1:00", state().formatTime(60.0))
    }

    @Test fun `formatTime minutes and seconds`() {
        assertEquals("23:45", state().formatTime(23 * 60 + 45.0))
    }

    @Test fun `formatTime over one hour`() {
        assertEquals("1:02:03", state().formatTime(3600 + 123.0))
    }

    @Test fun `formatTime negative clamps to zero`() {
        assertEquals("0:00", state().formatTime(-5.0))
    }

    // ── currentTimeFormatted / durationFormatted ──────────────────────────────

    @Test fun `currentTimeFormatted uses currentTime`() {
        val s = state(currentTime = 125.0, duration = 3600.0)
        assertEquals("2:05", s.currentTimeFormatted)
    }

    @Test fun `durationFormatted uses duration`() {
        val s = state(currentTime = 0.0, duration = 5400.0)
        assertEquals("1:30:00", s.durationFormatted)
    }

    // ── remainingTime ─────────────────────────────────────────────────────────

    @Test fun `remainingTime is duration minus currentTime`() {
        val s = state(currentTime = 300.0, duration = 1200.0)
        assertEquals(900.0, s.remainingTime, 0.001)
    }

    @Test fun `remainingTime clamps to zero when past end`() {
        val s = state(currentTime = 1300.0, duration = 1200.0)
        assertEquals(0.0, s.remainingTime, 0.001)
    }

    // ── progress ─────────────────────────────────────────────────────────────

    @Test fun `progress halfway`() {
        val s = PlayerState(currentTime = 600.0, duration = 1200.0, progress = 0.5)
        assertEquals(0.5, s.progress, 0.001)
    }
}
