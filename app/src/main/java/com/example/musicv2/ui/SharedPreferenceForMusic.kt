package com.example.musicv2.ui

import android.content.Context
import androidx.core.net.toUri
import com.example.musicv2.data.MusicData
import com.example.musicv2.utils.*

class SharedPreferenceForMusic(ctx: Context){
    private val share = ctx.getSharedPreferences(sharedPreference, Context.MODE_PRIVATE)
    fun returnMusic(): MusicData? =
        MusicData(share.getLong(sharedLong, (-1).toLong()), share.getString(sharedUri, null)?.toUri(), share.getString(
            sharedName, null), share.getString(sharedAlbum, null), share.getLong("sharedDuration", (-1).toLong()))
            .run {
                if (musicId == (-1).toLong() || musicArtUri == null || musicName == null || musicAlbum == null || duration == (-1).toLong()) null
                else this
            }
    fun saveMusic(music: MusicData){
        share.edit().run {
            putLong(sharedLong, music.musicId)
            putString(sharedUri, music.musicArtUri.toString())
            putString(sharedName, music.musicName)
            putString(sharedAlbum, music.musicAlbum)
            putLong("sharedDuration", music.duration)
            apply()
        }
    }
    fun isBlackTheme(): Boolean = share.getBoolean(theme, false)
    fun setBlackTheme(bool: Boolean){
        share.edit().run{
            putBoolean(theme, bool)
            apply()
        }
    }
}
