package com.storagelens.app.ui.screens.insights

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.data.models.ScanState
import com.storagelens.app.ui.components.DeleteConfirmationDialog
import com.storagelens.app.ui.components.DuplicateGroupCard
import com.storagelens.app.ui.components.EmptyStateView
import com.storagelens.app.ui.components.FileListItem
import com.storagelens.app.ui.components.FolderListItem
import com.storagelens.app.ui.components.MetricCard
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.StorageFormatter

enum class InsightTab(val title: String) {
    DUPLICATES("Duplicates"),
    STALE("Unused / Stale"),
    FOLDERS("Top Folders"),
    LARGEST("Largest Files")
}

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insights by viewModel.insights.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val pendingDeleteGroup by viewModel.pendingDeleteGroup.collectAsState()
    val pendingDeleteFile by viewModel.pendingDeleteFile.collectAsState()

    var selectedTab by remember { mutableStateOf(InsightTab.DUPLICATES) }
    val isScanning = scanState is ScanState.Scanning
    val hasScanned = insights.totalScannedFiles > 0 || scanState is ScanState.Completed

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StorageLensTheme.colors.background)
    ) {
        // Insights Title & Scan Action Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Insights",
                    color = StorageLensTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isScanning) "Scanning device storage..." else "Actionable analysis & duplicate detection",
                    color = if (isScanning) StorageLensTheme.colors.accentSecondary else StorageLensTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { viewModel.triggerScan() },
                enabled = !isScanning
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan Storage",
                    tint = if (isScanning) StorageLensTheme.colors.textTertiary else StorageLensTheme.colors.accentSecondary
                )
            }
        }

        // Active Scan Progress Banner
        if (isScanning) {
            val currentScan = scanState as ScanState.Scanning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StorageLensTheme.colors.surface)
                    .padding(12.dp)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(currentScan.progress * 100).toInt()}%",
                            color = StorageLensTheme.colors.accentSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = currentScan.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = StorageLensTheme.colors.accentSecondary,
                        trackColor = StorageLensTheme.colors.surfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Duplicate Waste",
                value = StorageFormatter.formatBytes(insights.duplicateWastedBytes),
                subtitle = "${insights.duplicateGroups.size} groups detected",
                icon = Icons.Default.ContentCopy,
                accentColor = StorageLensTheme.colors.error,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = InsightTab.DUPLICATES }
            )

            MetricCard(
                title = "Downloads Size",
                value = StorageFormatter.formatBytes(insights.downloadsSizeBytes),
                subtitle = "${insights.downloadsCount} files in downloads",
                icon = Icons.Default.Download,
                accentColor = StorageLensTheme.colors.warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = StorageLensTheme.colors.background,
            contentColor = StorageLensTheme.colors.accentSecondary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    height = 3.dp,
                    color = StorageLensTheme.colors.accentSecondary
                )
            },
            divider = {
                Divider(color = StorageLensTheme.colors.border)
            }
        ) {
            InsightTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = when (tab) {
                                InsightTab.DUPLICATES -> "Duplicates (${insights.duplicateGroups.size})"
                                InsightTab.STALE -> "Unused (${insights.staleFiles.size})"
                                InsightTab.FOLDERS -> "Top Folders"
                                InsightTab.LARGEST -> "Largest Files"
                            },
                            color = if (isSelected) StorageLensTheme.colors.accentSecondary else StorageLensTheme.colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            InsightTab.DUPLICATES -> {
                if (insights.duplicateGroups.isEmpty()) {
                    if (!hasScanned) {
                        EmptyStateView(
                            icon = Icons.Default.AutoAwesome,
                            title = "Storage Not Yet Scanned",
                            description = "Scan your device to detect duplicate files, find stale items, and calculate space savings.",
                            actionLabel = "Scan Device Storage",
                            onAction = { viewModel.triggerScan() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.CheckCircle,
                            title = "No Duplicate Files Found",
                            description = "Great news! All ${insights.totalScannedFiles} indexed files on your device are unique. No storage is wasted by duplicates.",
                            actionLabel = null,
                            onAction = null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(insights.duplicateGroups, key = { it.hash }) { group ->
                            DuplicateGroupCard(
                                group = group,
                                onToggleFile = { fileId ->
                                    viewModel.toggleDuplicateSelection(group.hash, fileId)
                                },
                                onDeleteSelected = { grp ->
                                    viewModel.promptDeleteDuplicates(grp)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            InsightTab.STALE -> {
                if (insights.staleFiles.isEmpty()) {
                    if (!hasScanned) {
                        EmptyStateView(
                            icon = Icons.Default.History,
                            title = "Storage Not Yet Scanned",
                            description = "Scan your device to find files that have not been modified or accessed in over 6 months.",
                            actionLabel = "Scan Device Storage",
                            onAction = { viewModel.triggerScan() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.CheckCircle,
                            title = "No Unused Files Found",
                            description = "All scanned files have been modified or accessed recently (within the last 6 months).",
                            actionLabel = null,
                            onAction = null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Files unmodified for over 180 days (${StorageFormatter.formatBytes(insights.staleFilesBytes)} total)",
                                color = StorageLensTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(insights.staleFiles, key = { it.id }) { file ->
                            FileListItem(
                                file = file,
                                onDelete = { viewModel.promptDeleteFile(file) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            InsightTab.FOLDERS -> {
                if (insights.largestFolders.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Folder,
                        title = if (!hasScanned) "Storage Not Yet Scanned" else "No Folder Data",
                        description = if (!hasScanned) "Run a storage scan to rank folders by size." else "No accessible subfolders found in shared storage.",
                        actionLabel = if (!hasScanned) "Scan Device Storage" else null,
                        onAction = if (!hasScanned) { { viewModel.triggerScan() } } else null,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(insights.largestFolders, key = { it.path }) { folder ->
                            FolderListItem(
                                folder = folder,
                                onClick = {}
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            InsightTab.LARGEST -> {
                if (insights.largestFiles.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Whatshot,
                        title = if (!hasScanned) "Storage Not Yet Scanned" else "No Files Indexed",
                        description = if (!hasScanned) "Run a storage scan to identify the largest files taking up space." else "No accessible files were found.",
                        actionLabel = if (!hasScanned) "Scan Device Storage" else null,
                        onAction = if (!hasScanned) { { viewModel.triggerScan() } } else null,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(insights.largestFiles, key = { it.id }) { file ->
                            FileListItem(
                                file = file,
                                onDelete = { viewModel.promptDeleteFile(file) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for deleting duplicate copies
    pendingDeleteGroup?.let { group ->
        DeleteConfirmationDialog(
            fileName = "${group.selectedFiles.size} duplicate copies",
            fileSize = group.selectedBytes,
            count = group.selectedFiles.size,
            onConfirm = { viewModel.confirmDeleteDuplicates() },
            onDismiss = { viewModel.dismissDeleteDuplicatesPrompt() }
        )
    }

    // Confirmation dialog for individual file delete
    pendingDeleteFile?.let { file ->
        DeleteConfirmationDialog(
            fileName = file.name,
            fileSize = file.sizeBytes,
            filePath = file.path,
            onConfirm = { viewModel.confirmDeleteFile() },
            onDismiss = { viewModel.dismissDeleteFilePrompt() }
        )
    }
}
