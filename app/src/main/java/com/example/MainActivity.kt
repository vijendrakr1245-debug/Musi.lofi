package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FullScreenNowPlayingOverlay
import com.example.ui.components.LofiWaveSplashOverlay
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LofiStudioScreen
import com.example.ui.screens.MusicConverterScreen
import com.example.ui.screens.MusicMoodScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.MidnightCharcoal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.viewmodel.LofiWaveViewModel
import kotlinx.coroutines.delay

enum class LofiNavTab(val label: String, val icon: ImageVector, val testTag: String) {
    HOME("Home", Icons.Default.Home, "nav_tab_home"),
    LIBRARY("Library", Icons.Default.LibraryMusic, "nav_tab_library"),
    MUSIC_CONVERTER("Studio", Icons.Default.GraphicEq, "nav_tab_converter"),
    THEMES("Themes", Icons.Default.Palette, "nav_tab_themes"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_tab_settings"),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic, "nav_tab_playlists"),
    LOFI_STUDIO("Lo-Fi Studio", Icons.Default.Tune, "nav_tab_studio"),
    EQUALIZER("Equalizer", Icons.Default.Equalizer, "nav_tab_equalizer"),
    MUSIC_MOOD("Music Mood", Icons.Default.AutoAwesome, "nav_tab_mood")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LofiWaveViewModel = viewModel()
            val accentTheme by viewModel.accentTheme.collectAsStateWithLifecycle()
            val pureAmoled by viewModel.pureAmoledBlack.collectAsStateWithLifecycle()

            MyApplicationTheme(
                accentTheme = accentTheme,
                pureAmoledBlack = pureAmoled
            ) {
                LofiWaveAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LofiWaveAppRoot(viewModel: LofiWaveViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(LofiNavTab.HOME) }
    var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
    var showSplashOverlay by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1150L)
        showSplashOverlay = false
    }

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredLibraryTracks.collectAsStateWithLifecycle()
    val convertedTracks by viewModel.convertedTracks.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteTrackIds.collectAsStateWithLifecycle()
    val history by viewModel.playbackHistory.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val presets by viewModel.allLofiPresets.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()

    val dspParams by viewModel.dspParams.collectAsStateWithLifecycle()
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()
    val visualizerSnapshot by viewModel.visualizerFlow.collectAsStateWithLifecycle()
    val visualizerMode by viewModel.visualizerMode.collectAsStateWithLifecycle()

    val selectedConverterCategory by viewModel.selectedConverterCategory.collectAsStateWithLifecycle()
    val selectedConverterPreset by viewModel.selectedConverterPreset.collectAsStateWithLifecycle()
    val converterIntensity by viewModel.converterIntensity.collectAsStateWithLifecycle()
    val isBeforeOriginalPreview by viewModel.isConverterBeforeOriginalPreview.collectAsStateWithLifecycle()
    val selectedExportQuality by viewModel.selectedExportQuality.collectAsStateWithLifecycle()
    val exportProgressState by viewModel.exportProgressState.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val queue by viewModel.playbackQueue.collectAsStateWithLifecycle()
    val sleepTimerSec by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedLibraryTab by viewModel.selectedLibraryTab.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val accentTheme by viewModel.accentTheme.collectAsStateWithLifecycle()
    val pureAmoled by viewModel.pureAmoledBlack.collectAsStateWithLifecycle()
    val highQualityDecoding by viewModel.highQualityDecoding.collectAsStateWithLifecycle()
    val audioPlaybackQuality by viewModel.audioPlaybackQuality.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val moodResult by viewModel.moodAnalysisResult.collectAsStateWithLifecycle()
    val isAnalyzingMood by viewModel.isAnalyzingMood.collectAsStateWithLifecycle()

