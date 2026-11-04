package com.storagelens.app.ui.screens.largefiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.LargeFileFilter
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.ui.components.DeleteConfirmationDialog
import com.storagelens.app.ui.components.EmptyStateView
import com.storagelens.app.ui.components.FileListItem
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.StorageFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFilesScreen(
    viewModel: LargeFilesViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val largeFiles by viewModel.largeFiles.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val pendingDeleteFile by viewModel.pendingDeleteFile.collectAsState()
    val pendingIntentSender by viewModel.pendingDeleteIntentSender.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Scoped Storage deletion permission launcher (Android 10 & 11+)
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { sender ->
            deletePermissionLauncher.launch(
                IntentSenderRequest.Builder(sender).build()
            )
        }
    }

    val totalBytes = largeFiles.sumOf { it.sizeBytes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StorageLensTheme.colors.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Large Files Finder",
                    color = StorageLensTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${largeFiles.size} files · ${StorageFormatter.formatBytes(totalBytes)} total",
                    color = StorageLensTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }

            // Sort button
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort order",
                        tint = StorageLensTheme.colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(StorageLensTheme.colors.surfaceVariant)
                ) {
                    SortOrder.values().forEach { order ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = order.label,
                                    color = if (order == sortOrder) StorageLensTheme.colors.accentSecondary else StorageLensTheme.colors.textPrimary,
                                    fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortMenuExpanded = false
                                viewModel.setSortOrder(order)
                            }
                        )
                    }
                }
            }
        }

        // Threshold Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LargeFileFilter.values().forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StorageLensTheme.colors.accentPrimary,
                        selectedLabelColor = StorageLensTheme.colors.textPrimary,
                        containerColor = StorageLensTheme.colors.surfaceVariant,
                        labelColor = StorageLensTheme.colors.textSecondary
                    ),
                    border = null
                )
            }
        }

        // Category Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" Chip
            val isAllSelected = selectedCategory == null
            FilterChip(
                selected = isAllSelected,
                onClick = { viewModel.setCategory(null) },
                label = { Text("All Types") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StorageLensTheme.colors.accentSecondary.copy(alpha = 0.2f),
                    selectedLabelColor = StorageLensTheme.colors.accentSecondary,
                    containerColor = StorageLensTheme.colors.surface,
                    labelColor = StorageLensTheme.colors.textTertiary
                ),
                border = null
            )

            // Category Chips
            listOf(StorageCategory.VIDEOS, StorageCategory.IMAGES, StorageCategory.DOCUMENTS, StorageCategory.AUDIO, StorageCategory.OTHER).forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(if (isSelected) null else cat) },
                    label = { Text(cat.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.color.copy(alpha = 0.2f),
                        selectedLabelColor = cat.color,
                        containerColor = StorageLensTheme.colors.surface,
                        labelColor = StorageLensTheme.colors.textTertiary
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Files List
        if (largeFiles.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Whatshot,
                title = "No Large Files Found",
                description = "No files match the ${selectedFilter.label} threshold. Run a storage scan to discover large media.",
                actionLabel = "Scan Storage",
                onAction = onNavigateToScan,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(largeFiles, key = { it.id }) { file ->
                    FileListItem(
                        file = file,
                        onOpen = { openFileWithApp(context, file) },
                        onShare = { shareFile(context, file) },
                        onDelete = { viewModel.promptDelete(file) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Confirmation dialog
    pendingDeleteFile?.let { file ->
        DeleteConfirmationDialog(
            fileName = file.name,
            fileSize = file.sizeBytes,
            filePath = file.path,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDeletePrompt() }
        )
    }
}

private fun openFileWithApp(context: Context, file: FileItem) {
    try {
        val uri: Uri = if (file.path.isNotEmpty() && File(file.path).exists()) {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
            } catch (e: Exception) {
                file.uri
            }
        } else {
            file.uri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        // Fallback
    }
}

private fun shareFile(context: Context, file: FileItem) {
    try {
        val uri: Uri = if (file.path.isNotEmpty() && File(file.path).exists()) {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.path))
            } catch (e: Exception) {
                file.uri
            }
        } else {
            file.uri
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share file"))
    } catch (e: Exception) {
        // Fallback
    }
}
