package com.storagelens.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.StorageFormatter

@Composable
fun CircularStorageGauge(
    usedPercentage: Float,
    usedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier,
    size: Dp = 210.dp,
    strokeWidth: Dp = 14.dp
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(usedPercentage) {
        val target = if (usedPercentage.isNaN() || usedPercentage.isInfinite()) 0f else (usedPercentage / 100f).coerceIn(0f, 1f)
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
    }

    val trackColor = StorageLensTheme.colors.surfaceVariant
    val primaryAccent = StorageLensTheme.colors.accentPrimary
    val secondaryAccent = StorageLensTheme.colors.accentSecondary
    val textColor = StorageLensTheme.colors.textPrimary
    val textSecondary = StorageLensTheme.colors.textSecondary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Track background arc (260 degree arc)
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress active arc
            val sweep = 260f * animatedProgress.value
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to primaryAccent,
                        0.7f to secondaryAccent,
                        1.0f to primaryAccent
                    ),
                    startAngle = 140f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Center metric info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = StorageFormatter.formatPercentage(animatedProgress.value * 100f),
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Used Storage",
                color = textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${StorageFormatter.formatBytes(usedBytes)} / ${StorageFormatter.formatBytes(totalBytes)}",
                color = secondaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
