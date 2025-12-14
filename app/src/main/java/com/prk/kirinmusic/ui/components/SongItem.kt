package com.prk.kirinmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prk.kirinmusic.model.Playlist
import com.prk.kirinmusic.model.Song

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: Song,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    playlists: List<Playlist> = emptyList(),
    onAddToPlaylist: (Playlist) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "selectionColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = if (isSelected) 8.dp else 2.dp,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .error(android.R.drawable.ic_menu_gallery)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset((-12).dp, 0.dp),
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reproducir", fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                                onClick = {
                                    onClick()
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Agregar a playlist", fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) },
                                onClick = { 
                                    showMenu = false
                                    showPlaylistMenu = true
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                        
                        // Sub-menu para Playlists (simulado con otro DropdownMenu por simplicidad)
                        if (showPlaylistMenu) {
                             DropdownMenu(
                                expanded = showPlaylistMenu,
                                onDismissRequest = { showPlaylistMenu = false },
                                offset = DpOffset((-12).dp, 0.dp),
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                            ) {
                                if (playlists.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Sin playlists creadas", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) },
                                        onClick = { showPlaylistMenu = false }
                                    )
                                } else {
                                    playlists.forEach { playlist ->
                                        DropdownMenuItem(
                                            text = { Text(playlist.name) },
                                            onClick = {
                                                onAddToPlaylist(playlist)
                                                showPlaylistMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
