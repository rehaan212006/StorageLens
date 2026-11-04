package com.storagelens.app.ui.screens.insights

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.storagelens.app.ui.components.DeleteConfirmationDialog
import com.storagelens.app.ui.components.DuplicateGroupCard
import com.storagelens.app.ui.components.EmptyStateView
import com.storagelens.app.ui.components.FileListItem
import com.storagelens.app.ui.components.FolderListItem
import com.storagelens.app.ui.components.MetricCard
import com.storagelens.app.ui.components.StorageCard
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
    val pendingDeleteGroup by viewModel.pendingDeleteGroup.collectAsState()
    val pendingDeleteFile by viewModel.pendingDeleteFile.collectAsState()

    var selectedTab by remember { mutableStateOf(InsightTab.DUPLICATES) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StorageLensTheme.colors.background)
    ) {
        // Insights Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Storage Insights",
                color = StorageLensTheme.colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actionable analysis & duplicate detection",
                color = StorageLensTheme.colors.textSecondary,
                fontSize = 12.sp
            )
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
                    color = StorageLensTheme.colors.accentSecondary,
                    height = 3.dp
                )
            },
            divider = { Divider(color = StorageLensTheme.colors.border) }
        ) {
            InsightTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
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
                    EmptyStateView(
                        icon = Icons.Default.AutoAwesome,
                        title = "No Duplicate Files Found",
                        description = "All scanned files are unique, or no scan has been performed yet.",
                        actionLabel = "Scan Device Storage",
                        onAction = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
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
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = "No Unused Files Found",
                        description = "Files that haven't been accessed or modified in over 6 months will appear here.",
                        actionLabel = "Scan Device Storage",
                        onAction = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
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
                        title = "No Folder Data",
                        description = "Run a storage scan to rank folders by size.",
                        actionLabel = "Scan Device Storage",
                        onAction = onNavigateToScan,
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
                        title = "No Files Indexed",
                        description = "Run a storage scan to identify the largest files taking up space.",
                        actionLabel = "Scan Device Storage",
                        onAction = onNavigateToScan,
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
