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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.data.models.FolderItem
import com.storagelens.app.ui.theme.StorageLensTheme
import com.storagelens.app.utils.StorageFormatter

@Composable
fun FolderListItem(
    folder: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StorageLensTheme.colors.surface)
            .clickable(enabled = folder.isAccessible) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StorageLensTheme.colors.accentPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (folder.isAccessible) Icons.Default.Folder else Icons.Default.Lock,
                contentDescription = null,
                tint = if (folder.isAccessible) StorageLensTheme.colors.accentPrimary else StorageLensTheme.colors.textTertiary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = if (folder.isAccessible) StorageLensTheme.colors.textPrimary else StorageLensTheme.colors.textTertiary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (folder.sizeBytes > 0) {
                    Text(
                        text = StorageFormatter.formatBytes(folder.sizeBytes),
                        color = StorageLensTheme.colors.accentSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " · ",
                        color = StorageLensTheme.colors.textTertiary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "${folder.fileCount} files",
                    color = StorageLensTheme.colors.textTertiary,
                    fontSize = 12.sp
                )
                if (folder.subfolderCount > 0) {
                    Text(
                        text = ", ${folder.subfolderCount} folders",
                        color = StorageLensTheme.colors.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open folder",
            tint = StorageLensTheme.colors.textTertiary
        )
    }
}
