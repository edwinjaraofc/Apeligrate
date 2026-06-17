package com.apeligrate.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apeligrate.ui.components.SentinelBackground
import com.apeligrate.ui.components.SentinelMapPanel

@Composable
fun MapScreen() {
    SentinelBackground {
        SentinelMapPanel(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            title = "Mapa estrategico",
            subtitle = "Vista ampliada del monitoreo comunitario sobre OpenStreetMap."
        )
    }
}
