package com.prk.kirinmusic.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.prk.kirinmusic.MediaPlaybackService
import com.prk.kirinmusic.model.Playlist
import com.prk.kirinmusic.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("kirin_music_prefs", Context.MODE_PRIVATE)

    var allSongs = mutableStateListOf<Song>()
    var filteredSongs = mutableStateListOf<Song>()
    var searchQuery by mutableStateOf("")

    var isSelectionMode by mutableStateOf(false)
    var selectedSongs = mutableStateListOf<Long>()

    var currentSong by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var isPlayerExpanded by mutableStateOf(false)

    var currentPosition by mutableStateOf(0f)
    var duration by mutableStateOf(0f)

    var isShuffleEnabled by mutableStateOf(false)
    var isRepeatEnabled by mutableStateOf(false)

    var sleepTimerMinutes by mutableStateOf(0)
    private var sleepTimerJob: Job? = null

    var playbackSpeed by mutableStateOf(1.0f)
    var cacheSizeText by mutableStateOf("Calculando...")
    
    // Theme settings
    var isDynamicTheme by mutableStateOf(true)
    var currentThemeColor by mutableStateOf(ThemeColor.Blue)

    // Playlists
    var playlists = mutableStateListOf<Playlist>()

    private var playbackService: MediaPlaybackService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlaybackService.LocalBinder
            playbackService = binder.getService()
            isBound = true

            playbackService?.onNext = {
                skipNext()
            }
            playbackService?.onPrev = {
                skipPrev()
            }

            if (playbackService?.currentSong != null) {
                syncWithService()
            } else if (currentSong != null) {
                // Servicio vacío, pero tenemos canción restaurada
                playbackService?.playNewSong(currentSong!!, startPlaying = false)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isBound = false
        }
    }

    init {
        val intent = Intent(application, MediaPlaybackService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startProgressLoop()
        calculateCacheSize()
    }

    private fun startProgressLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (isPlaying && playbackService != null) {
                    try {
                        playbackService?.let { service ->
                            currentPosition = service.currentPosition.toFloat()
                            duration = service.duration.toFloat()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                delay(500)
            }
        }
    }
    
    fun setTheme(dynamic: Boolean, color: ThemeColor) {
        isDynamicTheme = dynamic
        currentThemeColor = color
    }

    fun toggleSelection(songId: Long) {
        if (selectedSongs.contains(songId)) {
            selectedSongs.remove(songId)
            if (selectedSongs.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedSongs.add(songId)
        }
    }

    fun selectAll() {
        selectedSongs.clear()
        selectedSongs.addAll(filteredSongs.map { it.id })
    }

    fun clearSelection() {
        selectedSongs.clear()
        isSelectionMode = false
    }

    fun deleteSelectedSongs() {
        val songstoKeep = allSongs.filter { !selectedSongs.contains(it.id) }
        allSongs.clear()
        allSongs.addAll(songstoKeep)
        filterSongs()
        clearSelection()
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes = minutes
        sleepTimerJob?.cancel()

        if (minutes > 0) {
            Toast.makeText(getApplication(), "Temporizador: $minutes min", Toast.LENGTH_SHORT).show()
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                if (isActive) {
                    pauseMusic()
                    sleepTimerMinutes = 0
                    Toast.makeText(getApplication(), "Música detenida", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(getApplication(), "Temporizador cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    fun changePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        playbackService?.setPlaybackSpeed(speed)
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        // Aquí podrías comunicar al servicio si maneja la cola, 
        // o reorganizar la lista localmente si se implementa cola en ViewModel
        Toast.makeText(getApplication(), if(isShuffleEnabled) "Aleatorio activado" else "Aleatorio desactivado", Toast.LENGTH_SHORT).show()
    }

    fun toggleRepeat() {
        isRepeatEnabled = !isRepeatEnabled
        Toast.makeText(getApplication(), if(isRepeatEnabled) "Repetir activado" else "Repetir desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun pauseMusic() {
        if (isPlaying) {
            isPlaying = false
            playbackService?.togglePlayPause()
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            cacheSizeText = "12.5 MB"
        }
    }

    fun clearImageCache() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val imageLoader = ImageLoader(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()

            delay(500)
            cacheSizeText = "0 MB"
            Toast.makeText(context, "Caché borrada", Toast.LENGTH_SHORT).show()
        }
    }

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(name = name)
        playlists.add(newPlaylist)
    }

    fun addToPlaylist(playlist: Playlist, song: Song) {
        val index = playlists.indexOfFirst { it.id == playlist.id }
        if (index != -1) {
            val currentSongs = playlists[index].songIds.toMutableList()
            if (!currentSongs.contains(song.id)) {
                currentSongs.add(song.id)
                playlists[index] = playlists[index].copy(songIds = currentSongs)
                Toast.makeText(getApplication(), "Añadida a ${playlist.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(getApplication(), "La canción ya está en la playlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadLocalMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val context = getApplication<Application>().applicationContext
            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            val loadedSongs = mutableListOf<Song>()

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn)
                    val artist = it.getString(artistColumn)
                    val album = it.getString(albumColumn)
                    val durationMs = it.getLong(durationColumn)
                    val path = it.getString(dataColumn)
                    val albumId = it.getLong(albumIdColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()

                    // Eliminado para mejorar velocidad de carga
                    // val lyrics = getLyricsFromUri(context, contentUri)

                    loadedSongs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            uri = contentUri.toString(),
                            albumArtUri = albumArtUri,
                            duration = durationMs,
                            path = path,
                            lyrics = null // Se carga bajo demanda si es necesario
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) {
                allSongs.clear()
                allSongs.addAll(loadedSongs)
                filterSongs()

                // Restaurar última canción si no hay una activa
                if (currentSong == null) {
                    val lastId = prefs.getLong("last_song_id", -1L)
                    if (lastId != -1L) {
                        val song = allSongs.find { it.id == lastId }
                        if (song != null) {
                            currentSong = song
                            playbackService?.playNewSong(song, startPlaying = false)
                        }
                    }
                }
            }
        }
    }

    private fun getLyricsFromUri(context: Context, uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun findLrcLyrics(audioPath: String): String? {
        try {
            val audioFile = File(audioPath)
            val lrcFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists()) {
                return lrcFile.readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun filterSongs() {
        val query = searchQuery.lowercase()
        filteredSongs.clear()
        if (query.isEmpty()) {
            filteredSongs.addAll(allSongs)
        } else {
            filteredSongs.addAll(allSongs.filter {
                it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
            })
        }
    }

    fun playSong(song: Song) {
        currentSong = song
        isPlaying = true
        prefs.edit().putLong("last_song_id", song.id).apply()
        
        // Cargar letras asíncronamente si no están cargadas
        if (song.lyrics == null) {
            viewModelScope.launch(Dispatchers.IO) {
                // Intentar primero buscar .lrc local
                var lyrics = findLrcLyrics(song.path)
                
                // Si no hay .lrc, buscar en metadata
                if (lyrics == null) {
                    lyrics = getLyricsFromUri(getApplication(), Uri.parse(song.uri))
                }

                val updatedSong = song.copy(lyrics = lyrics)
                withContext(Dispatchers.Main) {
                    if (currentSong?.id == updatedSong.id) {
                        currentSong = updatedSong
                    }
                    // Actualizar en la lista principal también si se desea
                    val index = allSongs.indexOfFirst { it.id == song.id }
                    if (index != -1) {
                        allSongs[index] = updatedSong
                        filterSongs()
                    }
                }
            }
        }
        
        playbackService?.playNewSong(song)
        playbackService?.setPlaybackSpeed(playbackSpeed)
        currentPosition = 0f
        duration = 0f
    }

    fun togglePlayPause() {
        if (currentSong != null) {
            isPlaying = !isPlaying
            playbackService?.togglePlayPause()
        }
    }

    fun skipNext() {
        val currentIndex = allSongs.indexOfFirst { it.id == currentSong?.id }
        
        val nextIndex = if (isShuffleEnabled) {
            // Simple shuffle logic: pick random index
             (0 until allSongs.size).random()
        } else {
             currentIndex + 1
        }

        if (nextIndex >= 0 && nextIndex < allSongs.size) {
            playSong(allSongs[nextIndex])
        } else if (isRepeatEnabled && allSongs.isNotEmpty()) {
             playSong(allSongs[0]) // Loop back to start
        }
    }

    fun skipPrev() {
        val currentIndex = allSongs.indexOfFirst { it.id == currentSong?.id }
        if (currentIndex > 0) {
            playSong(allSongs[currentIndex - 1])
        }
    }

    fun seekTo(position: Float) {
        playbackService?.seekTo(position.toInt())
        currentPosition = position
    }

    private fun syncWithService() {
        playbackService?.let { service ->
            service.currentSong?.let {
                currentSong = it
                isPlaying = service.isPlaying
                duration = service.duration.toFloat()
                currentPosition = service.currentPosition.toFloat()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}

enum class ThemeColor {
    Blue, Green, Red, Purple, Orange
}
