package com.storagelens.app.ui.screens.explore

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.ui.components.BreadcrumbBar
import com.storagelens.app.ui.components.DeleteConfirmationDialog
import com.storagelens.app.ui.components.EmptyStateView
import com.storagelens.app.ui.components.FileListItem
import com.storagelens.app.ui.components.FolderListItem
import com.storagelens.app.ui.theme.StorageLensTheme
import java.io.File

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPath by viewModel.currentPath.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
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

    // SAF Tree Picker Launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (ignored: Exception) {}
            viewModel.loadSafTree(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StorageLensTheme.colors.background)
    ) {
        // Explorer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Folder Explorer",
                    color = StorageLensTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${folders.size} folders · ${files.size} files",
                    color = StorageLensTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Open Custom SAF Folder button
                IconButton(onClick = { safLauncher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open Directory via SAF",
                        tint = StorageLensTheme.colors.accentSecondary
                    )
                }

                // Sort Order Menu
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
        }

        // Breadcrumbs
        BreadcrumbBar(
            breadcrumbs = breadcrumbs,
            onSelectBreadcrumb = { path ->
                viewModel.loadDirectory(path)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StorageLensTheme.colors.accentSecondary)
            }
        } else if (folders.isEmpty() && files.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.FolderOpen,
                title = if (errorMessage != null) "Folder Inaccessible" else "Directory is Empty",
                description = errorMessage ?: "No accessible files or subdirectories found in this location.",
                actionLabel = "Choose Folder via SAF",
                onAction = { safLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Folders section
                if (folders.isNotEmpty()) {
                    items(folders, key = { "folder_${it.path}" }) { folder ->
                        FolderListItem(
                            folder = folder,
                            onClick = {
                                viewModel.loadDirectory(folder.path)
                            }
                        )
                    }
                }

                // Files section
                if (files.isNotEmpty()) {
                    items(files, key = { "file_${it.id}" }) { file ->
                        FileListItem(
                            file = file,
                            onOpen = { openFileWithApp(context, file) },
                            onShare = { shareFile(context, file) },
                            onDelete = { viewModel.promptDelete(file) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
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
