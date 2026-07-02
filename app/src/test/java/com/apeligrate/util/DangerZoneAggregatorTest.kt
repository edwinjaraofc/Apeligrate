package com.apeligrate.util

import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DangerZoneAggregatorTest {
    @Test
    fun `buildDangerZones creates grouped zone when alerts are close enough`() {
        val alerts = listOf(
            alert("a1", -12.04637, -77.04279),
            alert("a2", -12.04650, -77.04270),
            alert("a3", -12.04660, -77.04260)
        )

        val zones = DangerZoneAggregator.buildDangerZones(alerts)

        assertEquals(1, zones.size)
        assertTrue(zones.single().grouped)
        assertEquals(3, zones.single().reportCount)
        assertTrue(zones.single().radiusMeters >= DangerZoneAggregator.DEFAULT_ZONE_RADIUS_METERS)
    }

    @Test
    fun `buildDangerZones creates individual zones when cluster is too small`() {
        val alerts = listOf(
            alert("a1", -12.04637, -77.04279),
            alert("a2", -12.05000, -77.05000)
        )

        val zones = DangerZoneAggregator.buildDangerZones(alerts)

        assertEquals(2, zones.size)
        assertTrue(zones.all { !it.grouped })
        assertEquals(setOf("zone_a1", "zone_a2"), zones.map { it.id }.toSet())
    }

    @Test
    fun `findContainingZone prioritizes grouped zone over individual zone`() {
        val groupedZones = DangerZoneAggregator.buildDangerZones(
            listOf(
                alert("a1", -12.04637, -77.04279),
                alert("a2", -12.04650, -77.04270),
                alert("a3", -12.04660, -77.04260)
            )
        )
        val singleZone = DangerZoneAggregator.buildDangerZones(
            listOf(alert("a4", -12.04645, -77.04272))
        ).single()

        val zone = DangerZoneAggregator.findContainingZone(
            latitude = -12.04645,
            longitude = -77.04272,
            zones = groupedZones + singleZone
        )

        assertNotNull(zone)
        assertTrue(zone!!.grouped)
    }

    @Test
    fun `buildDangerZones ignores alerts without coordinates`() {
        val alerts = listOf(
            alert("a1", -12.04637, -77.04279),
            Alert(
                id = "a2",
                title = "Sin coordenadas",
                description = "No usable",
                severity = Severity.WARNING,
                timestamp = 0L,
                latitude = null,
                longitude = null
            )
        )

        val zones = DangerZoneAggregator.buildDangerZones(alerts)

        assertEquals(1, zones.size)
        assertFalse(zones.single().grouped)
    }

    private fun alert(id: String, latitude: Double, longitude: Double): Alert {
        return Alert(
            id = id,
            title = "Incidente",
            description = "Descripcion",
            severity = Severity.WARNING,
            timestamp = 0L,
            latitude = latitude,
            longitude = longitude
        )
    }
}
