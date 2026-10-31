package com.example.viewmodel

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.audio.AudioEngineBridge
import com.example.audio.StudioWavGenerator
import com.example.data.CustomEqPresetEntity
import com.example.data.CustomLofiPresetEntity
import com.example.data.DeviceMusicScanner
import com.example.data.FavoriteTrackEntity
import com.example.data.ImportedTrackEntity
import com.example.data.LofiDatabase
import com.example.data.PlaybackHistoryEntity
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.UserPreferencesRepository
import com.example.model.AiMoodRecommendation
import com.example.model.BuiltInLofiPresets
import com.example.model.EqualizerState
import com.example.model.LibraryFilterTab
import com.example.model.LibrarySortOrder
import com.example.model.LofiDspParameters
import com.example.model.LofiPreset
import com.example.model.TrackItem
import com.example.model.VisualizerSnapshot
import com.example.service.LofiPlaybackService
import com.example.ui.theme.AccentTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaylistUiModel(
    val playlist: PlaylistEntity,
    val tracks: List<TrackItem>
)

class LofiWaveViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = LofiDatabase.getInstance(appContext)
    private val dao = database.lofiDao()
    private val prefsRepo = UserPreferencesRepository(appContext)

    private val player = AudioEngineBridge.getOrCreatePlayer(appContext)
    private var mediaController: MediaController? = null

    // Real-time audio visualizer flow directly from PCM DSP processor
    val visualizerFlow: StateFlow<VisualizerSnapshot> = AudioEngineBridge.dspProcessor.visualizerFlow

    // Device & Studio tracks
    private val _studioTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    private val _mediaStoreTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    private val _importedTracksFlow = dao.getAllImportedTracks()

    // Search, Filter, Sort
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLibraryTab = MutableStateFlow(LibraryFilterTab.ALL_SONGS)
    val selectedLibraryTab: StateFlow<LibraryFilterTab> = _selectedLibraryTab.asStateFlow()

    private val _sortOrder = MutableStateFlow(LibrarySortOrder.RECENTLY_ADDED)
    val sortOrder: StateFlow<LibrarySortOrder> = _sortOrder.asStateFlow()

    // Playback state
    private val _currentTrack = MutableStateFlow<TrackItem?>(null)
    val currentTrack: StateFlow<TrackItem?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<TrackItem>>(emptyList())
    val playbackQueue: StateFlow<List<TrackItem>> = _playbackQueue.asStateFlow()

    // DSP & Equalizer State
    private val _dspParams = MutableStateFlow(LofiDspParameters())
    val dspParams: StateFlow<LofiDspParameters> = _dspParams.asStateFlow()

    private val _eqState = MutableStateFlow(EqualizerState())
    val eqState: StateFlow<EqualizerState> = _eqState.asStateFlow()

    // Theme & Preferences
    private val _accentTheme = MutableStateFlow(AccentTheme.ELECTRIC_PURPLE)
    val accentTheme: StateFlow<AccentTheme> = _accentTheme.asStateFlow()

    private val _pureAmoledBlack = MutableStateFlow(true)
    val pureAmoledBlack: StateFlow<Boolean> = _pureAmoledBlack.asStateFlow()

    private val _highQualityDecoding = MutableStateFlow(true)
    val highQualityDecoding: StateFlow<Boolean> = _highQualityDecoding.asStateFlow()

    // Sleep Timer
    private val _sleepTimerRemainingSec = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSec: StateFlow<Int?> = _sleepTimerRemainingSec.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Status message / snackbar banner
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Favorites IDs set
    val favoriteTrackIds: StateFlow<Set<String>> = dao.getAllFavorites()
        .combine(MutableStateFlow(Unit)) { list, _ -> list.map { it.trackId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Playback History
    val playbackHistory: StateFlow<List<PlaybackHistoryEntity>> = dao.getPlaybackHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined & Deduplicated All Tracks (Imported SAF + MediaStore + Studio WAV Masters)
    val allTracks: StateFlow<List<TrackItem>> = combine(
        _importedTracksFlow,
        _mediaStoreTracks,
        _studioTracks
    ) { importedEntities, mediaStore, studio ->
        val importedItems = importedEntities.map { it.toTrackItem() }
        val mapByUri = linkedMapOf<String, TrackItem>()
        val seenSignatures = hashSetOf<String>()

        fun addIfUnique(item: TrackItem) {
            val normUri = item.uriString.trim()
            val sig = "${item.title.trim().lowercase()}|${item.artist.trim().lowercase()}|${item.durationMs / 1000}"
            if (normUri !in mapByUri && sig !in seenSignatures) {
                mapByUri[normUri] = item
                seenSignatures.add(sig)
            }
        }

        for (item in importedItems) addIfUnique(item)
        for (item in mediaStore) addIfUnique(item)
        for (item in studio) addIfUnique(item)
        mapByUri.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Library Tracks
    val filteredLibraryTracks: StateFlow<List<TrackItem>> = combine(
        allTracks,
        _searchQuery,
        _selectedLibraryTab,
        _sortOrder,
        favoriteTrackIds
    ) { tracks, query, tab, sort, favIds ->
        var list = when (tab) {
            LibraryFilterTab.FAVORITES -> tracks.filter { it.id in favIds }
            LibraryFilterTab.RECENTLY_ADDED -> tracks.sortedByDescending { it.dateAddedMillis }
            else -> tracks
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q)
            }
        }
        when (sort) {
            LibrarySortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            LibrarySortOrder.ARTIST -> list.sortedBy { it.artist.lowercase() }
            LibrarySortOrder.RECENTLY_ADDED -> list.sortedByDescending { it.dateAddedMillis }
            LibrarySortOrder.DURATION -> list.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Lo-Fi Presets (Built-in 12 + User Custom Presets)
    val allLofiPresets: StateFlow<List<LofiPreset>> = dao.getCustomLofiPresets()
        .combine(MutableStateFlow(BuiltInLofiPresets.ALL)) { customEntities, builtIn ->
            customEntities.map { it.toLofiPreset() } + builtIn
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BuiltInLofiPresets.ALL)

    // Custom EQ Presets
    val customEqPresets: StateFlow<List<CustomEqPresetEntity>> = dao.getCustomEqPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playlists with resolved tracks ordered by sortIndex ASC
    val playlists: StateFlow<List<PlaylistUiModel>> = combine(
        dao.getAllPlaylists(),
        dao.getAllPlaylistTrackLinks(),
        allTracks
    ) { playlistEntities, links, tracks ->
        val tracksById = tracks.associateBy { it.id }
        playlistEntities.map { pl ->
            val orderedLinks = links
                .filter { it.playlistId == pl.playlistId }
                .sortedWith(compareBy<PlaylistTrackCrossRef> { it.sortIndex }.thenBy { it.addedAtMillis })
            val resolved = orderedLinks.mapNotNull { tracksById[it.trackId] }
            PlaylistUiModel(playlist = pl, tracks = resolved)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                persistCurrentPlaybackState()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _durationMs.value = player.duration.coerceAtLeast(0L)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId ?: return
            val track = allTracks.value.find { it.id == mediaId }
                ?: _playbackQueue.value.find { it.id == mediaId }
            if (track != null) {
                _currentTrack.value = track
                _durationMs.value = player.duration.coerceAtLeast(track.durationMs)
                _currentPositionMs.value = 0L
                recordTrackPlayed(track)
                viewModelScope.launch {
                    prefsRepo.saveLastPlayedTrackState(track.id, 0L)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            postBannerMessage("Skipped inaccessible audio file (${error.errorCodeName})")
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }

    init {
        player.addListener(playerListener)
        initializeDataAndEngine()
        startPositionPolling()
        bindForegroundMediaService()
    }

    private fun hasAudioReadPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun initializeDataAndEngine() {
        viewModelScope.launch {
            // 1. Restore saved preferences from DataStore
            val savedPrefs = prefsRepo.preferencesFlow.first()
            _accentTheme.value = savedPrefs.accentTheme
            _pureAmoledBlack.value = savedPrefs.pureAmoledBlack
            _highQualityDecoding.value = savedPrefs.highQualityDecoding
            _dspParams.value = savedPrefs.dspParams
            _eqState.value = savedPrefs.eqState

            AudioEngineBridge.applyLofiParameters(savedPrefs.dspParams)
            AudioEngineBridge.applyEqualizerState(savedPrefs.eqState)

            // 2. Ensure built-in studio WAV tracks exist on device storage
            val studio = StudioWavGenerator.ensureStudioTracksGenerated(appContext)
            _studioTracks.value = studio

            // 3. Validate persisted SAF tracks (gracefully prune deleted/inaccessible files)
            val savedImported = dao.getAllImportedTracks().first()
            for (entity in savedImported) {
                val item = entity.toTrackItem()
                if (!DeviceMusicScanner.isTrackUriAccessible(appContext, item)) {
                    dao.deleteImportedTrack(entity.id)
                }
            }

            // 4. Automatically scan accessible local device music if permission is granted
            if (hasAudioReadPermission()) {
                val discovered = DeviceMusicScanner.scanMediaStoreAudio(appContext)
                _mediaStoreTracks.value = discovered
                if (discovered.isNotEmpty()) {
                    persistDiscoveredTracksIfNew(discovered)
                }
            }

            // 5. Create default starter playlist if none exists yet
            val existingPlaylists = dao.getAllPlaylists().first()
            if (existingPlaylists.isEmpty() && studio.isNotEmpty()) {
                val plId = dao.insertPlaylist(
                    PlaylistEntity(
                        name = "Late Night Lo-Fi Sessions",
                        description = "Curated chillhop & rainy night tracks for live DSP processing"
                    )
                )
                studio.forEachIndexed { idx, track ->
                    dao.addTrackToPlaylist(
                        PlaylistTrackCrossRef(playlistId = plId, trackId = track.id, sortIndex = idx)
                    )
                }
            }

            // 6. Restore last played song and exact playback position
            delay(150)
            val currentAll = allTracks.value
            if (_currentTrack.value == null && currentAll.isNotEmpty()) {
                val lastId = savedPrefs.lastPlayedTrackId
                val restoredIndex = if (lastId != null) {
                    currentAll.indexOfFirst { it.id == lastId }.coerceAtLeast(0)
                } else {
                    0
                }
                val restoredPos = if (lastId != null && currentAll.getOrNull(restoredIndex)?.id == lastId) {
                    savedPrefs.lastPlayedPositionMs.coerceAtLeast(0L)
                } else {
                    0L
                }
                loadQueueAndPrepare(
                    queue = currentAll,
                    startIndex = restoredIndex,
                    initialPositionMs = restoredPos,
                    playImmediately = false
                )
            }
        }
    }

    private suspend fun persistDiscoveredTracksIfNew(tracks: List<TrackItem>) {
        val existing = dao.getAllImportedTracks().first()
        val existingUris = existing.map { it.uriString }.toHashSet()
        val existingSigs = existing.map {
            "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}|${it.durationMs / 1000}"
        }.toHashSet()

        val toInsert = tracks.filter { t ->
            val sig = "${t.title.trim().lowercase()}|${t.artist.trim().lowercase()}|${t.durationMs / 1000}"
            t.uriString !in existingUris && sig !in existingSigs
        }.map { t ->
            ImportedTrackEntity(
                id = t.id,
                uriString = t.uriString,
                title = t.title,
                artist = t.artist,
                album = t.album,
                durationMs = t.durationMs,
                formatBadge = t.formatBadge,
                folderName = t.folderName,
                dateAddedMillis = t.dateAddedMillis,
                albumArtUri = t.albumArtUri,
                lyricsOrNotes = t.lyricsOrNotes
            )
        }

        if (toInsert.isNotEmpty()) {
            dao.insertImportedTracks(toInsert)
        }
    }

    private fun bindForegroundMediaService() {
        runCatching {
            val sessionToken = SessionToken(
                appContext,
                ComponentName(appContext, LofiPlaybackService::class.java)
            )
            val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
            controllerFuture.addListener(
                {
                    runCatching {
                        mediaController = controllerFuture.get()
                    }
                },
                ContextCompat.getMainExecutor(appContext)
            )
        }
    }

    private fun ensureServiceStarted() {
        runCatching {
            val intent = Intent(appContext, LofiPlaybackService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            var tickCounter = 0
            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    val dur = player.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                    tickCounter++
                    if (tickCounter >= 10) { // Persist position every ~2 seconds
                        tickCounter = 0
                        _currentTrack.value?.let { active ->
                            prefsRepo.saveLastPlayedTrackState(active.id, pos)
                        }
                    }
                }
                delay(200L)
            }
        }
    }

    private fun persistCurrentPlaybackState() {
        val active = _currentTrack.value ?: return
        val pos = _currentPositionMs.value
        viewModelScope.launch {
            prefsRepo.saveLastPlayedTrackState(active.id, pos)
        }
    }

    // =========================================================================
    // PLAYBACK & QUEUE CONTROLS
    // =========================================================================

    fun playTrack(track: TrackItem, customQueue: List<TrackItem>? = null) {
        viewModelScope.launch {
            if (!DeviceMusicScanner.isTrackUriAccessible(appContext, track)) {
                postBannerMessage("File '${track.title}' was moved or deleted from device storage.")
                dao.deleteImportedTrack(track.id)
                return@launch
            }
            ensureServiceStarted()
            val queueToUse = customQueue?.takeIf { it.isNotEmpty() }
                ?: allTracks.value.ifEmpty { listOf(track) }
            val index = queueToUse.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            loadQueueAndPrepare(queueToUse, index, initialPositionMs = 0L, playImmediately = true)
            recordTrackPlayed(track)
            prefsRepo.saveLastPlayedTrackState(track.id, 0L)
        }
    }

    /**
     * Plays an entire playlist with optional Shuffle and Repeat modes.
     */
    fun playPlaylist(tracks: List<TrackItem>, shuffle: Boolean = false, repeatAll: Boolean = true) {
        if (tracks.isEmpty()) {
            postBannerMessage("Playlist is empty. Add songs to play.")
            return
        }
        viewModelScope.launch {
            ensureServiceStarted()
            val orderedQueue = if (shuffle) tracks.shuffled() else tracks
            _shuffleEnabled.value = shuffle
            player.shuffleModeEnabled = shuffle

            val targetRepeat = if (repeatAll) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            _repeatMode.value = targetRepeat
            player.repeatMode = targetRepeat

            loadQueueAndPrepare(orderedQueue, startIndex = 0, initialPositionMs = 0L, playImmediately = true)
            val first = orderedQueue.first()
            recordTrackPlayed(first)
            prefsRepo.saveLastPlayedTrackState(first.id, 0L)
            postBannerMessage(
                if (shuffle) "Shuffling playlist (${tracks.size} songs)"
                else "Playing playlist (${tracks.size} songs)"
            )
        }
    }

    private fun loadQueueAndPrepare(
        queue: List<TrackItem>,
        startIndex: Int,
        initialPositionMs: Long = 0L,
        playImmediately: Boolean
    ) {
        if (queue.isEmpty()) return
        _playbackQueue.value = queue
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        val selectedTrack = queue[safeIndex]
        _currentTrack.value = selectedTrack
        _durationMs.value = selectedTrack.durationMs
        _currentPositionMs.value = initialPositionMs.coerceIn(0L, selectedTrack.durationMs)

        val mediaItems = queue.map { track ->
            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
            track.albumArtUri?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.uriString))
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }

        player.setMediaItems(mediaItems, safeIndex, initialPositionMs)
        player.prepare()
        AudioEngineBridge.applyLofiParameters(_dspParams.value)
        AudioEngineBridge.applyEqualizerState(_eqState.value)
        if (playImmediately) {
            player.play()
        }
    }

    fun togglePlayPause() {
        ensureServiceStarted()
        if (player.mediaItemCount == 0) {
            val all = allTracks.value
            if (all.isNotEmpty()) {
                loadQueueAndPrepare(all, 0, initialPositionMs = 0L, playImmediately = true)
            }
            return
        }
        if (player.isPlaying) {
            player.pause()
            persistCurrentPlaybackState()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun skipNext() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.mediaItemCount > 0) {
            player.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        if (player.currentPosition > 3500L) {
            player.seekTo(0L)
            _currentPositionMs.value = 0L
            persistCurrentPlaybackState()
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (player.mediaItemCount > 0) {
            player.seekTo(player.mediaItemCount - 1, 0L)
        }
    }

    fun seekToPosition(positionMs: Long) {
        val dur = _durationMs.value.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, dur)
        player.seekTo(clamped)
        _currentPositionMs.value = clamped
        persistCurrentPlaybackState()
    }

    fun toggleShuffle() {
        val next = !_shuffleEnabled.value
        _shuffleEnabled.value = next
        player.shuffleModeEnabled = next
        postBannerMessage(if (next) "Shuffle Enabled" else "Shuffle Off")
    }

    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
        _repeatMode.value = nextMode
        player.repeatMode = nextMode
        val label = when (nextMode) {
            Player.REPEAT_MODE_ONE -> "Repeat One Song"
            Player.REPEAT_MODE_ALL -> "Repeat Playlist / Queue"
            else -> "Repeat Off"
        }
        postBannerMessage(label)
    }

    private fun recordTrackPlayed(track: TrackItem) {
        viewModelScope.launch {
            val existing = dao.getHistoryEntry(track.id)
            val newCount = (existing?.playCount ?: 0) + 1
            dao.upsertHistoryEntry(
                PlaybackHistoryEntity(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    uriString = track.uriString,
                    formatBadge = track.formatBadge,
                    durationMs = track.durationMs,
                    lastPlayedAtMillis = System.currentTimeMillis(),
                    playCount = newCount
                )
            )
        }
    }

    // =========================================================================
    // REAL-TIME LO-FI DSP & HI-FI SWITCHING (NO TRACK RESTART)
    // =========================================================================

    fun setLiveLofiMode(enabled: Boolean) {
        val updated = _dspParams.value.copy(isLiveLofiEnabled = enabled)
        _dspParams.value = updated
        AudioEngineBridge.applyLofiParameters(updated)
        viewModelScope.launch {
            prefsRepo.saveDspParameters(updated)
        }
        postBannerMessage(
            if (enabled) "Real-Time Live Lo-Fi Mode Active (${updated.presetName})"
            else "Original Hi-Fi Mode Active (Lo-Fi DSP Bypassed)"
        )
    }

    fun selectLofiPreset(preset: LofiPreset) {
        val updated = preset.parameters.copy(isLiveLofiEnabled = true)
        _dspParams.value = updated
        AudioEngineBridge.applyLofiParameters(updated)
        viewModelScope.launch {
            prefsRepo.saveDspParameters(updated)
        }
        postBannerMessage("Applied '${preset.name}' Real-Time DSP Preset")
    }

    fun updateDspParameter(transform: (LofiDspParameters) -> LofiDspParameters) {
        val current = _dspParams.value
        val updated = transform(current)
        _dspParams.value = updated
        AudioEngineBridge.applyLofiParameters(updated)
        viewModelScope.launch {
            prefsRepo.saveDspParameters(updated)
        }
    }

    fun saveCurrentAsCustomPreset(name: String, subtitle: String, moodCategory: String) {
        val cleanName = name.trim().ifEmpty { "My Custom Lo-Fi" }
        val id = "custom_${cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}_${System.currentTimeMillis() % 10000}"
        val p = _dspParams.value.copy(presetId = id, presetName = cleanName, isLiveLofiEnabled = true)
        _dspParams.value = p
        AudioEngineBridge.applyLofiParameters(p)

        viewModelScope.launch {
            dao.saveCustomLofiPreset(
                CustomLofiPresetEntity(
                    id = id,
                    name = cleanName,
                    subtitle = subtitle.ifBlank { "Custom user-engineered DSP chain" },
                    moodCategory = moodCategory.ifBlank { "Custom Studio" },
                    masterIntensity = p.masterIntensity,
                    lowPassCutoffHz = p.lowPassCutoffHz,
                    highPassCutoffHz = p.highPassCutoffHz,
                    bitDepth = p.bitDepth,
                    sampleRateDivisor = p.sampleRateDivisor,
                    analogSaturation = p.analogSaturation,
                    vinylCrackle = p.vinylCrackle,
                    vinylNoise = p.vinylNoise,
                    tapeHiss = p.tapeHiss,
                    wowFlutterDepth = p.wowFlutterDepth,
                    wowFlutterRateHz = p.wowFlutterRateHz,
                    warmBassBoostDb = p.warmBassBoostDb,
                    softDistortion = p.softDistortion,
                    reverbMix = p.reverbMix,
                    reverbRoomSize = p.reverbRoomSize,
                    stereoWidth = p.stereoWidth,
                    softCompression = p.softCompression,
                    highFreqReductionDb = p.highFreqReductionDb,
                    trebleGainDb = p.trebleGainDb,
                    playbackSpeed = p.playbackSpeed
                )
            )
            prefsRepo.saveDspParameters(p)
            postBannerMessage("Saved custom Lo-Fi preset '$cleanName'")
        }
    }

    fun deleteCustomLofiPreset(presetId: String) {
        viewModelScope.launch {
            dao.deleteCustomLofiPreset(presetId)
            postBannerMessage("Deleted custom preset")
        }
    }

    // =========================================================================
    // 10-BAND EQUALIZER & HIGH QUALITY AUDIO CONTROLS
    // =========================================================================

    fun updateEqualizer(transform: (EqualizerState) -> EqualizerState) {
        val updated = transform(_eqState.value)
        _eqState.value = updated
        AudioEngineBridge.applyEqualizerState(updated)
        viewModelScope.launch {
            prefsRepo.saveEqualizerState(updated)
        }
    }

    fun updateEqBand(bandIndex: Int, gainDb: Float) {
        val current = _eqState.value
        val mutableBands = current.bandGainsDb.toMutableList()
        if (bandIndex in mutableBands.indices) {
            mutableBands[bandIndex] = gainDb.coerceIn(-12f, 12f)
            updateEqualizer {
                it.copy(
                    presetName = "Custom Curve",
                    bandGainsDb = mutableBands
                )
            }
        }
    }

    fun selectBuiltInEqPreset(name: String, gains: List<Float>) {
        updateEqualizer {
            it.copy(
                isEnabled = true,
                presetName = name,
                bandGainsDb = gains
            )
        }
        postBannerMessage("Equalizer preset '$name' active")
    }

    fun saveCustomEqPreset(name: String) {
        val clean = name.trim().ifEmpty { "Custom EQ" }
        val current = _eqState.value
        viewModelScope.launch {
            dao.saveCustomEqPreset(
                CustomEqPresetEntity(
                    name = clean,
                    gainsCsv = current.bandGainsDb.joinToString(","),
                    bassBoostDb = current.bassBoostDb,
                    trebleBoostDb = current.trebleBoostDb
                )
            )
            updateEqualizer { it.copy(presetName = clean) }
            postBannerMessage("Saved custom EQ preset '$clean'")
        }
    }

    // =========================================================================
    // AI MUSIC ASSISTANT (MOOD & DSP ENGINE)
    // =========================================================================

    fun applyAiMoodRecommendation(rec: AiMoodRecommendation) {
        _dspParams.value = rec.customParameters.copy(isLiveLofiEnabled = true)
        AudioEngineBridge.applyLofiParameters(_dspParams.value)

        val eqGains = EqualizerState.BUILT_IN_EQ_PRESETS[rec.eqPresetName]
        if (eqGains != null) {
            _eqState.value = _eqState.value.copy(
                isEnabled = true,
                presetName = rec.eqPresetName,
                bandGainsDb = eqGains
            )
            AudioEngineBridge.applyEqualizerState(_eqState.value)
        }

        viewModelScope.launch {
            prefsRepo.saveDspParameters(_dspParams.value)
            prefsRepo.saveEqualizerState(_eqState.value)
        }
        postBannerMessage("AI Assistant applied '${rec.moodTitle}': ${rec.recommendedPresetName}")
    }

    // =========================================================================
    // DEVICE MUSIC ACCESS (SAF & MEDIASTORE) & DEDUPLICATION
    // =========================================================================

    fun scanDeviceMediaStore() {
        viewModelScope.launch {
            val found = DeviceMusicScanner.scanMediaStoreAudio(appContext)
            _mediaStoreTracks.value = found
            if (found.isNotEmpty()) {
                persistDiscoveredTracksIfNew(found)
                postBannerMessage("Synced ${found.size} device music files to permanent library")
            } else {
                postBannerMessage("No external MediaStore songs found. Use 'Import Songs' or play Studio WAV tracks.")
            }
        }
    }

    fun importSongsFromSaf(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = DeviceMusicScanner.importDocumentUris(appContext, uris)
            if (imported.isNotEmpty()) {
                val uniqueImported = filterNonDuplicateEntities(imported)
                if (uniqueImported.isNotEmpty()) {
                    dao.insertImportedTracks(uniqueImported)
                }
                val firstTrack = imported.first().toTrackItem()
                delay(100)
                playTrack(firstTrack)
                postBannerMessage(
                    if (uniqueImported.size == imported.size) {
                        "Imported & playing ${uniqueImported.size} song(s) with persistent permission"
                    } else {
                        "Imported ${uniqueImported.size} new song(s) (${imported.size - uniqueImported.size} duplicate skipped)"
                    }
                )
            } else {
                postBannerMessage("Could not read selected audio file.")
            }
        }
    }

    fun importFolderFromSaf(treeUri: Uri?) {
        if (treeUri == null) return
        viewModelScope.launch {
            val imported = DeviceMusicScanner.importFolderTreeUri(appContext, treeUri)
            if (imported.isNotEmpty()) {
                val uniqueImported = filterNonDuplicateEntities(imported)
                if (uniqueImported.isNotEmpty()) {
                    dao.insertImportedTracks(uniqueImported)
                }
                postBannerMessage(
                    "Imported ${uniqueImported.size} new song(s) from folder (${imported.size - uniqueImported.size} duplicates skipped)"
                )
            } else {
                postBannerMessage("No supported audio files found in selected folder.")
            }
        }
    }

    private suspend fun filterNonDuplicateEntities(candidates: List<ImportedTrackEntity>): List<ImportedTrackEntity> {
        val existing = allTracks.value
        val existingUris = existing.map { it.uriString.trim() }.toHashSet()
        val existingSigs = existing.map {
            "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}|${it.durationMs / 1000}"
        }.toHashSet()

        val result = mutableListOf<ImportedTrackEntity>()
        for (entity in candidates) {
            val uri = entity.uriString.trim()
            val sig = "${entity.title.trim().lowercase()}|${entity.artist.trim().lowercase()}|${entity.durationMs / 1000}"
            if (uri !in existingUris && sig !in existingSigs) {
                existingUris.add(uri)
                existingSigs.add(sig)
                result.add(entity)
            }
        }
        return result
    }

    /**
     * Removes a song reference from the app library and playlists WITHOUT deleting or modifying
     * the original audio file on the user's device.
     */
    fun removeTrackFromLibrary(track: TrackItem) {
        viewModelScope.launch {
            dao.deleteImportedTrack(track.id)
            dao.removeFavorite(track.id)
            dao.removeTrackFromAllPlaylists(track.id)
            _mediaStoreTracks.value = _mediaStoreTracks.value.filterNot { it.id == track.id }
            postBannerMessage("Removed '${track.title}' from app library (original file preserved)")
        }
    }

    // =========================================================================
    // LIBRARY, FAVORITES & PERMANENT PLAYLIST MANAGEMENT
    // =========================================================================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedLibraryTab(tab: LibraryFilterTab) {
        _selectedLibraryTab.value = tab
    }

    fun setSortOrder(order: LibrarySortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            if (trackId in favoriteTrackIds.value) {
                dao.removeFavorite(trackId)
                postBannerMessage("Removed from Favorites")
            } else {
                dao.addFavorite(FavoriteTrackEntity(trackId = trackId))
                postBannerMessage("Added to Favorites")
            }
        }
    }

    fun createPlaylist(name: String, description: String, initialTrackIds: List<String> = emptyList()) {
        val cleanName = name.trim().ifEmpty { "Lo-Fi Mix" }
        viewModelScope.launch {
            val plId = dao.insertPlaylist(
                PlaylistEntity(
                    name = cleanName,
                    description = description.ifBlank { "Custom Lo-Fi Wave Playlist" }
                )
            )
            initialTrackIds.distinct().forEachIndexed { idx, tid ->
                dao.addTrackToPlaylist(
                    PlaylistTrackCrossRef(playlistId = plId, trackId = tid, sortIndex = idx)
                )
            }
            postBannerMessage("Created playlist '$cleanName'")
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String, newDescription: String) {
        val cleanName = newName.trim().ifEmpty { "Renamed Playlist" }
        viewModelScope.launch {
            dao.updatePlaylistMetadata(
                playlistId = playlistId,
                name = cleanName,
                description = newDescription.trim().ifBlank { "Custom Lo-Fi Wave Playlist" }
            )
            postBannerMessage("Renamed playlist to '$cleanName'")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            dao.clearPlaylistTracks(playlistId)
            dao.deletePlaylist(playlistId)
            postBannerMessage("Deleted playlist")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            val currentLinks = dao.getAllPlaylistTrackLinks().first().filter { it.playlistId == playlistId }
            if (currentLinks.any { it.trackId == trackId }) {
                postBannerMessage("Song is already in this playlist")
                return@launch
            }
            val nextSortIndex = (currentLinks.maxOfOrNull { it.sortIndex } ?: -1) + 1
            dao.addTrackToPlaylist(
                PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, sortIndex = nextSortIndex)
            )
            postBannerMessage("Added song to playlist")
        }
    }

    /**
     * Adds a single song to multiple selected playlists simultaneously (avoiding duplicates).
     */
    fun addTrackToMultiplePlaylists(trackId: String, playlistIds: Collection<Long>) {
        if (playlistIds.isEmpty()) return
        viewModelScope.launch {
            val allLinks = dao.getAllPlaylistTrackLinks().first()
            var addedCount = 0
            for (plId in playlistIds) {
                val linksForPl = allLinks.filter { it.playlistId == plId }
                if (linksForPl.none { it.trackId == trackId }) {
                    val nextSort = (linksForPl.maxOfOrNull { it.sortIndex } ?: -1) + 1
                    dao.addTrackToPlaylist(
                        PlaylistTrackCrossRef(playlistId = plId, trackId = trackId, sortIndex = nextSort)
                    )
                    addedCount++
                }
            }
            postBannerMessage(
                if (addedCount > 0) "Added song to $addedCount playlist(s)"
                else "Song is already in the selected playlist(s)"
            )
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            dao.removeTrackFromPlaylist(playlistId = playlistId, trackId = trackId)
            postBannerMessage("Removed song from playlist")
        }
    }

    /**
     * Persists a new drag-and-drop track order for [playlistId] in Room Database.
     */
    fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<String>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updatedRefs = orderedTrackIds.distinct().mapIndexed { index, trackId ->
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = trackId,
                    sortIndex = index,
                    addedAtMillis = now + index
                )
            }
            dao.clearPlaylistTracks(playlistId)
            if (updatedRefs.isNotEmpty()) {
                dao.insertPlaylistTrackLinks(updatedRefs)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearPlaybackHistory()
            postBannerMessage("Cleared playback history")
        }
    }

    // =========================================================================
    // SLEEP TIMER & APP SETTINGS
    // =========================================================================

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSec.value = null
            postBannerMessage("Sleep timer cancelled")
            return
        }
        val totalSeconds = minutes * 60
        _sleepTimerRemainingSec.value = totalSeconds
        postBannerMessage("Sleep timer set for $minutes min")

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSec.value = remaining
            }
            if (remaining <= 0) {
                player.pause()
                _sleepTimerRemainingSec.value = null
                postBannerMessage("Sleep timer finished — playback paused")
            }
        }
    }

    fun updateThemePreferences(theme: AccentTheme, pureAmoled: Boolean, highQualityDecoding: Boolean) {
        _accentTheme.value = theme
        _pureAmoledBlack.value = pureAmoled
        _highQualityDecoding.value = highQualityDecoding
        viewModelScope.launch {
            prefsRepo.saveThemePreferences(theme, pureAmoled, highQualityDecoding)
        }
    }

    fun postBannerMessage(msg: String?) {
        _statusBannerMessage.value = msg
    }

    override fun onCleared() {
        persistCurrentPlaybackState()
        super.onCleared()
        player.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
    }
}
