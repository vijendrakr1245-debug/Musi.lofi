package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.PlaybackHistoryEntity
import com.example.model.LibraryFilterTab
import com.example.model.LibrarySortOrder
import com.example.model.TrackItem
import com.example.ui.components.formatDurationMillis
import com.example.ui.theme.AnalogAmber
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VinylRose
import com.example.viewmodel.PlaylistUiModel
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    allTracks: List<TrackItem>,
    filteredTracks: List<TrackItem>,
    favoriteIds: Set<String>,
    history: List<PlaybackHistoryEntity>,
    playlists: List<PlaylistUiModel>,
    searchQuery: String,
    selectedTab: LibraryFilterTab,
    sortOrder: LibrarySortOrder,
    currentTrack: TrackItem?,
    isPlaying: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (LibraryFilterTab) -> Unit,
    onSelectSortOrder: (LibrarySortOrder) -> Unit,
    onPlayTrack: (TrackItem, List<TrackItem>?) -> Unit,
    onPlayPlaylist: (List<TrackItem>, Boolean, Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddTrackToMultiplePlaylists: (String, Collection<Long>) -> Unit,
    onCreatePlaylistWithTrack: (String, String, List<String>) -> Unit,
    onRemoveTrackFromLibrary: (TrackItem) -> Unit,
    onImportSongsClick: () -> Unit,
    onImportFolderClick: () -> Unit,
    onScanDeviceClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var trackPendingPlaylistAdd by remember { mutableStateOf<TrackItem?>(null) }
    var quickPlaylistName by remember { mutableStateOf("") }
    val selectedPlaylistIds = remember { mutableStateListOf<Long>() }

    if (trackPendingPlaylistAdd != null) {
        val targetTrack = trackPendingPlaylistAdd!!
        AlertDialog(
            onDismissRequest = {
                trackPendingPlaylistAdd = null
                selectedPlaylistIds.clear()
            },
            containerColor = ElevatedCharcoal,
            title = {
                Column {
                    Text("Add to Playlists", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        text = targetTrack.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select one or multiple playlists:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    if (playlists.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.height(160.dp)
                        ) {
                            items(playlists, key = { it.playlist.playlistId }) { pl ->
                                val alreadyIn = pl.tracks.any { it.id == targetTrack.id }
                                val isChecked = pl.playlist.playlistId in selectedPlaylistIds || alreadyIn
                                Surface(
                                    color = GlassSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!alreadyIn) {
                                                if (pl.playlist.playlistId in selectedPlaylistIds) {
                                                    selectedPlaylistIds.remove(pl.playlist.playlistId)
                                                } else {
                                                    selectedPlaylistIds.add(pl.playlist.playlistId)
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                enabled = !alreadyIn,
                                                onCheckedChange = { checked ->
                                                    if (!alreadyIn) {
                                                        if (checked) selectedPlaylistIds.add(pl.playlist.playlistId)
                                                        else selectedPlaylistIds.remove(pl.playlist.playlistId)
                                                    }
                                                }
                                            )
                                            Column {
                                                Text(pl.playlist.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                Text(
                                                    text = if (alreadyIn) "Already in playlist" else "${pl.tracks.size} songs",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (alreadyIn) StudioEmerald else NeonCyan
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Inline quick playlist creation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quickPlaylistName,
                            onValueChange = { quickPlaylistName = it },
                            placeholder = { Text("Or new playlist name...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (quickPlaylistName.isNotBlank()) {
                                    onCreatePlaylistWithTrack(
                                        quickPlaylistName,
                                        "Created from Library",
                                        listOf(targetTrack.id)
                                    )
                                    quickPlaylistName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                        ) {
                            Text("Create", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (!targetTrack.isBuiltInSample) {
                        TextButton(
                            onClick = {
                                onRemoveTrackFromLibrary(targetTrack)
                                trackPendingPlaylistAdd = null
                                selectedPlaylistIds.clear()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = VinylRose, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remove from App Library (keeps file on phone)",
                                style = MaterialTheme.typography.labelSmall,
                                color = VinylRose
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPlaylistIds.isNotEmpty()) {
                            onAddTrackToMultiplePlaylists(targetTrack.id, selectedPlaylistIds.toList())
                        }
                        selectedPlaylistIds.clear()
                        trackPendingPlaylistAdd = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Add to Selected (${selectedPlaylistIds.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedPlaylistIds.clear()
                        trackPendingPlaylistAdd = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("library_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "OFFLINE MUSIC LIBRARY",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${allTracks.size} Songs • Permanent Room & SAF Storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sort_order_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sortOrder.label, style = MaterialTheme.typography.labelSmall)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        LibrarySortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    onSelectSortOrder(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Device Music Import & SAF Controls
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onImportSongsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_import_songs_button")
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Songs", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onImportFolderClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_import_folder_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Folder", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }

                OutlinedButton(
                    onClick = onScanDeviceClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("library_scan_device_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan MediaStore", tint = AnalogAmber, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Search Songs Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search songs, artists, albums, folders...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_songs_input")
            )
        }

        // All 8 Library Filter Category Tabs
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LibraryFilterTab.entries) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                            selectedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            LibraryFilterTab.ALBUMS -> {
                val groupedByAlbum = filteredTracks.groupBy { it.album }
                items(groupedByAlbum.entries.toList(), key = { "album_${it.key}" }) { (albumName, albumTracks) ->
                    GroupedCollectionCard(
                        title = albumName,
                        subtitle = "${albumTracks.firstOrNull()?.artist ?: "Various"} • ${albumTracks.size} song(s)",
                        icon = Icons.Default.Album,
                        tracks = albumTracks,
                        onPlayAll = { onPlayPlaylist(albumTracks, false, true) },
                        onShuffleAll = { onPlayPlaylist(albumTracks, true, true) },
                        onPlaySingle = { t -> onPlayTrack(t, albumTracks) }
                    )
                }
            }
            LibraryFilterTab.ARTISTS -> {
                val groupedByArtist = filteredTracks.groupBy { it.artist }
                items(groupedByArtist.entries.toList(), key = { "artist_${it.key}" }) { (artistName, artistTracks) ->
                    GroupedCollectionCard(
                        title = artistName,
                        subtitle = "${artistTracks.size} song(s) in library",
                        icon = Icons.Default.Person,
                        tracks = artistTracks,
                        onPlayAll = { onPlayPlaylist(artistTracks, false, true) },
                        onShuffleAll = { onPlayPlaylist(artistTracks, true, true) },
                        onPlaySingle = { t -> onPlayTrack(t, artistTracks) }
                    )
                }
            }
            LibraryFilterTab.FOLDERS -> {
                val groupedByFolder = filteredTracks.groupBy { it.folderName }
                items(groupedByFolder.entries.toList(), key = { "folder_${it.key}" }) { (folderName, folderTracks) ->
                    GroupedCollectionCard(
                        title = folderName,
                        subtitle = "${folderTracks.size} audio file(s) • Persistent URI access",
                        icon = Icons.Default.Folder,
                        tracks = folderTracks,
                        onPlayAll = { onPlayPlaylist(folderTracks, false, true) },
                        onShuffleAll = { onPlayPlaylist(folderTracks, true, true) },
                        onPlaySingle = { t -> onPlayTrack(t, folderTracks) }
                    )
                }
            }
            LibraryFilterTab.CUSTOM_PLAYLISTS -> {
                if (playlists.isEmpty()) {
                    item {
                        EmptyCollectionState("No custom playlists yet. Create unlimited playlists in the Playlists tab or via any song's '+' button.")
                    }
                } else {
                    items(playlists, key = { "lib_pl_${it.playlist.playlistId}" }) { plModel ->
                        GroupedCollectionCard(
                            title = plModel.playlist.name,
                            subtitle = "${plModel.playlist.description} • ${plModel.tracks.size} song(s)",
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            tracks = plModel.tracks,
                            onPlayAll = { onPlayPlaylist(plModel.tracks, false, true) },
                            onShuffleAll = { onPlayPlaylist(plModel.tracks, true, true) },
                            onPlaySingle = { t -> onPlayTrack(t, plModel.tracks) }
                        )
                    }
                }
            }
            LibraryFilterTab.RECENTLY_PLAYED -> {
                val historyTracks = history.mapNotNull { h -> allTracks.find { it.id == h.trackId } }
                if (historyTracks.isEmpty()) {
                    item {
                        EmptyCollectionState("No recently played tracks yet. Tap any song to start Live Lo-Fi playback.")
                    }
                } else {
                    items(historyTracks, key = { "recent_${it.id}" }) { track ->
                        TrackRowCard(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            isFavorite = track.id in favoriteIds,
                            onPlay = { onPlayTrack(track, historyTracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            trailingAction = {
                                IconButton(
                                    onClick = {
                                        selectedPlaylistIds.clear()
                                        trackPendingPlaylistAdd = track
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = "Add to Multiple Playlists",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        )
                    }
                }
            }
            else -> {
                if (filteredTracks.isEmpty()) {
                    item {
                        EmptyCollectionState(
                            if (selectedTab == LibraryFilterTab.CONVERTED_SONGS) {
                                "No converted songs exported yet. Open Music Converter from Home to convert & save any song as WAV."
                            } else {
                                "No matching songs found. Tap 'Import Songs' to pick MP3/WAV/FLAC files from your device."
                            }
                        )
                    }
                } else {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackRowCard(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying,
                            isFavorite = track.id in favoriteIds,
                            onPlay = { onPlayTrack(track, filteredTracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            trailingAction = {
                                IconButton(
                                    onClick = {
                                        selectedPlaylistIds.clear()
                                        trackPendingPlaylistAdd = track
                                    },
                                    modifier = Modifier.testTag("add_to_playlists_btn_${track.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = "Add to Multiple Playlists",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupedCollectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tracks: List<TrackItem>,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlaySingle: (TrackItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = ElevatedCharcoal,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onShuffleAll) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (t in tracks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassSurface)
                                .clickable { onPlaySingle(t) }
                                .padding(10.dp)
                        ) {
                            Text(
                                text = t.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatDurationMillis(t.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCollectionState(message: String) {
    Surface(
        color = ElevatedCharcoal,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

/**
 * Permanent Playlist Management Screen with Unlimited Playlists, Rename, Delete,
 * Multi-Song Add, Play/Shuffle/Repeat, and Drag-and-Drop Song Reordering.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistUiModel>,
    allTracks: List<TrackItem>,
    favoriteIds: Set<String>,
    history: List<PlaybackHistoryEntity>,
    onCreatePlaylist: (String, String, List<String>) -> Unit,
    onRenamePlaylist: (Long, String, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onRemoveTrackFromPlaylist: (Long, String) -> Unit,
    onReorderPlaylistTracks: (Long, List<String>) -> Unit,
    onPlayPlaylist: (List<TrackItem>, Boolean, Boolean) -> Unit,
    onPlaySingleTrackFromPlaylist: (TrackItem, List<TrackItem>) -> Unit,
    onClearHistory: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    val selectedTrackIds = remember { mutableStateListOf<String>() }

    // Rename playlist dialog state
    var playlistBeingRenamed by remember { mutableStateOf<PlaylistUiModel?>(null) }
    var renameTitleInput by remember { mutableStateOf("") }
    var renameDescInput by remember { mutableStateOf("") }

    // Add songs to existing playlist dialog state
    var playlistAddingSongs by remember { mutableStateOf<PlaylistUiModel?>(null) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = ElevatedCharcoal,
            title = {
                Text("Create Custom Lo-Fi Playlist", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description / Mood") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Select initial tracks (${selectedTrackIds.size} selected):",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                    LazyColumn(modifier = Modifier.height(170.dp)) {
                        items(allTracks, key = { "sel_${it.id}" }) { t ->
                            val checked = t.id in selectedTrackIds
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedTrackIds.remove(t.id) else selectedTrackIds.add(t.id)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it) selectedTrackIds.add(t.id) else selectedTrackIds.remove(t.id)
                                    }
                                )
                                Text(
                                    text = "${t.title} — ${t.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(newPlaylistName, newPlaylistDesc, selectedTrackIds.toList())
                        newPlaylistName = ""
                        newPlaylistDesc = ""
                        selectedTrackIds.clear()
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Create Playlist")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (playlistBeingRenamed != null) {
        val target = playlistBeingRenamed!!
        AlertDialog(
            onDismissRequest = { playlistBeingRenamed = null },
            containerColor = ElevatedCharcoal,
            title = { Text("Rename Playlist", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = renameTitleInput,
                        onValueChange = { renameTitleInput = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = renameDescInput,
                        onValueChange = { renameDescInput = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenamePlaylist(target.playlist.playlistId, renameTitleInput, renameDescInput)
                        playlistBeingRenamed = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Save Name")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistBeingRenamed = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (playlistAddingSongs != null) {
        val targetPl = playlistAddingSongs!!
        val existingIds = targetPl.tracks.map { it.id }.toSet()
        AlertDialog(
            onDismissRequest = { playlistAddingSongs = null },
            containerColor = ElevatedCharcoal,
            title = { Text("Add Songs to '${targetPl.playlist.name}'", style = MaterialTheme.typography.titleLarge) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(230.dp)
                ) {
                    items(allTracks, key = { "add_pl_${it.id}" }) { track ->
                        val isAdded = track.id in existingIds
                        Surface(
                            color = GlassSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAdded) {
                                    onAddTrackToPlaylist(targetPl.playlist.playlistId, track.id)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1)
                                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                                }
                                Text(
                                    text = if (isAdded) "ADDED ✓" else "+ ADD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAdded) StudioEmerald else NeonCyan
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { playlistAddingSongs = null }) {
                    Text("Done", color = NeonCyan)
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("playlists_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "PLAYLISTS & COLLECTIONS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Drag & Drop Reordering • Unlimited Room Playlists",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Playlist", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Smart Collection: Favorites
        item {
            val favTracks = allTracks.filter { it.id in favoriteIds }
            Surface(
                color = ElevatedCharcoal,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VinylRose.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("♥ FAVORITE SONGS", style = MaterialTheme.typography.labelLarge, color = VinylRose)
                        Text(
                            text = "${favTracks.size} liked track(s) saved permanently",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (favTracks.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { onPlayPlaylist(favTracks, true, true) }) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle Favorites", tint = VinylRose)
                            }
                            Button(
                                onClick = { onPlayPlaylist(favTracks, false, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = VinylRose)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Custom Playlists with Drag-and-Drop Reordering
        items(playlists, key = { it.playlist.playlistId }) { plModel ->
            PlaylistCardWithDragReorder(
                plModel = plModel,
                onPlayAll = { repeat -> onPlayPlaylist(plModel.tracks, false, repeat) },
                onShufflePlay = { repeat -> onPlayPlaylist(plModel.tracks, true, repeat) },
                onPlaySingleTrack = { track -> onPlaySingleTrackFromPlaylist(track, plModel.tracks) },
                onRenameClick = {
                    renameTitleInput = plModel.playlist.name
                    renameDescInput = plModel.playlist.description
                    playlistBeingRenamed = plModel
                },
                onAddSongsClick = { playlistAddingSongs = plModel },
                onDeleteClick = { onDeletePlaylist(plModel.playlist.playlistId) },
                onRemoveTrack = { trackId -> onRemoveTrackFromPlaylist(plModel.playlist.playlistId, trackId) },
                onCommitReorder = { newOrderedIds ->
                    onReorderPlaylistTracks(plModel.playlist.playlistId, newOrderedIds)
                }
            )
        }

        // Playback History Section
        if (history.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AnalogAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PLAYBACK HISTORY (${history.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = AnalogAmber
                        )
                    }
                    TextButton(onClick = onClearHistory) {
                        Text("Clear History", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
            items(history.take(10), key = { "hist_${it.trackId}" }) { h ->
                Surface(
                    color = ElevatedCharcoal,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(h.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${h.artist} • Played ${h.playCount} time(s)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Text(h.formatBadge, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCardWithDragReorder(
    plModel: PlaylistUiModel,
    onPlayAll: (Boolean) -> Unit,
    onShufflePlay: (Boolean) -> Unit,
    onPlaySingleTrack: (TrackItem) -> Unit,
    onRenameClick: () -> Unit,
    onAddSongsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveTrack: (String) -> Unit,
    onCommitReorder: (List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var repeatPlaylist by remember { mutableStateOf(true) }

    // Mutable local list synchronized with plModel.tracks for smooth real-time drag-and-drop reordering
    val localTracks = remember { mutableStateListOf<TrackItem>() }
    LaunchedEffect(plModel.tracks) {
        localTracks.clear()
        localTracks.addAll(plModel.tracks)
    }

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 56.dp.toPx() }

    Surface(
        color = ElevatedCharcoal,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
            .testTag("playlist_card_${plModel.playlist.playlistId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: Title, Description, Rename & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plModel.playlist.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${plModel.playlist.description} • ${localTracks.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRenameClick,
                        modifier = Modifier.testTag("rename_playlist_${plModel.playlist.playlistId}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename Playlist", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onAddSongsClick,
                        modifier = Modifier.testTag("add_songs_playlist_${plModel.playlist.playlistId}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Songs", tint = StudioEmerald, modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_playlist_${plModel.playlist.playlistId}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playlist Transport Action Row: Play All, Shuffle Playlist, Repeat Playlist
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onPlayAll(repeatPlaylist) },
                    enabled = localTracks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_playlist_${plModel.playlist.playlistId}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = { onShufflePlay(repeatPlaylist) },
                    enabled = localTracks.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shuffle_playlist_${plModel.playlist.playlistId}")
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }

                FilterChip(
                    selected = repeatPlaylist,
                    onClick = { repeatPlaylist = !repeatPlaylist },
                    label = { Text("Repeat", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repeat Playlist",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricPurple.copy(alpha = 0.25f),
                        selectedLabelColor = NeonCyan
                    )
                )
            }

            if (expanded && localTracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "DRAG HANDLE (≡) OR ARROWS TO REORDER SONGS:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    localTracks.forEachIndexed { index, track ->
                        val isDragging = index == draggedIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 0f)
                                .offset {
                                    IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDragging) ElectricPurple.copy(alpha = 0.35f) else GlassSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isDragging) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onPlaySingleTrack(track) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Drag Handle with real-time pointerInput drag-and-drop gesture
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .pointerInput(localTracks.size) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedIndex = index
                                                    dragOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    draggedIndex = -1
                                                    dragOffsetY = 0f
                                                    onCommitReorder(localTracks.map { it.id })
                                                },
                                                onDragCancel = {
                                                    draggedIndex = -1
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    val currentIdx = draggedIndex
                                                    if (currentIdx in localTracks.indices) {
                                                        if (dragOffsetY > rowHeightPx * 0.65f && currentIdx < localTracks.lastIndex) {
                                                            val item = localTracks.removeAt(currentIdx)
                                                            localTracks.add(currentIdx + 1, item)
                                                            draggedIndex = currentIdx + 1
                                                            dragOffsetY -= rowHeightPx
                                                        } else if (dragOffsetY < -rowHeightPx * 0.65f && currentIdx > 0) {
                                                            val item = localTracks.removeAt(currentIdx)
                                                            localTracks.add(currentIdx - 1, item)
                                                            draggedIndex = currentIdx - 1
                                                            dragOffsetY += rowHeightPx
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .testTag("drag_handle_${plModel.playlist.playlistId}_${track.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = NeonCyan
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${track.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${track.formatBadge} • ${formatDurationMillis(track.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }

                                // Accessible Up/Down Move Buttons
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val item = localTracks.removeAt(index)
                                            localTracks.add(index - 1, item)
                                            onCommitReorder(localTracks.map { it.id })
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (index < localTracks.lastIndex) {
                                    IconButton(
                                        onClick = {
                                            val item = localTracks.removeAt(index)
                                            localTracks.add(index + 1, item)
                                            onCommitReorder(localTracks.map { it.id })
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveTrack(track.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Remove from playlist",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Playlist is empty. Tap '+' above to add songs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
