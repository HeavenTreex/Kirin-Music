package com.prk.kirinmusic.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String,
    val albumArtUri: String,
    val duration: Long,
    val path: String,
    val lyrics: String? = null
)