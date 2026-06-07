package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.LocalAudio
import com.example.ui.theme.*
import com.example.ui.viewmodel.HostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSelectorDialog(
    viewModel: HostViewModel,
    onDismiss: () -> Unit,
    onPlay: (LocalAudio) -> Unit
) {
    val songs by viewModel.localSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Songs, 1 = Playlists
    var searchQuery by remember { mutableStateOf("") }
    
    var selectionMode by remember { mutableStateOf(false) }
    var selectedSongs by remember { mutableStateOf(setOf<LocalAudio>()) }
    
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    
    // For viewing a specific playlist
    var viewingPlaylist by remember { mutableStateOf<com.example.data.Playlist?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ThemeBackground) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        if (viewingPlaylist != null) {
                            viewingPlaylist = null
                        } else if (selectionMode) {
                            selectionMode = false
                            selectedSongs = emptySet()
                        } else {
                            onDismiss() 
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThemeOnBackground)
                    }
                    Text(
                        if (selectionMode) "${selectedSongs.size} selected" 
                        else if (viewingPlaylist != null) viewingPlaylist!!.name 
                        else "Select Music",
                        color = ThemeOnBackground,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (selectionMode && selectedSongs.isNotEmpty()) {
                        IconButton(onClick = { showCreatePlaylistDialog = true }) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = "Create Playlist", tint = ThemePrimary)
                        }
                    }
                }
                
                if (viewingPlaylist == null) {
                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = ThemeBackground,
                        contentColor = ThemePrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; selectionMode = false; selectedSongs = emptySet() },
                            text = { Text("Songs", color = if (selectedTab == 0) ThemePrimary else ThemeOnSurfaceVariant) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; selectionMode = false; selectedSongs = emptySet() },
                            text = { Text("Playlists", color = if (selectedTab == 1) ThemePrimary else ThemeOnSurfaceVariant) }
                        )
                    }
                }

                if (selectedTab == 0 && viewingPlaylist == null) {
                    // Songs Tab
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = { Text("Search songs or artists...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThemeSurface,
                            unfocusedContainerColor = ThemeSurface
                        ),
                        shape = CircleShape
                    )

                    val filteredSongs = remember(songs, searchQuery) {
                        songs.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredSongs) { song ->
                            val isSelected = selectedSongs.contains(song)
                            ListItem(
                                headlineContent = { Text(song.title, color = ThemeOnBackground, maxLines = 1) },
                                supportingContent = { Text(song.artist, color = ThemeOnSurfaceVariant, maxLines = 1) },
                                leadingContent = {
                                    if (selectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(checkedColor = ThemePrimary)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(40.dp).background(ThemePrimary.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, null, tint = ThemePrimary)
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .clickable {
                                        if (selectionMode) {
                                            selectedSongs = if (isSelected) selectedSongs - song else selectedSongs + song
                                            if (selectedSongs.isEmpty()) selectionMode = false
                                        } else {
                                            onPlay(song)
                                            onDismiss()
                                        }
                                    }
                            )
                        }
                    }
                } else if (selectedTab == 1 && viewingPlaylist == null) {
                    // Playlists Tab
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(playlists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name, color = ThemeOnBackground, maxLines = 1) },
                                supportingContent = { Text("${playlist.getSongs().size} songs", color = ThemeOnSurfaceVariant) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(ThemeSurfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.QueueMusic, null, tint = ThemePrimary)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    viewingPlaylist = playlist
                                }
                            )
                        }
                        if (playlists.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No playlists yet. Long press a song or use selection to create one.", color = ThemeOnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                } else if (viewingPlaylist != null) {
                    // Viewing Playlist
                    val plSongs = viewingPlaylist!!.getSongs()
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(plSongs) { song ->
                            ListItem(
                                headlineContent = { Text(song.title, color = ThemeOnBackground, maxLines = 1) },
                                supportingContent = { Text(song.artist, color = ThemeOnSurfaceVariant, maxLines = 1) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(ThemePrimary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, null, tint = ThemePrimary)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    onPlay(song)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
                
                if (selectedTab == 0 && !selectionMode && viewingPlaylist == null) {
                    ExtendedFloatingActionButton(
                        onClick = { selectionMode = true },
                        modifier = Modifier.align(Alignment.End).padding(16.dp),
                        containerColor = ThemePrimary,
                        contentColor = ThemeOnPrimary
                    ) {
                        Icon(Icons.Default.Checklist, "Select")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select")
                    }
                }
            }

            if (showCreatePlaylistDialog) {
                var playlistName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreatePlaylistDialog = false },
                    title = { Text("Create Playlist", color = ThemeOnSurface) },
                    text = {
                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (playlistName.isNotBlank()) {
                                    viewModel.savePlaylist(playlistName, selectedSongs.toList())
                                    showCreatePlaylistDialog = false
                                    selectionMode = false
                                    selectedSongs = emptySet()
                                    selectedTab = 1
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylistDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    containerColor = ThemeSurface
                )
            }
        }
    }
}
