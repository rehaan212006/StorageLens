package com.storagelens.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.storagelens.app.data.models.SortOrder
import com.storagelens.app.data.preferences.ThemeMode
import com.storagelens.app.ui.components.StorageCard
import com.storagelens.app.ui.theme.StorageLensTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    val defaultSortOrder by viewModel.defaultSortOrder.collectAsState()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState()

    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StorageLensTheme.colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Settings",
                    color = StorageLensTheme.colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "App preferences & open source details",
                    color = StorageLensTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Appearance / Theme Section
        item {
            StorageCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = StorageLensTheme.colors.accentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Appearance",
                            color = StorageLensTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        ThemeMode.DARK to "Dark (Recommended)",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.SYSTEM to "Follow System"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = StorageLensTheme.colors.accentSecondary,
                                    unselectedColor = StorageLensTheme.colors.border
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                color = StorageLensTheme.colors.textPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Scanning & Sorting Preferences
        item {
            StorageCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = StorageLensTheme.colors.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scan & Explorer Preferences",
                            color = StorageLensTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Default sort order picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortMenuExpanded = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Default Sort Order",
                                color = StorageLensTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = defaultSortOrder.label,
                                color = StorageLensTheme.colors.accentSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Box {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = StorageLensTheme.colors.textSecondary
                            )

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                modifier = Modifier.background(StorageLensTheme.colors.surfaceVariant)
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label, color = StorageLensTheme.colors.textPrimary) },
                                        onClick = {
                                            sortMenuExpanded = false
                                            viewModel.setDefaultSortOrder(order)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = StorageLensTheme.colors.border, modifier = Modifier.padding(vertical = 8.dp))

                    // Show hidden files toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Hidden Files",
                                color = StorageLensTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Include files beginning with a dot (.)",
                                color = StorageLensTheme.colors.textTertiary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = showHiddenFiles,
                            onCheckedChange = { viewModel.setShowHiddenFiles(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StorageLensTheme.colors.accentSecondary,
                                checkedTrackColor = StorageLensTheme.colors.accentPrimary
                            )
                        )
                    }
                }
            }
        }

        // Privacy & Scoped Storage Section
        item {
            StorageCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StorageLensTheme.colors.success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy & Security",
                            color = StorageLensTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• 100% Offline: No internet permission is requested.\n• Scoped Storage Compliant: Uses official MediaStore & SAF APIs.\n• Never reads private app data or tracks user behavior.\n• All scanning & hashing happens locally on your CPU.",
                        color = StorageLensTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = { showPrivacyDialog = true }) {
                        Text(
                            text = "Read Full Privacy Policy",
                            color = StorageLensTheme.colors.accentSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Support the Developer Section
        item {
            StorageCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = StorageLensTheme.colors.warning.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StorageLensTheme.colors.warning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = StorageLensTheme.colors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Support the Developer",
                                color = StorageLensTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "StorageLens is free & open-source",
                                color = StorageLensTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "If StorageLens helps you keep your device clean, consider supporting its independent open-source development.",
                        color = StorageLensTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            openUrl(context, "https://buymeacoffee.com/storagelens")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StorageLensTheme.colors.warning
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = null,
                            tint = StorageLensTheme.colors.background,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buy Me a Coffee",
                            color = StorageLensTheme.colors.background,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // About & Links Section
        item {
            StorageCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StorageLensTheme.colors.accentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "About StorageLens",
                            color = StorageLensTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", color = StorageLensTheme.colors.textSecondary, fontSize = 13.sp)
                        Text("1.0.0 (Build 1)", color = StorageLensTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Divider(color = StorageLensTheme.colors.border, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl(context, "https://github.com/storagelens/storagelens-android") }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = StorageLensTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GitHub Repository", color = StorageLensTheme.colors.textPrimary, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = StorageLensTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
                    }

                    Divider(color = StorageLensTheme.colors.border, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLicensesDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = StorageLensTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open-Source Licenses", color = StorageLensTheme.colors.textPrimary, fontSize = 13.sp)
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = StorageLensTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Open-source licenses dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            containerColor = StorageLensTheme.colors.surface,
            title = {
                Text(
                    text = "Open-Source Licenses",
                    color = StorageLensTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "StorageLens is licensed under the Apache License 2.0.\n\nThird-Party Libraries:\n• AndroidX Core KTX (Apache 2.0)\n• Jetpack Compose & Material 3 (Apache 2.0)\n• KotlinX Coroutines (Apache 2.0)\n• Jetpack DataStore (Apache 2.0)\n• Navigation Compose (Apache 2.0)",
                        color = StorageLensTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLicensesDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StorageLensTheme.colors.accentPrimary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Full Privacy dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = StorageLensTheme.colors.surface,
            title = {
                Text(
                    text = "Privacy Guarantee",
                    color = StorageLensTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "StorageLens is strictly local and private:\n\n1. No Internet Access: The application does not contain internet permissions in AndroidManifest.xml.\n\n2. Scoped Storage: Android restricts file access to user-permitted media and document trees. StorageLens will never access private app sandboxes.\n\n3. Zero Telemetry: No crash reporters, analytics SDKs, or ad identifiers are bundled.\n\n4. Hashing Safety: SHA-256 duplicate detection processes file bytes in a streaming 64KB buffer strictly within local RAM.",
                        color = StorageLensTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StorageLensTheme.colors.accentPrimary
                    )
                ) {
                    Text("Understood")
                }
            }
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (ignored: Exception) {}
}
