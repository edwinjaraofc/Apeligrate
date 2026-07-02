package com.apeligrate.util

import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.data.remote.ORSResponse
import com.apeligrate.data.remote.ORSRoute
import com.apeligrate.data.remote.ORSSummary
import com.apeligrate.data.remote.RouteService
import com.apeligrate.domain.model.Alert
import com.apeligrate.domain.model.DangerZone
import com.apeligrate.domain.model.Severity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SafeRoutePlannerTest {
    @Test
    fun `planSafeRoute prefixes bearer token and decodes returned polyline`() = runTest {
        val routeService = FakeRouteService(
            response = ORSResponse(
                routes = listOf(
                    ORSRoute(
                        geometry = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        summary = ORSSummary(distance = 1200.0, duration = 900.0)
                    )
                )
            )
        )
        val planner = SafeRoutePlanner(routeService)

        val result = planner.planSafeRoute(
            origin = DeviceCoordinates(38.5, -120.2),
            destination = DeviceCoordinates(43.252, -126.453),
            dangerZones = emptyList(),
            apiKey = "token-123"
        )

        assertNotNull(result)
        assertEquals("Bearer token-123", routeService.lastAuthorizationHeader)
        assertEquals(3, result!!.points.size)
        assertEquals(1200.0, result.distanceMeters, 0.0)
        assertEquals(900.0, result.durationSeconds, 0.0)
    }

    @Test
    fun `planSafeRoute counts touched danger zones on decoded path`() = runTest {
        val routeService = FakeRouteService(
            response = ORSResponse(
                routes = listOf(
                    ORSRoute(
                        geometry = "_p~iF~ps|U_ulLnnqC_mqNvxq`@",
                        summary = ORSSummary(distance = 1200.0, duration = 900.0)
                    )
                )
            )
        )
        val planner = SafeRoutePlanner(routeService)
        val dangerZone = DangerZone(
            id = "zone-1",
            centerLatitude = 40.7,
            centerLongitude = -120.95,
            radiusMeters = 150f,
            reports = listOf(
                Alert(
                    id = "a1",
                    title = "Incidente",
                    description = "Descripcion",
                    severity = Severity.WARNING,
                    timestamp = 0L,
                    latitude = 40.7,
                    longitude = -120.95
                )
            ),
            grouped = false
        )

        val result = planner.planSafeRoute(
            origin = DeviceCoordinates(38.5, -120.2),
            destination = DeviceCoordinates(43.252, -126.453),
            dangerZones = listOf(dangerZone),
            apiKey = "Bearer token-456"
        )

        assertNotNull(result)
        assertEquals("Bearer token-456", routeService.lastAuthorizationHeader)
        assertTrue(result!!.touchedZones >= 1)
    }

    private class FakeRouteService(
        private val response: ORSResponse
    ) : RouteService {
        var lastAuthorizationHeader: String? = null

        override suspend fun getRoute(
            profile: String,
            apiKey: String,
            request: com.apeligrate.data.remote.ORSRequest
        ): ORSResponse {
            lastAuthorizationHeader = apiKey
            return response
        }
    }
}
