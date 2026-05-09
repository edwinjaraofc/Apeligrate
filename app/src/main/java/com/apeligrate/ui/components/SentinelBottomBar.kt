package com.apeligrate.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apeligrate.ui.theme.PrimaryContainer

enum class SentinelTab {
    INICIO, FEED, REPORTAR, PERFIL
}

@Composable
fun SentinelBottomBar(
    selectedTab: SentinelTab,
    onTabSelected: (SentinelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xCC131313)) // Surface/80
            .padding(bottom = 12.dp, top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SentinelNavItem(
                label = "Inicio",
                icon = Icons.Default.Home,
                isSelected = selectedTab == SentinelTab.INICIO,
                onClick = { onTabSelected(SentinelTab.INICIO) }
            )
            SentinelNavItem(
                label = "Feed",
                icon = Icons.Default.Apps,
                isSelected = selectedTab == SentinelTab.FEED,
                onClick = { onTabSelected(SentinelTab.FEED) }
            )
            SentinelNavItem(
                label = "Reportar",
                icon = Icons.Default.AddAlert,
                isSelected = selectedTab == SentinelTab.REPORTAR,
                onClick = { onTabSelected(SentinelTab.REPORTAR) }
            )
            SentinelNavItem(
                label = "Perfil",
                icon = Icons.Default.Person,
                isSelected = selectedTab == SentinelTab.PERFIL,
                onClick = { onTabSelected(SentinelTab.PERFIL) }
            )
        }
    }
}

@Composable
private fun SentinelNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")
    val backgroundColor by animateColorAsState(
        if (isSelected) PrimaryContainer else Color.Transparent,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}
