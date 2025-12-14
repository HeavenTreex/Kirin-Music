package com.prk.kirinmusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prk.kirinmusic.model.Song
import com.prk.kirinmusic.viewmodel.MainViewModel

@Composable
fun SongListScreen(viewModel: MainViewModel) {
    // El Scaffold en MainScreen ya maneja el espacio del BottomBar/MiniPlayer a través del innerPadding
    // Solo añadimos un pequeño margen estético al final de la lista
    val bottomPadding = 16.dp

    // Utiliza remember para estabilizar la lista y evitar recomposiciones innecesarias
    val filteredSongs = remember(viewModel.filteredSongs) { viewModel.filteredSongs }
    val selectedSongs = remember(viewModel.selectedSongs) { viewModel.selectedSongs }
    val currentSongId = viewModel.currentSong?.id
    val isSelectionMode = viewModel.isSelectionMode
    val isGlobalPlaying = viewModel.isPlaying

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SearchBar(
            query = viewModel.searchQuery,
            onQueryChange = {
                viewModel.searchQuery = it
                viewModel.filterSongs()
            },
            isSelectionMode = isSelectionMode,
            selectedCount = selectedSongs.size,
            onClearSelection = { viewModel.clearSelection() },
            onDeleteSelection = { showDeleteDialog = true },
            onSelectAll = { viewModel.selectAll() }
        )

        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.MusicOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No se encontraron canciones",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding)
            ) {
                items(
                    items = filteredSongs,
                    key = { it.id },
                    contentType = { "song_item" }
                ) { song ->
                    // Extraemos el estado de selección y reproducción para evitar recomponer todos los items
                    // cuando algo cambia globalmente pero no afecta a este item específico
                    val isSelected = selectedSongs.contains(song.id)
                    val isCurrentSong = currentSongId == song.id
                    
                    SongItem(
                        song = song,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        isCurrentSong = isCurrentSong,
                        isAudioPlaying = isGlobalPlaying,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSelection(song.id)
                            } else {
                                viewModel.playSong(song)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                viewModel.isSelectionMode = true
                                viewModel.toggleSelection(song.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar canciones") },
            text = { Text("¿Estás seguro de que quieres eliminar ${selectedSongs.size} canciones?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedSongs()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onSelectAll: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isFocused) {
        focusManager.clearFocus()
    }

    if (isSelectionMode) {
        TopAppBar(
            title = { Text("$selectedCount seleccionados") },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Rounded.Close, "Cancelar")
                }
            },
            actions = {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Rounded.SelectAll, "Seleccionar todo")
                }
                IconButton(onClick = onDeleteSelection) {
                    Icon(Icons.Rounded.Delete, "Eliminar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    } else {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { Text("Buscar canción, artista...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isCurrentSong: Boolean,
    isAudioPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else if (isCurrentSong) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(false) // Deshabilitar crossfade para mejorar rendimiento en scroll rápido
                    .size(128)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                error = rememberVectorPainter(Icons.Rounded.MusicNote)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrentSong && !isSelectionMode) {
            PlaybackIndicator(
                color = MaterialTheme.colorScheme.primary,
                isAnimating = isAudioPlaying
            )
        }
    }
}

@Composable
fun PlaybackIndicator(
    color: Color,
    isAnimating: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "music_indicator_transition")

    val duration1 = 500
    val duration2 = 400
    val duration3 = 600

    val h1Animated by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration1, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    
    val h2Animated by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration2, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val h3Animated by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration3, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val h1 = if (isAnimating) h1Animated else 0.4f
    val h2 = if (isAnimating) h2Animated else 0.6f
    val h3 = if (isAnimating) h3Animated else 0.3f

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(14.dp) // Altura fija para el indicador
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h1)
                .background(color, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h2)
                .background(color, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h3)
                .background(color, RoundedCornerShape(50))
        )
    }
}
