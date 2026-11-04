package com.storagelens.app.ui.screens.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.data.models.ScanState
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.ui.components.CategoryRowItem
import com.storagelens.app.ui.components.CircularStorageGauge
import com.storagelens.app.ui.components.MetricCard
import com.storagelens.app.ui.components.MultiSegmentCategoryBar
import com.storagelens.app.ui.components.PermissionBanner
import com.storagelens.app.ui.components.StorageCard
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.DateFormatter
import com.storagelens.app.utils.StorageFormatter

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToLargeFiles: () -> Unit,
    onNavigateToInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.storageStats.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    val isScanning = scanState is ScanState.Scanning

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StorageLensTheme.colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "StorageLens",
                        color = StorageLensTheme.colors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Device Storage Overview",
                        color = StorageLensTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }

                // Scanning indicator or status badge
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(StorageLensTheme.colors.accentSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = StorageLensTheme.colors.accentSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scanning",
                                color = StorageLensTheme.colors.accentSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Permission notice if not granted
        if (!hasStoragePermission) {
            item {
                PermissionBanner(onRequestPermission = onRequestPermission)
            }
        }

        // Main Circular Gauge Card
        item {
            StorageCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (stats.isAvailable) {
                        CircularStorageGauge(
                            usedPercentage = stats.usedPercentage,
                            usedBytes = stats.usedBytes,
                            totalBytes = stats.totalBytes,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Text(
                            text = "Storage statistics unavailable",
                            color = StorageLensTheme.colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = StorageLensTheme.colors.border)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row of quick metrics: Used & Free
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Capacity",
                                color = StorageLensTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = StorageFormatter.formatBytes(stats.totalBytes),
                                color = StorageLensTheme.colors.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(StorageLensTheme.colors.border)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Free Space",
                                color = StorageLensTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = StorageFormatter.formatBytes(stats.freeBytes),
                                color = StorageLensTheme.colors.success,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Active Scan Progress Card
        if (isScanning) {
            item {
                val currentScan = scanState as ScanState.Scanning
                StorageCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = StorageLensTheme.colors.accentSecondary
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentScan.currentPhase,
                                color = StorageLensTheme.colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(currentScan.progress * 100).toInt()}%",
                                color = StorageLensTheme.colors.accentSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = currentScan.progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = StorageLensTheme.colors.accentSecondary,
                            trackColor = StorageLensTheme.colors.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${StorageFormatter.formatCount(currentScan.scannedFilesCount)} files indexed",
                                color = StorageLensTheme.colors.textTertiary,
                                fontSize = 11.sp
                            )

                            OutlinedButton(
                                onClick = { viewModel.cancelScan() },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = StorageLensTheme.colors.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", color = StorageLensTheme.colors.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Scan Storage Action Button
        item {
            Button(
                onClick = {
                    if (!isScanning) {
                        viewModel.triggerScan()
                    }
                },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StorageLensTheme.colors.accentPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Scanning Device..." else "Scan Storage",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Last scan status info
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StorageLensTheme.colors.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (stats.lastScanTimestamp > 0L) {
                        "Last scanned: ${DateFormatter.formatDetailed(stats.lastScanTimestamp)}"
                    } else {
                        "Tap Scan Storage to analyze disk contents"
                    },
                    color = StorageLensTheme.colors.textTertiary,
                    fontSize = 12.sp
                )
            }
        }

        // Storage Breakdown by Category Card
        item {
            StorageCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Storage Allocation",
                        color = StorageLensTheme.colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Categorized files on shared storage",
                        color = StorageLensTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Multi-segment horizontal bar
                    MultiSegmentCategoryBar(stats = stats)

                    Spacer(modifier = Modifier.height(16.dp))

                    // List of categories
                    StorageCategory.values().forEach { category ->
                        val size = stats.categorySizes[category] ?: 0L
                        val count = stats.categoryCounts[category] ?: 0
                        CategoryRowItem(
                            category = category,
                            sizeBytes = size,
                            fileCount = count,
                            totalBytes = stats.totalBytes
                        )
                    }
                }
            }
        }

        // Quick Shortcuts Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Explore",
                    value = "Browse",
                    subtitle = "Folder trees",
                    icon = Icons.Default.Folder,
                    accentColor = StorageLensTheme.colors.accentPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToExplore
                )

                MetricCard(
                    title = "Large Files",
                    value = "Clean",
                    subtitle = "100 MB+ items",
                    icon = Icons.Default.Whatshot,
                    accentColor = StorageLensTheme.colors.warning,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLargeFiles
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