    // Auto-dismiss status toast banner after 3.2 seconds
    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3200L)
            viewModel.postBannerMessage(null)
        }
    }

    // Android 13+ notification permission launcher for foreground media service
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // MediaStore audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.scanDeviceMediaStore()
        } else {
            viewModel.postBannerMessage("Use 'Import Songs' to pick audio files directly without storage permission.")
        }
    }

    val triggerMediaStoreScan = {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        audioPermissionLauncher.launch(perm)
    }

    // Android Storage Access Framework (SAF) Multiple Audio Documents Picker
    val openMultipleAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importSongsFromSaf(uris)
        }
    }

    // Android Storage Access Framework (SAF) Folder Picker
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        viewModel.importFolderFromSaf(treeUri)
    }

    val launchSafSongPicker = {
        openMultipleAudioLauncher.launch(arrayOf("audio/*"))
    }

    val launchSafFolderPicker = {
        openFolderLauncher.launch(null)
    }

    // Back handler for non-Home tabs
    if (currentTab != LofiNavTab.HOME && !isNowPlayingExpanded) {
        BackHandler {
            currentTab = LofiNavTab.HOME
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Persistent Mini Player Bar
                    currentTrack?.let { activeTrack ->
                        MiniPlayerBar(
                            track = activeTrack,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosMs,
                            durationMs = durationMs,
                            dspParams = dspParams,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() },
                            onToggleLofiMode = { enabled -> viewModel.setLiveLofiMode(enabled) },
                            onExpandNowPlaying = { isNowPlayingExpanded = true }
                        )
                    }

                    // 5-Tab Bottom Navigation Bar (Home, Library, Studio, Themes, Settings)
                    val bottomNavTabs = listOf(
                        LofiNavTab.HOME,
                        LofiNavTab.LIBRARY,
                        LofiNavTab.MUSIC_CONVERTER,
                        LofiNavTab.THEMES,
                        LofiNavTab.SETTINGS
                    )
                    NavigationBar(
                        containerColor = MidnightCharcoal,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavTabs.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = ElectricPurple.copy(alpha = 0.24f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                androidx.compose.animation.Crossfade(
                    targetState = currentTab,
                    label = "screen_crossfade"
                ) { activeTab ->
                    when (activeTab) {
                        LofiNavTab.HOME -> {
                            HomeScreen(
                                allTracks = allTracks,
                                favoriteIds = favoriteIds,
                                history = history,
                                presets = presets,
                                dspParams = dspParams,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                currentPositionMs = currentPosMs,
                                durationMs = durationMs,
                                moodResult = moodResult,
                                onPlayTrack = { track ->
                                    viewModel.playTrack(track)
                                    isNowPlayingExpanded = true
                                },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onSkipNext = { viewModel.skipNext() },
                                onSeekTo = { pos -> viewModel.seekToPosition(pos) },
                                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                onSelectPreset = { preset -> viewModel.selectLofiPreset(preset) },
                                onToggleLofiMode = { enabled -> viewModel.setLiveLofiMode(enabled) },
                                onApplyAiRecommendation = { rec -> viewModel.applyAiMoodRecommendation(rec) },
                                onOpenMoodScreen = { currentTab = LofiNavTab.MUSIC_MOOD },
                                onApplyMoodPreset = { preset -> viewModel.applyMoodSuggestedPreset(preset) },
                                onImportSongsClick = launchSafSongPicker,
                                onScanDeviceClick = triggerMediaStoreScan,
                                onNavigateToStudio = { currentTab = LofiNavTab.LOFI_STUDIO },
                                onOpenSettings = { currentTab = LofiNavTab.SETTINGS },
                                onOpenLibraryCollection = { filterTab ->
                                    viewModel.setSelectedLibraryTab(filterTab)
                                    currentTab = LofiNavTab.LIBRARY
                                },
                                onExpandNowPlaying = { isNowPlayingExpanded = true },
                                onSelectSoundMode = { mode -> viewModel.setSoundMode(mode) },
                                onOpenMusicConverter = { currentTab = LofiNavTab.MUSIC_CONVERTER },
                                onOpenThemes = { currentTab = LofiNavTab.THEMES }
                            )
                        }
                        LofiNavTab.MUSIC_CONVERTER -> {
                            MusicConverterScreen(
                                currentTrack = currentTrack,
                                allTracks = allTracks,
                                convertedTracks = convertedTracks,
                                isPlaying = isPlaying,
                                dspParams = dspParams,
                                visualizerSnapshot = visualizerSnapshot,
                                visualizerMode = visualizerMode,
                                selectedCategory = selectedConverterCategory,
                                selectedPreset = selectedConverterPreset,
                                converterIntensity = converterIntensity,
                                isBeforeOriginalPreview = isBeforeOriginalPreview,
                                selectedExportQuality = selectedExportQuality,
                                exportProgressState = exportProgressState,
                                onSelectCategory = { cat -> viewModel.selectConverterCategory(cat) },
                                onSelectPreset = { preset -> viewModel.selectConverterPreset(preset) },
                                onIntensityChange = { intensity -> viewModel.setConverterIntensity(intensity) },
                                onToggleBeforeAfterPreview = { before -> viewModel.toggleConverterBeforeAfterPreview(before) },
                                onSelectExportQuality = { q -> viewModel.setExportQuality(q) },
                                onStartExport = { viewModel.exportCurrentSongWithConverter() },
                                onCancelExport = { viewModel.cancelExportConversion() },
                                onPlayTrack = { track ->
                                    viewModel.playTrack(track)
                                    isNowPlayingExpanded = true
                                },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onImportAudioClick = launchSafSongPicker,
                                onSelectSoundMode = { mode -> viewModel.setSoundMode(mode) },
                                onSelectVisualizerMode = { vMode -> viewModel.setVisualizerMode(vMode) },
                                onUpdateDsp = { transform -> viewModel.updateDspParameter(transform) },
                                onOpenEqualizer = { currentTab = LofiNavTab.EQUALIZER },
                                onOpenStudio = { currentTab = LofiNavTab.LOFI_STUDIO },
                                onBack = { currentTab = LofiNavTab.HOME }
                            )
                        }
                        LofiNavTab.LIBRARY -> {
                            LibraryScreen(
                                allTracks = allTracks,
                                filteredTracks = filteredTracks,
                                favoriteIds = favoriteIds,
                                history = history,
                                playlists = playlists,
                                searchQuery = searchQuery,
                                selectedTab = selectedLibraryTab,
                                sortOrder = sortOrder,
                                currentTrack = currentTrack,
                                isPlaying = isPlaying,
                                onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                                onSelectTab = { tab -> viewModel.setSelectedLibraryTab(tab) },
                                onSelectSortOrder = { order -> viewModel.setSortOrder(order) },
                                onPlayTrack = { track, list ->
                                    viewModel.playTrack(track, list)
                                    isNowPlayingExpanded = true
                                },
                                onPlayPlaylist = { list, shuffle, repeat ->
                                    viewModel.playPlaylist(list, shuffle, repeat)
                                    isNowPlayingExpanded = true
                                },
                                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                onAddTrackToMultiplePlaylists = { tId, plIds -> viewModel.addTrackToMultiplePlaylists(tId, plIds) },
                                onCreatePlaylistWithTrack = { name, desc, ids -> viewModel.createPlaylist(name, desc, ids) },
                                onRemoveTrackFromLibrary = { track -> viewModel.removeTrackFromLibrary(track) },
                                onImportSongsClick = launchSafSongPicker,
                                onImportFolderClick = launchSafFolderPicker,
                                onScanDeviceClick = triggerMediaStoreScan
                            )
                        }
                        LofiNavTab.PLAYLISTS -> {
                            PlaylistsScreen(
                                playlists = playlists,
                                allTracks = allTracks,
                                favoriteIds = favoriteIds,
                                history = history,
                                onCreatePlaylist = { name, desc, ids -> viewModel.createPlaylist(name, desc, ids) },
                                onRenamePlaylist = { id, name, desc -> viewModel.renamePlaylist(id, name, desc) },
                                onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                onAddTrackToPlaylist = { plId, tId -> viewModel.addTrackToPlaylist(plId, tId) },
                                onRemoveTrackFromPlaylist = { plId, tId -> viewModel.removeTrackFromPlaylist(plId, tId) },
                                onReorderPlaylistTracks = { id, orderedIds -> viewModel.reorderPlaylistTracks(id, orderedIds) },
                                onPlayPlaylist = { list, shuffle, repeat ->
                                    viewModel.playPlaylist(list, shuffle, repeat)
                                    isNowPlayingExpanded = true
                                },
                                onPlaySingleTrackFromPlaylist = { track, list ->
                                    viewModel.playTrack(track, list)
                                    isNowPlayingExpanded = true
                                },
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }
                        LofiNavTab.LOFI_STUDIO -> {
                            LofiStudioScreen(
                                dspParams = dspParams,
                                presets = presets,
                                visualizerSnapshot = visualizerSnapshot,
                                isPlaying = isPlaying,
                                onToggleLofiMode = { enabled -> viewModel.setLiveLofiMode(enabled) },
                                onSelectPreset = { preset -> viewModel.selectLofiPreset(preset) },
                                onUpdateDsp = { transform -> viewModel.updateDspParameter(transform) },
                                onSaveCustomPreset = { name, sub, cat -> viewModel.saveCurrentAsCustomPreset(name, sub, cat) },
                                onDeleteCustomPreset = { id -> viewModel.deleteCustomLofiPreset(id) },
                                visualizerMode = visualizerMode,
                                onSelectVisualizerMode = { vMode -> viewModel.setVisualizerMode(vMode) },
                                onSelectSoundMode = { mode -> viewModel.setSoundMode(mode) },
                                onRenameCustomPreset = { id, name, sub -> viewModel.renameCustomLofiPreset(id, name, sub) },
                                onResetAudioEditor = { viewModel.resetAudioEditorToDefault() }
                            )
                        }
                        LofiNavTab.EQUALIZER -> {
                            EqualizerScreen(
                                eqState = eqState,
                                customEqPresets = customEqPresets,
                                onUpdateEq = { transform -> viewModel.updateEqualizer(transform) },
                                onUpdateBand = { idx, gain -> viewModel.updateEqBand(idx, gain) },
                                onSelectBuiltInPreset = { name, gains -> viewModel.selectBuiltInEqPreset(name, gains) },
                                onSaveCustomEqPreset = { name -> viewModel.saveCustomEqPreset(name) },
                                onApplyBassEnhancementPreset = { presetId -> viewModel.applyQuickBassEnhancementPreset(presetId) },
                                onResetAllEffects = { viewModel.resetAudioEditorToDefault() }
                            )
                        }
                        LofiNavTab.MUSIC_MOOD -> {
                            MusicMoodScreen(
                                currentTrack = currentTrack ?: allTracks.firstOrNull(),
                                moodResult = moodResult,
                                isAnalyzing = isAnalyzingMood,
                                dspParams = dspParams,
                                onAnalyzeAgain = { viewModel.analyzeCurrentTrackMood(manualTrigger = true) },
                                onApplyPreset = { preset -> viewModel.applyMoodSuggestedPreset(preset) },
                                onIgnoreSuggestion = { viewModel.ignoreMoodSuggestion() },
                                onBack = { currentTab = LofiNavTab.HOME }
                            )
                        }
                        LofiNavTab.THEMES -> {
                            com.example.ui.screens.ThemesScreen(
                                accentTheme = accentTheme,
                                pureAmoledBlack = pureAmoled,
                                highQualityDecoding = highQualityDecoding,
                                onUpdateThemePrefs = { th, amoled, hq -> viewModel.updateThemePreferences(th, amoled, hq) }
                            )
                        }
                        LofiNavTab.SETTINGS -> {
                            SettingsScreen(
                                accentTheme = accentTheme,
                                pureAmoledBlack = pureAmoled,
                                highQualityDecoding = highQualityDecoding,
                                sleepTimerRemainingSec = sleepTimerSec,
                                eqState = eqState,
                                audioPlaybackQuality = audioPlaybackQuality,
                                currentTrack = currentTrack ?: allTracks.firstOrNull(),
                                onSelectAudioPlaybackQuality = { q -> viewModel.setAudioPlaybackQuality(q) },
                                onUpdateEq = { transform -> viewModel.updateEqualizer(transform) },
                                onUpdateThemePrefs = { th, amoled, hq -> viewModel.updateThemePreferences(th, amoled, hq) },
                                onSetSleepTimerMinutes = { mins -> viewModel.startSleepTimer(mins) },
                                onImportSongsClick = launchSafSongPicker,
                                onImportFolderClick = launchSafFolderPicker,
                                onScanDeviceClick = triggerMediaStoreScan,
                                onClearHistory = { viewModel.clearHistory() },
                                onOpenStudio = { currentTab = LofiNavTab.LOFI_STUDIO },
                                onOpenEqualizer = { currentTab = LofiNavTab.EQUALIZER },
                                onOpenMusicConverter = { currentTab = LofiNavTab.MUSIC_CONVERTER },
                                onOpenThemes = { currentTab = LofiNavTab.THEMES }
                            )
                        }
                    }
                }

                // Floating Status Toast Banner
                AnimatedVisibility(
                    visible = statusBanner != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 20.dp, end = 20.dp)
                ) {
                    Surface(
                        color = ElevatedCharcoal.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(50),
                        tonalElevation = 12.dp
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Full-Screen Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            currentTrack?.let { activeTrack ->
                FullScreenNowPlayingOverlay(
                    track = activeTrack,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPosMs,
                    durationMs = durationMs,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = activeTrack.id in favoriteIds,
                    dspParams = dspParams,
                    visualizerSnapshot = visualizerSnapshot,
                    queue = queue,
                    sleepTimerRemainingSec = sleepTimerSec,
                    onClose = { isNowPlayingExpanded = false },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipPrevious = { viewModel.skipPrevious() },
                    onSkipNext = { viewModel.skipNext() },
                    onSeekTo = { pos -> viewModel.seekToPosition(pos) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onToggleFavorite = { viewModel.toggleFavorite(activeTrack.id) },
                    onToggleLofiMode = { enabled -> viewModel.setLiveLofiMode(enabled) },
                    onSpeedChange = { spd ->
                        viewModel.updateDspParameter { it.copy(playbackSpeed = spd) }
                    },
                    onSelectQueueTrack = { qTrack -> viewModel.playTrack(qTrack, queue) },
                    onSetSleepTimerMinutes = { mins -> viewModel.startSleepTimer(mins) },
                    onOpenLofiStudio = { currentTab = LofiNavTab.LOFI_STUDIO },
                    onOpenEqualizer = { currentTab = LofiNavTab.EQUALIZER },
                    onOpenMoodScreen = { currentTab = LofiNavTab.MUSIC_MOOD },
                    onSelectSoundMode = { mode -> viewModel.setSoundMode(mode) },
                    onOpenMusicConverter = { currentTab = LofiNavTab.MUSIC_CONVERTER }
                )
            }
        }

        // Animated Premium Splash Screen Overlay on App Launch
        AnimatedVisibility(
            visible = showSplashOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LofiWaveSplashOverlay(
                onDismiss = { showSplashOverlay = false }
            )
        }
    }
}
