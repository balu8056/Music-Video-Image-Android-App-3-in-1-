package com.example.musicv2.ui

import android.app.ActivityManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.lifecycle.*
import com.example.musicv2.MusicService
import com.example.musicv2.data.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.File

interface MediaChange{
    fun onMetaChanged(metadata: MediaMetadataCompat?)
    fun onStateChanged(state: PlaybackStateCompat?)
}

class ViewModelFactory(private val context: Context): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(context) as T
    }
}

class MainViewModel(private val context: Context): ViewModel(), KodeinAware {
    override val kodein: Kodein by kodein(context)
    val sharedPref: SharedPreferenceForMusic by instance()

    private val _musicFromContent = MutableLiveData<MutableList<MusicData>>()
    val musicFromContent: LiveData<MutableList<MusicData>> = _musicFromContent

    private val _videoContent = MutableLiveData<ArrayList<VideoData>>()
    val videoContent: LiveData<ArrayList<VideoData>> = _videoContent
    private val _videoByFolder = MutableLiveData<MutableList<VideoByFolder>>()
    val videoByFolder: LiveData<MutableList<VideoByFolder>> = _videoByFolder

    private val _imageContent = MutableLiveData<ArrayList<ImageData>>()
    val imageContent: LiveData<ArrayList<ImageData>> = _imageContent
    private val _imageByFolder = MutableLiveData<MutableList<ImageByFolder>>()
    val imageByFolder: LiveData<MutableList<ImageByFolder>> = _imageByFolder

    var mediaChange: MediaChange? = null

    var mediaBrowserCompat: MediaBrowserCompat? = null

    private val _mainController = MutableLiveData<MediaControllerCompat>()
    val controller : LiveData<MediaControllerCompat> = _mainController

    val mediaControllerCompat = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            mediaChange?.onMetaChanged(metadata)
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            mediaChange?.onStateChanged(state)
        }
    }

    private val connectionCallback = object: MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            mediaBrowserCompat?.sessionToken?.apply { _mainController.value = MediaControllerCompat(context.applicationContext,this) }
        }
    }

    init {
        mediaBrowserCompat = MediaBrowserCompat(context, ComponentName(context, MusicService::class.java),
            connectionCallback, null)
    }


    fun getImageByFolder(imageData: MutableList<ImageData>){
        val tempImageByFolder = mutableListOf<ImageByFolder>()
        val albumSet = mutableSetOf<String>()
        imageData.forEach { albumSet.add(it.folderName.toString()) }
        albumSet.forEach { folder-> imageData.filter { folder == it.folderName }.let { tempImageByFolder.add(ImageByFolder(folder, it as ArrayList<ImageData>)) } }
        _imageByFolder.value = tempImageByFolder
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getImageFromContent(folder: String? = null): MutableList<ImageData>? {
        val tempImages = mutableListOf<ImageData>()
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder)
        cursor?.use {
            while (it.moveToNext())
                try { it.getString(it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)).let { DATA ->
                        tempImages.add(ImageData(
                                it.getLong(it.getColumnIndex(MediaStore.Images.Media._ID)),
                                it.getString(it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)),
                                DATA, File(DATA).parentFile?.name,
                                it.getLong(it.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN))
                            ))
                    }
                }catch (e: Exception){}
        }
        cursor?.close()
        return if (folder != null){
            _imageContent.value = tempImages.filter { it.folderName == folder } as ArrayList<ImageData>
            null
        } else tempImages
    }

    fun getVideoByFolder(imageData: MutableList<VideoData>){
        val tempVideoByFolder = mutableListOf<VideoByFolder>()
        val albumSet = mutableSetOf<String>()
        imageData.forEach { albumSet.add(it.folderName.toString()) }
        albumSet.forEach { folder-> imageData.filter { folder == it.folderName }.let { tempVideoByFolder.add(VideoByFolder(folder, it as ArrayList<VideoData>)) } }
        _videoByFolder.value = tempVideoByFolder
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getVideoFromContent(folder: String? = null): MutableList<VideoData>?{
        val tempVideo = mutableListOf<VideoData>()
        val sortOrder = "${MediaStore.Video.VideoColumns.DATE_TAKEN} DESC"
        val cursor = context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder)
        cursor?.use {
            while (it.moveToNext())
                try { it.getString(it.getColumnIndex(MediaStore.Video.VideoColumns.DATA)).let { DATA->
                        tempVideo.add(VideoData(it.getLong(it.getColumnIndex(MediaStore.Video.Media._ID)),
                            it.getString(it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)),
                            it.getLong(it.getColumnIndex(MediaStore.Video.Media.DURATION)),
                            it.getLong(it.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)),
                            File(DATA).parentFile?.name
                        )) }
                }catch (e: Exception){}
            }
        cursor?.close()
        return if (folder != null) {
            _videoContent.value = tempVideo.filter { it.folderName == folder } as ArrayList<VideoData>
            null
        } else tempVideo
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMusicFromContent() {
        val tempMusic = mutableListOf<MusicData>()
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        val cursor: Cursor? = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, sortOrder)
        cursor?.use {
            while (it.moveToNext())
                tempMusic.add(MusicData(it.getLong(it.getColumnIndex(MediaStore.Audio.Media._ID)),
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), it.getLong(it.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID))),
                        it.getString(it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),
                        it.getString(it.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                        it.getLong(it.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    ))
        }
        _musicFromContent.value = tempMusic
        cursor?.close()
    }

    @Suppress("DEPRECATION")
    fun getRunService(): Boolean{
        return(context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE).any { it.service.className == "${context.packageName}.MusicService" }
    }

    override fun onCleared() {
        _musicFromContent.value?.clear()
        _videoByFolder.value?.clear()
        _imageByFolder.value?.clear()
        super.onCleared()
    }
}