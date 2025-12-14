package com.prk.kirinmusic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prk.kirinmusic.ui.SettingsScreen
import com.prk.kirinmusic.ui.components.BottomNavBar
import com.prk.kirinmusic.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // Usamos isPlayerExpanded directamente para la visibilidad del FullPlayer
    val isPlayerExpanded = viewModel.isPlayerExpanded
    val currentSong = viewModel.currentSong
    val scope = rememberCoroutineScope()
    
    // Estado para el bottom sheet
    val fullPlayerSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Sincronizamos el estado del sheet con el viewModel
    LaunchedEffect(isPlayerExpanded) {
        if (isPlayerExpanded) {
            fullPlayerSheetState.show()
        } else {
            fullPlayerSheetState.hide()
        }
    }

    // Manejo del botón atrás
    BackHandler(enabled = isPlayerExpanded || selectedTab != 0) {
        if (isPlayerExpanded) {
            scope.launch { 
                fullPlayerSheetState.hide()
            }.invokeOnCompletion {
                if (!fullPlayerSheetState.isVisible) {
                     viewModel.isPlayerExpanded = false
                }
            }
        } else {
            selectedTab = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini reproductor siempre visible si hay canción (cubierto por el full player)
                    AnimatedVisibility(
                        visible = currentSong != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            shadowElevation = 16.dp,
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ModernMiniPlayer(
                                viewModel = viewModel,
                                onClick = { viewModel.isPlayerExpanded = true }
                            )
                        }
                    }
                    BottomNavBar(
                        selectedItem = selectedTab,
                        onItemSelected = { selectedTab = it }
                    )
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 300),
                label = "TabNavigation",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { tab ->
                when (tab) {
                    0 -> SongListScreen(viewModel)
                    1 -> ScreenPlaceholder("Playlists")
                    2 -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { selectedTab = 0 }
                    )
                }
            }
        }

        // Full Player como ModalBottomSheet controlado
        if (isPlayerExpanded) {
            FullPlayerSheet(
                viewModel = viewModel,
                sheetState = fullPlayerSheetState,
                onDismissRequest = { viewModel.isPlayerExpanded = false }
            )
        }
    }
}

