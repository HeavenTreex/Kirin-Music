package com.prk.kirinmusic.model

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

object SongRepository {
    fun getSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        // Seleccionamos solo música (IS_MUSIC != 0)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val path = cursor.getString(dataColumn) ?: ""

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                val lyrics = getLyrics(path)

                songList.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = contentUri.toString(),
                        albumArtUri = albumArtUri,
                        duration = duration,
                        path = path,
                        lyrics = lyrics
                    )
                )
            }
        }
        return songList
    }

    private fun getLyrics(audioPath: String): String? {
        if (audioPath.isEmpty()) return null
        return try {
            val audioFile = File(audioPath)
            val lrcFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists()) {
                lrcFile.readText()
            } else {
                val txtFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}.txt")
                if (txtFile.exists()) {
                    txtFile.readText()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
