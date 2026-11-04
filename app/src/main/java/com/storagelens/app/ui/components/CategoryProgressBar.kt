package com.storagelens.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.data.models.StorageStats
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.StorageFormatter

@Composable
fun MultiSegmentCategoryBar(
    stats: StorageStats,
    modifier: Modifier = Modifier
) {
    val totalBytes = stats.totalBytes.coerceAtLeast(1L)
    val usedBytes = stats.usedBytes
    val freeBytes = stats.freeBytes

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(stats) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    // Segmented bar
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(StorageLensTheme.colors.surfaceVariant)
    ) {
        // Draw categories
        for (category in StorageCategory.values()) {
            val catBytes = stats.categorySizes[category] ?: 0L
            val weight = if (totalBytes > 0) (catBytes.toFloat() / totalBytes.toFloat()) * animationProgress.value else 0f
            if (weight > 0.005f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(category.color)
                )
                Spacer(modifier = Modifier.width(1.5.dp))
            }
        }

        // Available free space segment
        val freeWeight = if (totalBytes > 0) (freeBytes.toFloat() / totalBytes.toFloat()) else 1f
        if (freeWeight > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(freeWeight.coerceAtLeast(0.01f))
                    .background(StorageLensTheme.colors.surfaceVariant)
            )
        }
    }
}

@Composable
fun CategoryRowItem(
    category: StorageCategory,
    sizeBytes: Long,
    fileCount: Int,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val percent = if (totalBytes > 0L) {
        (sizeBytes.toFloat() / totalBytes.toFloat()) * 100f
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(category.color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = category.displayName,
                    color = StorageLensTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (fileCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${StorageFormatter.formatCount(fileCount)})",
                        color = StorageLensTheme.colors.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = StorageFormatter.formatBytes(sizeBytes),
                    color = StorageLensTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = StorageFormatter.formatPercentage(percent),
                    color = StorageLensTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.width(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar for individual category
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(StorageLensTheme.colors.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(category.color)
            )
        }
    }
}
