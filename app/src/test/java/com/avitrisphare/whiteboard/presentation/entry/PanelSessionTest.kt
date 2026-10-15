package com.avitrisphare.whiteboard.presentation.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelSessionTest {
    @Test
    fun licenseKeyMustMatchTheServerFormat() {
        assertTrue(LicenseKey.isWellFormed("GYSH-AB34-CD78-EF92"))
        assertTrue(LicenseKey.isWellFormed("gysh-ab34-cd78-ef92"))
        assertFalse(LicenseKey.isWellFormed("GYSH-0000-0000-0000"))
        assertFalse(LicenseKey.isWellFormed("GYSH-8F2A-4C91-B7E"))
        assertFalse(LicenseKey.isWellFormed("GYSH"))
    }

    @Test
    fun greetingFollowsTheTimeOfDay() {
        assertEquals("Good Morning, Priya", classGreeting(9, "Priya Sharma"))
        assertEquals("Good Afternoon, Priya", classGreeting(14, "Priya Sharma"))
        assertEquals("Good Evening, Teacher", classGreeting(20, "  "))
    }

    @Test
    fun boardAgeIsRelativeToNow() {
        val now = 1_700_000_000_000L
        assertEquals("Just now", boardAge(now - 20_000L, now))
        assertEquals("5m ago", boardAge(now - 5L * 60_000L, now))
        assertEquals("2h ago", boardAge(now - 2L * 60L * 60_000L, now))
        assertEquals("3d ago", boardAge(now - 3L * 24L * 60L * 60_000L, now))
    }

    @Test
    fun emailLocalPartBecomesADisplayName() {
        assertEquals("Priya Sharma", displayNameFromEmail("priya.sharma@dps.edu"))
        assertEquals("Teacher", displayNameFromEmail("@dps.edu"))
    }
}
