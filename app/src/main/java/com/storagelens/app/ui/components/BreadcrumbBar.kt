package com.storagelens.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagelens.app.ui.theme.StorageLensTheme

data class BreadcrumbItem(
    val name: String,
    val path: String
)

@Composable
fun BreadcrumbBar(
    breadcrumbs: List<BreadcrumbItem>,
    onSelectBreadcrumb: (path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(breadcrumbs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, crumb ->
            val isLast = index == breadcrumbs.size - 1

            TextButton(
                onClick = { onSelectBreadcrumb(crumb.path) },
                enabled = !isLast
            ) {
                if (index == 0) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (isLast) StorageLensTheme.colors.accentSecondary else StorageLensTheme.colors.textSecondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = crumb.name,
                    color = if (isLast) StorageLensTheme.colors.accentSecondary else StorageLensTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = StorageLensTheme.colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}
