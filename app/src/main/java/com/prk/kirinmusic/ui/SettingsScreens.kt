package com.prk.kirinmusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.prk.kirinmusic.viewmodel.MainViewModel
import com.prk.kirinmusic.viewmodel.ThemeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    navController: NavController? = null,
    onNavigateBack: () -> Unit = {}
) {
    // Manejar el gesto de retroceso: si no hay navController, usamos el callback manual
    BackHandler {
        if (navController != null && navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Ajustes",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (navController != null && navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsGroupCard(title = "Apariencia") {
                ThemeSelector(viewModel)
            }

            SettingsGroupCard(title = "Reproducción") {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Temporizador de apagado",
                    subtitle = if (viewModel.sleepTimerMinutes > 0) "${viewModel.sleepTimerMinutes} min restantes" else "Desactivado",
                    onClick = {
                        val nextTime = when (viewModel.sleepTimerMinutes) {
                            0 -> 15
                            15 -> 30
                            30 -> 60
                            else -> 0
                        }
                        viewModel.setSleepTimer(nextTime)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                PlaybackSpeedSelector(viewModel)
            }

            SettingsGroupCard(title = "Almacenamiento") {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Borrar caché de imágenes",
                    subtitle = "Ocupado: ${viewModel.cacheSizeText}",
                    onClick = { viewModel.clearImageCache() }
                )
            }

            SettingsGroupCard(title = "Información") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Versión",
                    subtitle = "1.0.0 (Beta)",
                    onClick = { }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Desarrollador",
                    subtitle = "PRK Studios",
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun ThemeSelector(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tema de la aplicación", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (viewModel.isDynamicTheme) "Dinámico (Material You)" else "Personalizado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTheme(true, ThemeColor.Blue) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = viewModel.isDynamicTheme,
                        onClick = { viewModel.setTheme(true, ThemeColor.Blue) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dinámico", style = MaterialTheme.typography.bodyMedium)
                }
                
                Text(
                    "Colores fijos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 56.dp, bottom = 8.dp, top = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ThemeColorCircle(Color(0xFF005EB4), ThemeColor.Blue, viewModel)
                    ThemeColorCircle(Color(0xFF006D3B), ThemeColor.Green, viewModel)
                    ThemeColorCircle(Color(0xFFB3261E), ThemeColor.Red, viewModel)
                    ThemeColorCircle(Color(0xFF6750A4), ThemeColor.Purple, viewModel)
                    ThemeColorCircle(Color(0xFF9A4521), ThemeColor.Orange, viewModel)
                }
            }
        }
    }
}

@Composable
fun ThemeColorCircle(color: Color, themeColor: ThemeColor, viewModel: MainViewModel) {
    val isSelected = !viewModel.isDynamicTheme && viewModel.currentThemeColor == themeColor
    val scale by animateFloatAsState(if (isSelected) 1.2f else 1f, label = "scale")
    
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable { viewModel.setTheme(false, themeColor) }
            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlaybackSpeedSelector(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Velocidad de reproducción", style = MaterialTheme.typography.bodyLarge)
            Text("${viewModel.playbackSpeed}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            speeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        viewModel.changePlaybackSpeed(speed)
                        expanded = false
                    },
                    leadingIcon = if (viewModel.playbackSpeed == speed) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}
