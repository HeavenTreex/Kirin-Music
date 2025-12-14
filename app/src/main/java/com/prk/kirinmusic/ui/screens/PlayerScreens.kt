package com.prk.kirinmusic.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prk.kirinmusic.viewmodel.MainViewModel

@Composable
fun MiniPlayer(viewModel: MainViewModel, onClick: () -> Unit) {
    val song = viewModel.currentSong ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
    ) {
        // Progreso de la canción
        LinearProgressIndicator(
            progress = {
                if (viewModel.duration > 0) viewModel.currentPosition / viewModel.duration else 0f
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .size(128)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Rounded.MusicNote)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.bounceClick()
            ) {
                Icon(
                    imageVector = if (viewModel.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FullPlayerContent(viewModel: MainViewModel, onShowOptions: () -> Unit) {
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapePlayerLayout(viewModel, onShowOptions)
    } else {
        PortraitPlayerLayout(viewModel, onShowOptions)
    }
}

@Composable
fun PortraitPlayerLayout(viewModel: MainViewModel, onShowOptions: () -> Unit) {
    val song = viewModel.currentSong ?: return

    val scale by animateFloatAsState(
        if (viewModel.isPlaying) 1f else 0.92f,
        animationSpec = tween(durationMillis = 400),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerTopBar(
            onCollapse = { viewModel.isPlayerExpanded = false },
            onMenuClick = onShowOptions
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
                .scale(scale)
                .shadow(
                    elevation = if (viewModel.isPlaying) 16.dp else 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = rememberVectorPainter(Icons.Rounded.Album)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth()) {
            SongInfo(song.title, song.artist)
            Spacer(modifier = Modifier.height(24.dp))
            PlayerSlider(viewModel)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PlayerControls(viewModel)
    }
}

@Composable
fun LandscapePlayerLayout(viewModel: MainViewModel, onShowOptions: () -> Unit) {
    val song = viewModel.currentSong ?: return

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.45f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = rememberVectorPainter(Icons.Rounded.Album)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerTopBar(
                onCollapse = { viewModel.isPlayerExpanded = false },
                onMenuClick = onShowOptions
            )
            SongInfo(song.title, song.artist)
            PlayerSlider(viewModel)
            PlayerControls(viewModel)
        }
    }
}

@Composable
fun PlayerTopBar(onCollapse: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onCollapse, modifier = Modifier.bounceClick()) {
            Icon(Icons.Rounded.KeyboardArrowDown, "Minimizar", Modifier.size(32.dp))
        }
        Text(
            "REPRODUCIENDO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        IconButton(onClick = onMenuClick, modifier = Modifier.bounceClick()) {
            Icon(Icons.Rounded.MoreVert, "Opciones")
        }
    }
}

@Composable
fun SongInfo(title: String, artist: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(0.8f)
        )
    }
}

@Composable
fun PlayerSlider(viewModel: MainViewModel) {
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    if (!isDragging) {
        sliderValue = viewModel.currentPosition
    }

    Column {
        Slider(
            value = sliderValue,
            onValueChange = {
                isDragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                viewModel.seekTo(sliderValue)
                isDragging = false
            },
            valueRange = 0f..viewModel.duration.coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMs(sliderValue.toLong()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMs(viewModel.duration.toLong()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlayerControls(viewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.skipPrev() }, modifier = Modifier
            .size(48.dp)
            .bounceClick()) {
            Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(32.dp))
        }
        FilledIconButton(
            onClick = { viewModel.togglePlayPause() },
            modifier = Modifier
                .size(72.dp)
                .bounceClick(),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            AnimatedContent(
                targetState = viewModel.isPlaying,
                transitionSpec = { scaleIn() togetherWith scaleOut() },
                label = "PlayPause"
            ) { isPlaying ->
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        IconButton(onClick = { viewModel.skipNext() }, modifier = Modifier
            .size(48.dp)
            .bounceClick()) {
            Icon(Icons.Rounded.SkipNext, null, Modifier.size(32.dp))
        }
    }
}

@Composable
fun PlayerOptionsSheetContent(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val song = viewModel.currentSong ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton("Opciones", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
            TabButton("Letra", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "SheetContent",
            modifier = Modifier.heightIn(min = 300.dp)
        ) { tab ->
            if (tab == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OptionItem(Icons.Rounded.Timer, "Temporizador", "Detener en ${viewModel.sleepTimerMinutes} min", { viewModel.setSleepTimer(15); onDismiss() })
                    OptionItem(Icons.Rounded.Speed, "Velocidad", "Normal (1.0x)", {})
                    OptionItem(Icons.Rounded.GraphicEq, "Ecualizador", "Pop", {})
                    OptionItem(Icons.Rounded.Share, "Compartir", null, {})
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!song.lyrics.isNullOrBlank()) {
                        Text(
                            text = song.lyrics,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 32.sp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(40.dp))
                        Icon(Icons.Rounded.Lyrics, null, Modifier
                            .size(64.dp)
                            .alpha(0.3f), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sin letra disponible", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "TabColor")
    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "TabContentColor")

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun OptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun formatMs(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

fun Modifier.shadow(elevation: Dp, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp), clip: Boolean = elevation > 0.dp) = this.graphicsLayer {
    this.shadowElevation = elevation.toPx()
    this.shape = shape
    this.clip = clip
}

fun Modifier.bounceClick(scaleDown: Float = 0.90f) = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(
        targetValue = if (buttonState == ButtonState.Pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState == ButtonState.Pressed) {
                    waitForUpOrCancellation()
                    ButtonState.Idle
                } else {
                    awaitFirstDown(false)
                    ButtonState.Pressed
                }
            }
        }
}

enum class ButtonState { Idle, Pressed }
