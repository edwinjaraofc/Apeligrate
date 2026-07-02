package com.apeligrate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentReportTest {
    @Test
    fun `isCritical returns true for critical categories`() {
        assertTrue("Robo a mano armada".isCritical())
        assertTrue("Hurto/Arrebato".isCritical())
    }

    @Test
    fun `isCritical returns false for non critical categories`() {
        assertFalse("Acoso".isCritical())
        assertFalse("Zona peligrosa".isCritical())
    }

    @Test
    fun `toAlert maps critical incident to critical alert`() {
        val report = IncidentReport(
            id = "r1",
            category = "Robo a mano armada",
            description = "Incidente critico",
            reportedAt = 1234L,
            latitude = -12.0,
            longitude = -77.0
        )

        val alert = report.toAlert()

        assertEquals("r1", alert.id)
        assertEquals("Robo a mano armada", alert.title)
        assertEquals("Incidente critico", alert.description)
        assertEquals(Severity.CRITICAL, alert.severity)
        assertEquals(1234L, alert.timestamp)
        assertEquals(-12.0, alert.latitude)
        assertEquals(-77.0, alert.longitude)
    }

    @Test
    fun `toAlert maps non critical incident to warning alert`() {
        val report = IncidentReport(
            id = "r2",
            category = "Acoso",
            description = "Incidente no critico"
        )

        val alert = report.toAlert()

        assertEquals(Severity.WARNING, alert.severity)
    }
}
