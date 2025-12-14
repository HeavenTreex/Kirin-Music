package com.prk.kirinmusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prk.kirinmusic.model.Song
import com.prk.kirinmusic.ui.SettingsScreen
import com.prk.kirinmusic.ui.components.BottomNavBar
import com.prk.kirinmusic.ui.components.SongItem
import com.prk.kirinmusic.ui.screens.PlayerLayer
import com.prk.kirinmusic.ui.theme.MusicPlayerTheme
import com.prk.kirinmusic.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: MainViewModel = viewModel()
            
            MusicPlayerTheme(viewModel = viewModel) {
                val context = LocalContext.current

                
                val storagePermission = if (Build.VERSION.SDK_INT >= 33) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionsToRequest = remember {
                    mutableListOf(storagePermission).apply {
                        if (Build.VERSION.SDK_INT >= 33) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                }

                val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val isStorageGranted = permissions[storagePermission] ?: false

                    if (isStorageGranted) {
                        viewModel.loadLocalMusic()
                    }
                }

                LaunchedEffect(Unit) {
                    val isStorageGranted = ContextCompat.checkSelfPermission(
                        context,
                        storagePermission
                    ) == PackageManager.PERMISSION_GRANTED

                    if (isStorageGranted) {
                        viewModel.loadLocalMusic()
                    } else {
                   
                        multiplePermissionsLauncher.launch(permissionsToRequest)
                    }
                }

                val window = (LocalContext.current as? android.app.Activity)?.window
                val view = LocalView.current
                val isDark = isSystemInDarkTheme()

                SideEffect {
                    window?.let { win ->
                        win.statusBarColor = Color.Transparent.toArgb()
                        win.navigationBarColor = Color.Transparent.toArgb()
                        
                        val insets = WindowCompat.getInsetsController(win, view)
                        insets.isAppearanceLightStatusBars = !isDark
                        insets.isAppearanceLightNavigationBars = !isDark
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MainApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {

    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
   
            if (!viewModel.isSelectionMode) {
                BottomNavBar(
                    selectedItem = selectedItem,
                    onItemSelected = { selectedItem = it }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            
           
            AnimatedContent(
                targetState = selectedItem,
                transitionSpec = {
                    val duration = 300 
                        if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(duration)) { it } + fadeIn(animationSpec = tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { -it / 3 } + fadeOut(animationSpec = tween(duration))
                    } else {
                        slideInHorizontally(animationSpec = tween(duration)) { -it } + fadeIn(animationSpec = tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { it / 3 } + fadeOut(animationSpec = tween(duration))
                    }
                },
                label = "MainNavigationTransition",
                 modifier = Modifier.fillMaxSize().padding(bottom = if (viewModel.currentSong != null) 80.dp else 0.dp) // Espacio extra para el mini player
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> HomeScreen(viewModel = viewModel, isPlaylistTab = false) 
                    1 -> HomeScreen(viewModel = viewModel, isPlaylistTab = true)  
                    2 -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { selectedItem = 0 } 
                    )
                }
            }
            
            if (viewModel.currentSong != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    PlayerLayer(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, isPlaylistTab: Boolean) {
    var isSearchActive by remember { mutableStateOf(false) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Nueva Playlist") },
            text = { 
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteDialog) {
        val title = if (viewModel.isSelectionMode) "Eliminar canciones" else "Eliminar canción"
        val message = if (viewModel.isSelectionMode)
            "¿Eliminar ${viewModel.selectedSongs.size} canciones seleccionadas?"
        else
            "¿Estás seguro de que quieres eliminar '${songToDelete?.title}' de la lista?"

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.isSelectionMode) {
                            viewModel.deleteSelectedSongs()
                        } else {
                            viewModel.allSongs.remove(songToDelete)
                            viewModel.filterSongs()
                            songToDelete = null
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (viewModel.isSelectionMode) {
                TopAppBar(
                    title = { Text("${viewModel.selectedSongs.size} seleccionados") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (viewModel.selectedSongs.size == viewModel.filteredSongs.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAll()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Seleccionar todos")
                        }

                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            } else {

                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            if (isPlaylistTab) "Playlists" else "Biblioteca",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        if (!isPlaylistTab) { 
                            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                                Icon(
                                    if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                    null
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            if (isPlaylistTab) {
                FloatingActionButton(
                    onClick = { showCreatePlaylistDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.PlaylistAdd, "Nueva Playlist")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
            if (isSearchActive && !isPlaylistTab) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = {
                        viewModel.searchQuery = it
                        viewModel.filterSongs()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Buscar canción...") },
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isPlaylistTab) {
               
                when {
                    viewModel.allSongs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Cargando librería o sin permisos...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    viewModel.filteredSongs.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No se encontraron resultados.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        val listState = rememberLazyListState()
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(bottom = 16.dp) // Espacio extra no necesario aquí, lo maneja el Box padre
                        ) {
                            items(
                                items = viewModel.filteredSongs,
                                key = { it.id },
                                contentType = { "song" } 
                            ) { song ->
                                SongItem(
                                    song = song,
                                    isSelected = viewModel.selectedSongs.contains(song.id),
                                    isSelectionMode = viewModel.isSelectionMode,
                                    onClick = {
                                        if (viewModel.isSelectionMode) {
                                            viewModel.toggleSelection(song.id)
                                        } else {
                                            viewModel.playSong(song)
                                        }
                                    },
                                    onLongClick = {
                                        if (!viewModel.isSelectionMode) {
                                            viewModel.isSelectionMode = true
                                            viewModel.toggleSelection(song.id)
                                        }
                                    },
                                    onDelete = {
                                        songToDelete = song
                                        showDeleteDialog = true
                                    },
                                    playlists = viewModel.playlists,
                                    onAddToPlaylist = { playlist ->
                                        viewModel.addToPlaylist(playlist, song)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Vista de Playlists
                if (viewModel.playlists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay playlists creadas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.playlists, key = { it.id }) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text("${playlist.songIds.size} canciones") },
                                leadingContent = {
                                    Icon(
                                        Icons.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            .padding(8.dp)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    // Lógica futura para abrir playlist
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
