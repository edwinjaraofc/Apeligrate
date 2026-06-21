package com.apeligrate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apeligrate.data.local.DeviceCoordinates
import com.apeligrate.domain.model.IncidentReport
import com.apeligrate.ui.theme.DarkBackground
import com.apeligrate.ui.theme.TextSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val kind: MarkerKind,
    val category: String = ""
)

private enum class MarkerKind {
    USER,
    REPORT,
    PICKER
}

@Composable
fun SentinelMapPanel(
    modifier: Modifier = Modifier,
    centerCoordinates: DeviceCoordinates? = null,
    reportMarkers: List<IncidentReport> = emptyList(),
    title: String = "Mapa de vigilancia",
    subtitle: String = "Visualización basada en OpenStreetMap, sin API key.",
    isPickerMode: Boolean = false,
    onLocationPicked: (Double, Double) -> Unit = { _, _ -> }
) {
    val fallbackCoordinates = remember { DeviceCoordinates(-12.0464, -77.0428) }
    val center = centerCoordinates ?: fallbackCoordinates
    val zoom = if (centerCoordinates != null) 16.5 else 12.0
    val markers = remember(centerCoordinates, reportMarkers) {
        buildMarkers(centerCoordinates, reportMarkers)
    }

    SentinelCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                val mapView = rememberMapViewWithLifecycle()

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        Configuration.getInstance().userAgentValue = context.packageName
                        mapView.apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            setBuiltInZoomControls(false)
                            minZoomLevel = 4.0
                            maxZoomLevel = 19.0
                        }
                    },
                    update = { view ->
                        // Center and zoom
                        view.controller.setZoom(zoom)
                        view.controller.setCenter(GeoPoint(center.latitude, center.longitude))
                        
                        view.overlays.clear()

                        // Location Picker Overlay
                        if (isPickerMode) {
                            val eventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    onLocationPicked(p.latitude, p.longitude)
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint): Boolean = false
                            }
                            view.overlays.add(MapEventsOverlay(eventsReceiver))
                        }
                        
                        // Add Circles for reports (Radius) with category-specific colors
                        markers.filter { it.kind == MarkerKind.REPORT }.forEach { point ->
                            val colorInt = getCategoryColor(point.category)
                            val circlePoints = Polygon.pointsAsCircle(GeoPoint(point.latitude, point.longitude), 200.0)
                            val circle = Polygon(view).apply {
                                points = circlePoints
                                fillColor = android.graphics.Color.argb(40, android.graphics.Color.red(colorInt), android.graphics.Color.green(colorInt), android.graphics.Color.blue(colorInt))
                                strokeColor = android.graphics.Color.argb(100, android.graphics.Color.red(colorInt), android.graphics.Color.green(colorInt), android.graphics.Color.blue(colorInt))
                                strokeWidth = 2f
                            }
                            view.overlays.add(circle)
                        }

                        markers.forEach { point ->
                            Marker(view).apply {
                                position = GeoPoint(point.latitude, point.longitude)
                                setTitle(point.label)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = ContextCompat.getDrawable(
                                    view.context,
                                    org.osmdroid.library.R.drawable.marker_default
                                )?.mutate()?.apply {
                                    setTint(
                                        when (point.kind) {
                                            MarkerKind.USER -> android.graphics.Color.parseColor("#2A9D8F")
                                            MarkerKind.REPORT -> getCategoryColor(point.category)
                                            MarkerKind.PICKER -> android.graphics.Color.parseColor("#FFC107")
                                        }
                                    )
                                }
                            }.also(view.overlays::add)
                        }
                        view.invalidate()
                    }
                )
            }
        }
    }
}

private fun getCategoryColor(category: String): Int {
    return when (category) {
        "Robo a mano armada" -> android.graphics.Color.parseColor("#FF5252")
        "Hurto/Arrebato" -> android.graphics.Color.parseColor("#FF7043")
        "Acoso" -> android.graphics.Color.parseColor("#E91E63")
        "Zona peligrosa" -> android.graphics.Color.parseColor("#FFC107")
        "Calle sin iluminacion" -> android.graphics.Color.parseColor("#90A4AE")
        else -> android.graphics.Color.parseColor("#FF535B")
    }
}

private fun buildMarkers(
    centerCoordinates: DeviceCoordinates?,
    reportMarkers: List<IncidentReport>
): List<MapMarker> {
    val publicReports = reportMarkers.mapNotNull { report ->
        val latitude = report.latitude
        val longitude = report.longitude
        if (latitude == null || longitude == null) {
            null
        } else {
            MapMarker(
                latitude = latitude,
                longitude = longitude,
                label = report.category.ifBlank { "Incidente reportado" },
                kind = MarkerKind.REPORT,
                category = report.category
            )
        }
    }

    return buildList {
        if (centerCoordinates != null) {
            add(
                MapMarker(
                    latitude = centerCoordinates.latitude,
                    longitude = centerCoordinates.longitude,
                    label = "Ubicación seleccionada",
                    kind = MarkerKind.USER
                )
            )
        }
        addAll(publicReports)
        if (publicReports.isEmpty() && centerCoordinates == null) {
            add(MapMarker(-12.0464, -77.0428, "Centro de monitoreo", MarkerKind.REPORT, "Zona peligrosa"))
            add(MapMarker(-12.0673, -77.0336, "Alerta prioritaria", MarkerKind.REPORT, "Robo a mano armada"))
            add(MapMarker(-12.0838, -77.0506, "Zona con reportes recientes", MarkerKind.REPORT, "Hurto/Arrebato"))
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    return mapView
}
