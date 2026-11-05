@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.winterdeveloper.nexora.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winterdeveloper.nexora.BuildConfig
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.core.designsystem.NexoraPalettes
import com.winterdeveloper.nexora.core.designsystem.glass
import com.winterdeveloper.nexora.core.designsystem.neonGlow
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.BackgroundMode
import com.winterdeveloper.nexora.domain.model.ClockStyle
import com.winterdeveloper.nexora.domain.model.IconShape
import com.winterdeveloper.nexora.domain.model.TemperatureUnit
import com.winterdeveloper.nexora.launcher.ui.AppIconImage
import com.winterdeveloper.nexora.launcher.ui.ConfirmDialog
import com.winterdeveloper.nexora.launcher.ui.NeonChip
import com.winterdeveloper.nexora.launcher.ui.TextInputDialog
import com.winterdeveloper.nexora.settings.BackupEvent
import com.winterdeveloper.nexora.settings.CitySearchState
import com.winterdeveloper.nexora.settings.SettingsViewModel
import kotlin.math.roundToInt

interface SettingsBridge {
    fun close()
    fun requestDefaultLauncher()
    fun openNotificationAccess()
    fun pickSystemWallpaper()
    fun pickLiveWallpaper()
    fun exportBackup()
    fun importBackup()
    fun toast(text: String)
}

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    isDefaultLauncher: Boolean,
    badgeAccessGranted: Boolean,
    bridge: SettingsBridge,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val hiddenApps by vm.hiddenApps.collectAsStateWithLifecycle()
    val citySearch by vm.citySearch.collectAsStateWithLifecycle()
    val backupEvent by vm.backupEvent.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    val context = LocalContext.current
    val a = settings.appearance

    var saveProfileOpen by remember { mutableStateOf(false) }
    var resetLayoutOpen by remember { mutableStateOf(false) }
    var cityQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(backupEvent) {
        val event = backupEvent ?: return@LaunchedEffect
        val text = when (event) {
            is BackupEvent.Exported -> context.getString(R.string.backup_exported)
            is BackupEvent.Imported -> context.getString(
                R.string.backup_imported,
                event.summary.homeApps + event.summary.favorites,
                event.summary.folders,
            )
            BackupEvent.Failed -> context.getString(R.string.backup_failed)
        }
        bridge.toast(text)
        vm.consumeBackupEvent()
    }

    fun update(transform: (Appearance) -> Appearance) {
        vm.updateAppearance(transform)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = bridge::close) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black, palette.navy))),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { BrandHeader() }

            // ---- Default launcher ----
            item {
                Card {
                    Text(
                        stringResource(if (isDefaultLauncher) R.string.default_launcher_yes else R.string.default_launcher_no),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        stringResource(R.string.default_launcher_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    if (!isDefaultLauncher) {
                        Spacer(Modifier.height(10.dp))
                        NeonButton(stringResource(R.string.default_launcher_set), bridge::requestDefaultLauncher)
                    }
                }
            }

            // ---- Theme ----
            item {
                Card(title = stringResource(R.string.section_theme)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NexoraPalettes.all.forEach { p ->
                            val selected = p.id == a.themeId
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .background(Brush.linearGradient(listOf(p.accent, p.secondary)), CircleShape)
                                        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
                                        .clickable { update { it.copy(themeId = p.id) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Rounded.Check, null, tint = Color.Black)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(p.nameRes), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // ---- Theme profiles ----
            item {
                Card(title = stringResource(R.string.section_profiles), subtitle = stringResource(R.string.profiles_desc)) {
                    if (profiles.isEmpty()) {
                        Text(stringResource(R.string.profiles_empty), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    }
                    profiles.forEach { profile ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val p = NexoraPalettes.byId(profile.appearance.themeId)
                            Box(Modifier.size(20.dp).background(Brush.linearGradient(listOf(p.accent, p.secondary)), CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(profile.name, modifier = Modifier.weight(1f), color = Color.White)
                            TextButton(onClick = {
                                vm.applyProfile(profile)
                                bridge.toast(context.getString(R.string.profile_applied, profile.name))
                            }) { Text(stringResource(R.string.action_apply)) }
                            IconButton(onClick = { vm.deleteProfile(profile.id) }) {
                                Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete), tint = Color(0xFFFF5470))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedNeonButton(stringResource(R.string.profile_save_current)) { saveProfileOpen = true }
                }
            }

            // ---- Background & wallpaper ----
            item {
                Card(title = stringResource(R.string.section_background)) {
                    ChipRow(
                        options = BackgroundMode.entries,
                        selected = a.background,
                        label = { stringResource(backgroundLabel(it)) },
                        onSelect = { mode -> update { it.copy(background = mode) } },
                    )
                    if (a.background == BackgroundMode.SYSTEM_WALLPAPER) {
                        SliderRow(
                            title = stringResource(R.string.wallpaper_dim),
                            valueText = "${(a.wallpaperDim * 100).roundToInt()}%",
                            value = a.wallpaperDim,
                            range = 0f..0.8f,
                            steps = 7,
                            onChange = { v -> update { it.copy(wallpaperDim = v) } },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedNeonButton(stringResource(R.string.wallpaper_choose), bridge::pickSystemWallpaper)
                        OutlinedNeonButton(stringResource(R.string.wallpaper_live), bridge::pickLiveWallpaper)
                    }
                }
            }

            // ---- Icons & grid ----
            item {
                Card(title = stringResource(R.string.section_icons)) {
                    SliderRow(
                        title = stringResource(R.string.icon_size),
                        valueText = "${a.iconSizeDp} dp",
                        value = a.iconSizeDp.toFloat(),
                        range = Appearance.MIN_ICON_DP.toFloat()..Appearance.MAX_ICON_DP.toFloat(),
                        steps = (Appearance.MAX_ICON_DP - Appearance.MIN_ICON_DP) / 4 - 1,
                        onChange = { v -> update { it.copy(iconSizeDp = (v / 4f).roundToInt() * 4) } },
                    )
                    Text(stringResource(R.string.icon_shape), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    ChipRow(
                        options = IconShape.entries,
                        selected = a.iconShape,
                        label = { stringResource(iconShapeLabel(it)) },
                        onSelect = { shape -> update { it.copy(iconShape = shape) } },
                    )
                    SliderRow(
                        title = stringResource(R.string.grid_columns),
                        valueText = a.gridColumns.toString(),
                        value = a.gridColumns.toFloat(),
                        range = Appearance.MIN_COLUMNS.toFloat()..Appearance.MAX_COLUMNS.toFloat(),
                        steps = Appearance.MAX_COLUMNS - Appearance.MIN_COLUMNS - 1,
                        onChange = { v -> update { it.copy(gridColumns = v.roundToInt()) } },
                    )
                    SwitchRow(stringResource(R.string.show_labels), a.showLabels) { on -> update { it.copy(showLabels = on) } }
                }
            }

            // ---- Clock ----
            item {
                Card(title = stringResource(R.string.section_clock)) {
                    ChipRow(
                        options = ClockStyle.entries,
                        selected = a.clockStyle,
                        label = { stringResource(clockLabel(it)) },
                        onSelect = { style -> update { it.copy(clockStyle = style) } },
                    )
                    SwitchRow(stringResource(R.string.clock_24h), a.use24Hour) { on -> update { it.copy(use24Hour = on) } }
                    SwitchRow(stringResource(R.string.clock_seconds), a.showSeconds) { on -> update { it.copy(showSeconds = on) } }
                }
            }

            // ---- Animations ----
            item {
                Card(title = stringResource(R.string.section_animations)) {
                    SwitchRow(
                        title = stringResource(R.string.animations_enabled),
                        checked = a.animationsEnabled,
                        subtitle = stringResource(R.string.animations_desc),
                    ) { on -> update { it.copy(animationsEnabled = on) } }
                }
            }

            // ---- Weather ----
            item {
                val w = settings.weather
                Card(title = stringResource(R.string.section_weather), subtitle = stringResource(R.string.weather_desc)) {
                    SwitchRow(stringResource(R.string.weather_show), w.enabled) { vm.setWeatherEnabled(it) }
                    if (w.enabled) {
                        Text(
                            if (w.hasLocation) stringResource(R.string.weather_city_current, w.cityName ?: "")
                            else stringResource(R.string.weather_no_city),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accent,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cityQuery,
                            onValueChange = { cityQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.weather_city_search)) },
                            trailingIcon = {
                                IconButton(onClick = { vm.searchCity(cityQuery) }) {
                                    Icon(Icons.Rounded.Search, stringResource(R.string.action_search))
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { vm.searchCity(cityQuery) }),
                        )
                        when (val s = citySearch) {
                            CitySearchState.Idle -> Unit
                            CitySearchState.Loading -> Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = palette.accent, modifier = Modifier.size(28.dp))
                            }
                            CitySearchState.Failed -> Text(
                                stringResource(R.string.weather_search_failed),
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color(0xFFFF8090),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            is CitySearchState.Results -> {
                                if (s.cities.isEmpty()) {
                                    Text(
                                        stringResource(R.string.weather_search_empty),
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                s.cities.forEach { city ->
                                    Text(
                                        city.displayName,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                cityQuery = ""
                                                vm.chooseCity(city)
                                            }
                                            .padding(vertical = 10.dp),
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ChipRow(
                            options = TemperatureUnit.entries,
                            selected = w.unit,
                            label = { if (it == TemperatureUnit.CELSIUS) "°C" else "°F" },
                            onSelect = { vm.setTemperatureUnit(it) },
                        )
                        if (w.hasLocation) {
                            TextButton(onClick = { vm.clearCity() }) { Text(stringResource(R.string.weather_clear_city)) }
                        }
                    }
                }
            }

            // ---- Notification badges ----
            item {
                Card(title = stringResource(R.string.section_badges), subtitle = stringResource(R.string.badges_desc)) {
                    SwitchRow(stringResource(R.string.badges_show), settings.badgesEnabled) { on ->
                        vm.setBadgesEnabled(on)
                        if (on && !badgeAccessGranted) bridge.openNotificationAccess()
                    }
                    if (settings.badgesEnabled && !badgeAccessGranted) {
                        Text(
                            stringResource(R.string.badges_access_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC46B),
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedNeonButton(stringResource(R.string.badges_grant), bridge::openNotificationAccess)
                    }
                }
            }

            // ---- Hidden apps ----
            item {
                Card(
                    title = stringResource(R.string.section_hidden),
                    subtitle = pluralStringResource(R.plurals.hidden_count, hiddenApps.size, hiddenApps.size),
                ) {
                    Text(stringResource(R.string.hidden_desc), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
                }
            }
            items(hiddenApps, key = { "h:" + it.key.flatten() }) { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(18.dp), palette)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(app, 36, a.iconShape)
                    Spacer(Modifier.width(12.dp))
                    Text(app.label, modifier = Modifier.weight(1f), color = Color.White)
                    IconButton(onClick = { vm.unhide(app) }) {
                        Icon(Icons.Rounded.Visibility, stringResource(R.string.action_unhide), tint = palette.accent)
                    }
                }
            }

            // ---- Backup ----
            item {
                Card(title = stringResource(R.string.section_backup), subtitle = stringResource(R.string.backup_desc)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeonButton(stringResource(R.string.backup_export), bridge::exportBackup)
                        OutlinedNeonButton(stringResource(R.string.backup_import), bridge::importBackup)
                    }
                }
            }

            // ---- Layout ----
            item {
                Card(title = stringResource(R.string.section_layout), subtitle = stringResource(R.string.layout_desc)) {
                    OutlinedNeonButton(stringResource(R.string.layout_reset)) { resetLayoutOpen = true }
                }
            }

            // ---- About ----
            item {
                Card(title = stringResource(R.string.section_about)) {
                    Text("NEXORA OS ${BuildConfig.VERSION_NAME}", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.about_brand), color = palette.accent, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.about_weather_credit), color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (saveProfileOpen) {
        TextInputDialog(
            title = stringResource(R.string.profile_save_current),
            initial = "",
            confirmLabel = stringResource(R.string.action_save),
            allowEmpty = false,
            onConfirm = {
                vm.saveProfile(it)
                saveProfileOpen = false
            },
            onDismiss = { saveProfileOpen = false },
        )
    }

    if (resetLayoutOpen) {
        ConfirmDialog(
            title = stringResource(R.string.layout_reset),
            message = stringResource(R.string.layout_reset_message),
            confirmLabel = stringResource(R.string.action_reset),
            onConfirm = {
                vm.resetHomeLayout()
                resetLayoutOpen = false
            },
            onDismiss = { resetLayoutOpen = false },
        )
    }
}

@Composable
private fun BrandHeader() {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "NEXORA OS",
            style = MaterialTheme.typography.headlineMedium
                .copy(fontWeight = FontWeight.Bold, letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing * 4)
                .merge(neonGlow(palette.accent)),
            color = Color.White,
        )
        Text(
            stringResource(R.string.slogan),
            style = MaterialTheme.typography.labelLarge,
            color = palette.accent,
        )
    }
}

@Composable
private fun Card(
    title: String? = null,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp), palette)
            .padding(18.dp),
    ) {
        if (title != null) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = palette.accent)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
            }
            Spacer(Modifier.height(12.dp))
        }
        content()
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = palette.accent, checkedThumbColor = Color.Black),
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    val palette = LocalPalette.current
    // Local state keeps the thumb smooth; the value is saved when the finger lifts.
    var local by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row {
            Text(title, modifier = Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, color = palette.accent, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            valueRange = range,
            steps = steps.coerceAtLeast(0),
            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
        )
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            NeonChip(selected = option == selected, label = label(option)) { onSelect(option) }
        }
    }
}

@Composable
private fun NeonButton(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
        shape = RoundedCornerShape(50),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun OutlinedNeonButton(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
    ) { Text(text) }
}

private fun backgroundLabel(mode: BackgroundMode) = when (mode) {
    BackgroundMode.NEXORA_GRADIENT -> R.string.background_nexora
    BackgroundMode.SYSTEM_WALLPAPER -> R.string.background_wallpaper
    BackgroundMode.OLED_BLACK -> R.string.background_oled
}

private fun iconShapeLabel(shape: IconShape) = when (shape) {
    IconShape.SYSTEM -> R.string.icon_shape_system
    IconShape.CIRCLE -> R.string.icon_shape_circle
    IconShape.SQUIRCLE -> R.string.icon_shape_squircle
    IconShape.ROUNDED_SQUARE -> R.string.icon_shape_rounded
}

private fun clockLabel(style: ClockStyle) = when (style) {
    ClockStyle.LARGE -> R.string.clock_large
    ClockStyle.NEON -> R.string.clock_neon
    ClockStyle.MINIMAL -> R.string.clock_minimal
}