@Composable
fun ScreenPlaceholder(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Pantalla de $name", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun ModernMiniPlayer(
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    val currentSong = viewModel.currentSong
    val isPlaying = viewModel.isPlaying
    val duration = viewModel.duration
    val currentPosition = viewModel.currentPosition

    val progress = remember(currentPosition, duration) {
        if (duration > 0) (currentPosition / duration).coerceIn(0f, 1f) else 0f
    }

    if (currentSong != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .height(72.dp),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                leadingContent = {
                    Card(
                        shape = RoundedCornerShape(12.dp), // Esquinas más suaves (Material Expressive)
                        elevation = CardDefaults.cardElevation(0.dp), // Sin elevación, más flat
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.size(48.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentSong.albumArtUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Mini Carátula",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = rememberVectorPainter(Icons.Rounded.MusicNote)
                        )
                    }
                },
                headlineContent = {
                    Text(
                        text = currentSong.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold) // Tipografía más expresiva
                    )
                },
                supportingContent = {
                    Text(
                        text = currentSong.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(40.dp)
                    ) {
                        Crossfade(targetState = isPlaying, label = "MiniPlayPause") { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pausar" else "Reproducir",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FullPlayerSheet(
    viewModel: MainViewModel,
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val currentSong = viewModel.currentSong
    val isPlaying = viewModel.isPlaying
    val scope = rememberCoroutineScope()
    
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { 
        mutableFloatStateOf(if (viewModel.duration > 0) viewModel.currentPosition / viewModel.duration else 0f) 
    }

    var showOptionsSheet by remember { mutableStateOf(false) }
    val optionsSheetState = rememberModalBottomSheetState()

    var showLyricsSheet by remember { mutableStateOf(false) }
    val lyricsSheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, // Fondo más limpio
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = { BottomSheetDefaults.DragHandle() }, // Handle visible para mejor affordance
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismissRequest()
                        }
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Colapsar", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                // Encapsulado "Playing from" en una píldora
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { /* Acción opcional */ }
                            .padding(horizontal = 16.dp, vertical = 8.dp) // Padding más generoso
                    ) {
                         Icon(
                            Icons.Rounded.LibraryMusic, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Biblioteca",
                            style = MaterialTheme.typography.labelLarge, // Texto un poco más grande
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                FilledTonalIconButton(
                    onClick = { showOptionsSheet = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Carátula con estilo "Expressive" (Esquinas grandes)
            Card(
                shape = RoundedCornerShape(32.dp), // Esquinas más redondeadas
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentSong?.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Carátula del Álbum",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = rememberVectorPainter(Icons.Rounded.MusicNote)
                    )

                    if (currentSong?.albumArtUri == null) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Info Canción Centrada
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally // Centrado
            ) {
                Text(
                    text = currentSong?.title ?: "Sin título",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    textAlign = TextAlign.Center, // Centrado
                    modifier = Modifier.basicMarquee(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong?.artist ?: "Artista Desconocido",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    textAlign = TextAlign.Center, // Centrado
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slider con tiempos
            val duration = viewModel.duration
            val currentPos = viewModel.currentPosition
            
            LaunchedEffect(currentPos, duration, isDragging) {
                if (!isDragging && duration > 0) {
                    sliderPosition = currentPos / duration
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderPosition.coerceIn(0f, 1f),
                    onValueChange = { 
                        isDragging = true
                        sliderPosition = it 
                    },
                    onValueChangeFinished = {
                        viewModel.seekTo(sliderPosition * duration)
                        isDragging = false
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime((if (isDragging) sliderPosition * duration else currentPos).toLong()),
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration.toLong()),
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controles de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, // Distribución uniforme
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    val tint = if (viewModel.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Aleatorio", tint = tint, modifier = Modifier.size(28.dp))
                }

                // Previous
                FilledTonalIconButton(
                    onClick = { viewModel.skipPrev() },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                // Play/Pause (FAB Style)
                val playSource = remember { MutableInteractionSource() }
                val playPressed by playSource.collectIsPressedAsState()
                val playScale by animateFloatAsState(if (playPressed) 0.9f else 1f, label = "playScale")

                val playCornerSize by animateDpAsState(
                    targetValue = if (isPlaying) 28.dp else 44.dp,
                    label = "playCornerAnimation"
                )

                Box(
                    modifier = Modifier
                        .size(88.dp) // Botón grande
                        .scale(playScale)
                        .clip(RoundedCornerShape(playCornerSize)) // Squircle shape animated
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            interactionSource = playSource,
                            indication = null,
                            onClick = { viewModel.togglePlayPause() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = isPlaying, label = "PlayPause") { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir/Pausar",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Next
                FilledTonalIconButton(
                    onClick = { viewModel.skipNext() },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                // Repeat
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    val tint = if (viewModel.isRepeatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(Icons.Rounded.Repeat, contentDescription = "Repetir", tint = tint, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = optionsSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Opciones",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                val closeSheet: () -> Unit = {
                    scope.launch { optionsSheetState.hide() }.invokeOnCompletion {
                        if (!optionsSheetState.isVisible) showOptionsSheet = false
                    }
                }

                ListItem(
                    headlineContent = { Text("Ver Letra") },
                    supportingContent = { Text("Mostrar letras de la canción") },
                    leadingContent = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
                    modifier = Modifier.clickable {
                        closeSheet()
                        showLyricsSheet = true
                    }
                )

                ListItem(
                    headlineContent = { Text("Temporizador de Apagado") },
                    supportingContent = { 
                        val time = if(viewModel.sleepTimerMinutes > 0) "${viewModel.sleepTimerMinutes} min" else "Desactivado"
                        Text(time, color = MaterialTheme.colorScheme.primary)
                    },
                    leadingContent = { 
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            val newTime = when (viewModel.sleepTimerMinutes) {
                                0 -> 15
                                15 -> 30
                                30 -> 60
                                else -> 0
                            }
                            viewModel.setSleepTimer(newTime)
                        }) {
                             Icon(Icons.Rounded.Autorenew, contentDescription = "Cambiar")
                        }
                    },
                    modifier = Modifier.clickable { 
                        val newTime = when (viewModel.sleepTimerMinutes) {
                            0 -> 15
                            15 -> 30
                            30 -> 60
                            else -> 0
                        }
                        viewModel.setSleepTimer(newTime)
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Velocidad de Reproducción") },
                    supportingContent = { 
                         Text("${viewModel.playbackSpeed}x", color = MaterialTheme.colorScheme.primary)
                    },
                    leadingContent = { 
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    },
                    modifier = Modifier.clickable { 
                        val newSpeed = when (viewModel.playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            2.0f -> 0.5f
                            else -> 1.0f
                        }
                        viewModel.changePlaybackSpeed(newSpeed)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                ListItem(
                    headlineContent = { Text("Añadir a Playlist") },
                    leadingContent = { 
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    },
                    modifier = Modifier.clickable { 
                        closeSheet()
                    }
                )
            }
        }
    }

    if (showLyricsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet = false },
            sheetState = lyricsSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxHeight(0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Letra",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val lyrics = currentSong?.lyrics
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    if (lyrics.isNullOrBlank()) {
                        Text(
                            text = "No hay letra disponible",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = lyrics,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

// Función auxiliar para formatear tiempo
fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

// Helper simple para el placeholder de imagen si falla Coil
@Composable
fun rememberVectorPainter(image: ImageVector) = androidx.compose.ui.graphics.vector.rememberVectorPainter(image)
