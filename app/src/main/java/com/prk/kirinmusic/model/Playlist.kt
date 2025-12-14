package com.prk.kirinmusic.model

data class Playlist(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val songIds: List<Long> = emptyList()
)
