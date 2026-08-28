package com.charlztech.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charlztech.tv.data.model.StreamServer
import com.charlztech.tv.ui.theme.AppColors
import com.charlztech.tv.util.StreamServerUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamServerCard(
    server: StreamServer,
    index: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = StreamServerUtils.kind(server)
    val isFast = StreamServerUtils.isFastDirect(server)
    val kindColor = when (kind) {
        StreamServerUtils.ServerKind.HLS -> AppColors.Secondary
        StreamServerUtils.ServerKind.DASH -> AppColors.Primary
        StreamServerUtils.ServerKind.MP4 -> AppColors.Accent
        StreamServerUtils.ServerKind.EMBED -> AppColors.Upcoming
        StreamServerUtils.ServerKind.UNKNOWN -> AppColors.TextSecondary
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) AppColors.Primary.copy(alpha = 0.18f) else AppColors.SurfaceElevated
        ),
        border = if (isActive) {
            BorderStroke(1.5.dp, AppColors.Primary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isActive) AppColors.Primary else AppColors.SurfaceHighlight,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.White else AppColors.TextPrimary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    StreamServerUtils.displayName(server, index),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StreamTag(text = kind.label, color = kindColor)
                    if (isFast) {
                        StreamTag(text = "FAST", color = AppColors.Live, icon = Icons.Default.Bolt)
                    }
                    if (isActive) {
                        StreamTag(text = "PLAYING", color = AppColors.Primary)
                    }
                }
            }
            Icon(
                if (isActive) Icons.Default.Speed else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isActive) AppColors.Primary else AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun StreamTag(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
