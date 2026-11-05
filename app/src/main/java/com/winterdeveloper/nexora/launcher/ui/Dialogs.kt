@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.domain.model.AppShortcut
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.launcher.FolderUi

@Composable
fun AppActionsSheet(
    app: LauncherApp,
    appearance: Appearance,
    isHome: Boolean,
    isFavorite: Boolean,
    shortcuts: List<AppShortcut>,
    shortcutsNeedDefault: Boolean,
    onOpen: () -> Unit,
    onShortcut: (AppShortcut) -> Unit,
    onToggleHome: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToFolder: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onEditHome: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = palette.navy) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(app, 48, appearance.iconShape)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(app.label, style = MaterialTheme.typography.titleLarge, color = Color.White)
                        if (app.label != app.systemLabel) {
                            Text(app.systemLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                        }
                        if (app.isWorkProfile) {
                            Text(stringResource(R.string.work_profile), style = MaterialTheme.typography.labelSmall, color = palette.accent)
                        }
                    }
                }
            }
            if (shortcuts.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.shortcuts_title)) }
                items(shortcuts, key = { "sc:" + it.id }) { s ->
                    ActionRow(Icons.Rounded.Bolt, s.label) { onShortcut(s) }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color.White.copy(alpha = 0.08f)) }
            } else if (shortcutsNeedDefault) {
                item {
                    Text(
                        stringResource(R.string.shortcuts_need_default),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
            item { ActionRow(Icons.Rounded.OpenInNew, stringResource(R.string.action_open), onOpen) }
            item {
                ActionRow(
                    Icons.Rounded.Home,
                    stringResource(if (isHome) R.string.action_remove_from_home else R.string.action_add_to_home),
                    onToggleHome,
                )
            }
            item {
                ActionRow(
                    if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite),
                    onToggleFavorite,
                )
            }
            item { ActionRow(Icons.Rounded.Folder, stringResource(R.string.action_add_to_folder), onAddToFolder) }
            item { ActionRow(Icons.Rounded.Edit, stringResource(R.string.action_rename), onRename) }
            if (onEditHome != null) {
                item { ActionRow(Icons.Rounded.Dashboard, stringResource(R.string.action_edit_home), onEditHome) }
            }
            item { ActionRow(Icons.Rounded.VisibilityOff, stringResource(R.string.action_hide), onHide) }
            item { ActionRow(Icons.Rounded.Info, stringResource(R.string.action_app_info), onAppInfo) }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = LocalPalette.current.accent,
    )
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = LocalPalette.current.accent)
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    allowEmpty: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 40) text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = allowEmpty || text.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun FolderChooserDialog(
    folders: List<FolderUi>,
    onPick: (FolderUi) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_folder)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    ActionRow(Icons.Rounded.CreateNewFolder, stringResource(R.string.folder_new), onNew)
                }
                items(folders, key = { it.id }) { f ->
                    ActionRow(Icons.Rounded.Folder, f.name) { onPick(f) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun FolderContentDialog(
    folder: FolderUi,
    appearance: Appearance,
    badges: Map<String, Int>,
    onAppClick: (LauncherApp) -> Unit,
    onRemoveApp: (LauncherApp) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var editing by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.navy,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    folder.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onRename) { Icon(Icons.Rounded.Edit, stringResource(R.string.action_rename)) }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.folder_delete), tint = Color(0xFFFF5470))
                }
            }
        },
        text = {
            if (folder.apps.isEmpty()) {
                Text(stringResource(R.string.folder_empty), color = Color.White.copy(alpha = 0.6f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    items(folder.apps, key = { it.key.flatten() }) { app ->
                        Box {
                            AppTile(
                                app = app,
                                sizeDp = appearance.iconSizeDp.coerceAtMost(56),
                                shape = appearance.iconShape,
                                showLabel = true,
                                badgeCount = badges[app.key.packageName] ?: 0,
                                modifier = Modifier.combinedClickable(
                                    onClick = { if (!editing) onAppClick(app) },
                                    onLongClick = { editing = true },
                                ),
                            )
                            if (editing) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color(0xFFFF3D5A), CircleShape)
                                        .clickable { onRemoveApp(app) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        stringResource(R.string.folder_remove_app),
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (editing) editing = false else onDismiss() }) {
                Text(stringResource(if (editing) R.string.action_done else R.string.action_close))
            }
        },
        dismissButton = {
            if (!editing && folder.apps.isNotEmpty()) {
                TextButton(onClick = { editing = true }) { Text(stringResource(R.string.action_edit)) }
            }
        },
    )
}

/** Confirmation used for destructive actions. */
@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = Color(0xFFFF5470)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
