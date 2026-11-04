package com.storagelens.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.data.models.FileItem
import com.storagelens.app.data.models.StorageCategory
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.DateFormatter
import com.storagelens.app.utils.StorageFormatter

@Composable
fun FileListItem(
    file: FileItem,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StorageLensTheme.colors.surface)
            .clickable {
                if (isSelectionMode) {
                    onToggleSelect?.invoke()
                } else {
                    onOpen?.invoke()
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect?.invoke() },
                colors = CheckboxDefaults.colors(
                    checkedColor = StorageLensTheme.colors.accentSecondary,
                    uncheckedColor = StorageLensTheme.colors.border
                ),
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        // File icon badge
        val icon = getFileIcon(file.category, file.isDirectory)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(file.category.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = file.category.color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // File info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = StorageLensTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = StorageFormatter.formatBytes(file.sizeBytes),
                    color = StorageLensTheme.colors.accentSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " · ",
                    color = StorageLensTheme.colors.textTertiary,
                    fontSize = 12.sp
                )
                Text(
                    text = DateFormatter.formatDate(file.lastModified),
                    color = StorageLensTheme.colors.textTertiary,
                    fontSize = 12.sp
                )
            }
            if (file.path.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.path,
                    color = StorageLensTheme.colors.textTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Actions menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = StorageLensTheme.colors.textSecondary
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(StorageLensTheme.colors.surfaceVariant)
            ) {
                if (onOpen != null) {
                    DropdownMenuItem(
                        text = { Text("Open", color = StorageLensTheme.colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = StorageLensTheme.colors.textSecondary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpen.invoke()
                        }
                    )
                }
                if (onShare != null) {
                    DropdownMenuItem(
                        text = { Text("Share", color = StorageLensTheme.colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = StorageLensTheme.colors.textSecondary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShare.invoke()
                        }
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = StorageLensTheme.colors.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = StorageLensTheme.colors.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete.invoke()
                        }
                    )
                }
            }
        }
    }
}

private fun getFileIcon(category: StorageCategory, isDirectory: Boolean): ImageVector {
    if (isDirectory) return Icons.Default.Folder
    return when (category) {
        StorageCategory.IMAGES -> Icons.Default.Image
        StorageCategory.VIDEOS -> Icons.Default.VideoFile
        StorageCategory.AUDIO -> Icons.Default.AudioFile
        StorageCategory.DOCUMENTS -> Icons.Default.Description
        else -> Icons.Default.Description
    }
}
