package com.example.musicv2.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.musicv2.R
import com.example.musicv2.data.ImageData
import com.example.musicv2.data.MusicData
import com.example.musicv2.data.VideoData
import com.example.musicv2.ui.SharedPreferenceForMusic
import java.util.concurrent.TimeUnit

const val storageRequestCode = 1234

const val notifyId = 1234

const val mediaService = "mediaService"

const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
const val ACTION_STOP = "ACTION_STOP"
const val add_Queue = "add_Queue"
const val update_Queue = "update_Queue"
const val isplayable = "isplayable"
const val updatedQ = "updatedQ"
const val musicForService = "music"

const val albumName = "albumName"
const val videoAlbumName = "videoAlbumName"

const val imagePosition = "imagePosition"

const val sharedPreference = "sharedPreference"
const val sharedLong = "sharedLong"
const val sharedUri = "sharedUri"
const val sharedName = "sharedName"
const val sharedAlbum = "sharedAlbum"

const val notificationChannelDescription = "Music"

const val theme = "Theme"
const val lightMode = "lightMode"
const val darkMode = "darkMode"

@RequiresApi(Build.VERSION_CODES.KITKAT)
const val videoFullScreenFlag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Window.full(){
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    decorView.systemUiVisibility = videoFullScreenFlag
    statusBarColor = Color.TRANSPARENT
}

fun Menu.vOff(id: Int){ getItem(id)?.isVisible = false }

fun Context.orientationCheck(recyclerView: RecyclerView, portSpan: Int, landSpan: Int){
    when(resources.configuration.orientation){
        Configuration.ORIENTATION_PORTRAIT -> recyclerView.layoutManager = GridLayoutManager(applicationContext, portSpan)
        Configuration.ORIENTATION_LANDSCAPE -> recyclerView.layoutManager = GridLayoutManager(applicationContext, landSpan)
    }
}

fun checkMenu(item: MenuItem, sharedPref: SharedPreferenceForMusic): Boolean{
    item.isChecked = !item.isChecked
    sharedPref.setBlackTheme(item.isChecked)
    settingTheme(item.isChecked)
    return sharedPref.isBlackTheme()
}

fun Context.startActivityFromUtil(classToStart: Class<out Any>, name: String?=null, value1: MusicData?=null, value2: VideoData?=null, isSingleTop: Boolean=true, value3: ArrayList<ImageData>?=null){
    startActivity(Intent(applicationContext, classToStart).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isSingleTop) addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        when {
            value1 != null -> putExtra(name, value1)
            value2 != null -> putExtra(name, value2)
            value3 != null -> putExtra(name, value3)
        }
    })
}

fun returnMusicUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

fun returnVideoUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

fun returnImageUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

fun View.viewVisibilityOn() { visibility = View.VISIBLE }

fun View.viewGone(){ visibility = View.GONE }

fun settingTheme(bool: Boolean){
    if (bool) ThemeHelper.applyTheme(darkMode)
    else ThemeHelper.applyTheme(lightMode)
}

fun Context.setImage(uri: Uri?, fit: Boolean=true, center: Boolean = false): RequestBuilder<Drawable> =
    Glide.with(applicationContext)
        .load(uri)
        .error(R.drawable.colored)
        .let { when {
                fit -> it.fitCenter()
                center -> it.centerCrop()
                else -> it
            } }

fun Context.setGalleryImage(uri: Uri?, fit: Boolean=true, center: Boolean = false): RequestBuilder<Bitmap> =
    Glide.with(applicationContext)
        .asBitmap()
        .load(uri)
        .error(R.drawable.colored)
        .skipMemoryCache(true)
        .let {
            when {
                fit -> it.fitCenter()
                center -> it.centerCrop()
                else -> it
            }
        }

fun Context.setImageGlide(uri: Uri?, fit: Boolean=true, center: Boolean = false): RequestBuilder<Drawable> =
    Glide.with(applicationContext)
        .load(uri)
        .error(R.drawable.colored)
        .skipMemoryCache(true)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .let {
            when {
                fit -> it.fitCenter()
                center -> it.centerCrop()
                else -> it
            }
        }

fun getTime(milli: Long): String = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(milli), TimeUnit.MILLISECONDS.toMinutes(milli)-TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milli)), TimeUnit.MILLISECONDS.toSeconds(milli)-TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milli)))

fun getClockTime(milli: Int): String =
    ((milli/1000) % 60).let {
        if (it < 10) "${(milli/ 60000) % 60} : 0$it"
        else "${(milli/ 60000) % 60} : $it"
    }

val permissionsArray = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

fun Context.checkPermission(): Boolean = ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.WRITE_SETTINGS) != PackageManager.PERMISSION_GRANTED