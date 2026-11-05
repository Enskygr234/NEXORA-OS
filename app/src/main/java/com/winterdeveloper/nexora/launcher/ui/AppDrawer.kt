@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.core.designsystem.glass
import com.winterdeveloper.nexora.domain.model.AppCategory
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.usecase.AppSearch
import com.winterdeveloper.nexora.domain.usecase.ListOps
import com.winterdeveloper.nexora.launcher.FolderUi
import com.winterdeveloper.nexora.launcher.LauncherUiState

enum class DrawerTab(val labelRes: Int) {
    ALL(R.string.drawer_tab_all),
    FAVORITES(R.string.drawer_tab_favorites),
    CATEGORIES(R.string.drawer_tab_categories),
    FOLDERS(R.string.drawer_tab_folders),
}

@Composable
fun AppDrawer(
    state: LauncherUiState,
    appearance: Appearance,
    badges: Map<String, Int>,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onAppClick: (LauncherApp) -> Unit,
    onAppLongClick: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (FolderUi) -> Unit,
    onCreateFolder: () -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(DrawerTab.ALL) }
    var category by rememberSaveable { mutableStateOf<AppCategory?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val close by rememberUpdatedState(onClose)
    val closeThreshold = with(density) { 110.dp.toPx() }

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            try {
                focusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                // Field not attached yet; the user can tap it.
            }
        }
    }

    // Pull down at the top of the list closes the drawer.
    val pullDownConnection = remember(closeThreshold) {
        object : NestedScrollConnection {
            var accumulated = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    accumulated += available.y
                    if (accumulated > closeThreshold) {
                        accumulated = 0f
                        close()
                    }
                } else if (consumed.y != 0f) {
                    accumulated = 0f
                }
                return Offset.Zero
            }
        }
    }

    val results = remember(query, state.apps) { AppSearch.search(state.apps, query) }
    val searching = query.isNotBlank()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2030612))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Handle: drag down to close.
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(closeThreshold) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { change, amount ->
                            total += amount
                            if (total > closeThreshold / 2) {
                                change.consume()
                                total = 0f
                                close()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 42.dp, height = 4.dp)
                    .background(palette.accent.copy(alpha = 0.6f), RoundedCornerShape(50))
            )
        }

        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = palette.accent) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, stringResource(R.string.action_clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    results.firstOrNull()?.let {
                        keyboard?.hide()
                        onAppClick(it)
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.07f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = palette.accent,
                ),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, stringResource(R.string.settings_title), tint = Color.White)
            }
        }

        if (!searching) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(DrawerTab.entries.toList()) { t ->
                    NeonChip(selected = tab == t, label = stringResource(t.labelRes)) { tab = t }
                }
            }
            if (tab == DrawerTab.CATEGORIES) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.categories) { c ->
                        NeonChip(selected = category == c, label = stringResource(categoryLabel(c))) {
                            category = if (category == c) null else c
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
        }

        val columns = appearance.gridColumns
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullDownConnection),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val tile: @Composable (LauncherApp) -> Unit = { app ->
                AppTile(
                    app = app,
                    sizeDp = appearance.iconSizeDp,
                    shape = appearance.iconShape,
                    showLabel = true,
                    badgeCount = badges[app.key.packageName] ?: 0,
                    modifier = Modifier.combinedClickable(
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) },
                    ),
                )
            }

            when {
                searching -> {
                    if (results.isEmpty()) message(R.string.search_no_results)
                    items(results, key = { "s:" + it.key.flatten() }) { tile(it) }
                }
                tab == DrawerTab.ALL -> alphabetical(state.apps, palette.accent, tile)
                tab == DrawerTab.FAVORITES -> {
                    if (state.favorites.isEmpty()) message(R.string.favorites_empty)
                    items(state.favorites, key = { "f:" + it.key.flatten() }) { tile(it) }
                }
                tab == DrawerTab.CATEGORIES -> {
                    val list = state.apps.filter { category == null || it.category == category }
                    items(list, key = { "c:" + it.key.flatten() }) { tile(it) }
                }
                else -> { // DrawerTab.FOLDERS
                    item(key = "new-folder") {
                        FolderTile(
                            name = stringResource(R.string.folder_new),
                            count = null,
                            isNew = true,
                            onClick = onCreateFolder,
                        )
                    }
                    items(state.folders, key = { "d:" + it.id }) { folder ->
                        FolderTile(
                            name = folder.name,
                            count = folder.apps.size,
                            isNew = false,
                            onClick = { onOpenFolder(folder) },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.message(res: Int) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            stringResource(res),
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun LazyGridScope.alphabetical(
    apps: List<LauncherApp>,
    accent: Color,
    tile: @Composable (LauncherApp) -> Unit,
) {
    // Collation can interleave letters (e.g. "A" and "Ä"), so each header is emitted once
    // to keep lazy-grid keys unique.
    val seen = HashSet<String>()
    apps.forEach { app ->
        val letter = ListOps.sectionLetter(app.label)
        if (seen.add(letter)) {
            item(key = "h:$letter", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    letter,
                    modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
        }
        item(key = "a:" + app.key.flatten()) { tile(app) }
    }
}

@Composable
fun NeonChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            labelColor = Color.White.copy(alpha = 0.8f),
            selectedContainerColor = palette.accent.copy(alpha = 0.22f),
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.12f),
            selectedBorderColor = palette.accent,
        ),
    )
}

@Composable
private fun FolderTile(name: String, count: Int?, isNew: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .padding(6.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .glass(RoundedCornerShape(22.dp), palette),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isNew) Icons.Rounded.CreateNewFolder else Icons.Rounded.Folder,
                contentDescription = null,
                tint = if (isNew) Color.White.copy(alpha = 0.8f) else palette.accent,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (count != null) {
            Text(
                pluralStringResourceSafe(count),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun pluralStringResourceSafe(count: Int): String =
    androidx.compose.ui.res.pluralStringResource(R.plurals.folder_app_count, count, count)

fun categoryLabel(c: AppCategory): Int = when (c) {
    AppCategory.GAMES -> R.string.category_games
    AppCategory.AUDIO -> R.string.category_audio
    AppCategory.VIDEO -> R.string.category_video
    AppCategory.IMAGE -> R.string.category_image
    AppCategory.SOCIAL -> R.string.category_social
    AppCategory.NEWS -> R.string.category_news
    AppCategory.MAPS -> R.string.category_maps
    AppCategory.PRODUCTIVITY -> R.string.category_productivity
    AppCategory.ACCESSIBILITY -> R.string.category_accessibility
    AppCategory.OTHER -> R.string.category_other
}
