package com.example.musicv2

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.example.musicv2.data.MusicData
import com.example.musicv2.ui.SharedPreferenceForMusic
import com.example.musicv2.ui.music.MusicPlayer
import com.example.musicv2.utils.*
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

@Suppress("DEPRECATION")
class MusicService : MediaBrowserServiceCompat(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val sharedPref: SharedPreferenceForMusic by instance()

    private lateinit var notificationTarget: NotificationTarget

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private var exoplayer: SimpleExoPlayer? = null

    private var currentMusic = 0
    private var currentMusicNotify: MusicData? = null
    private val queue = ArrayList<MediaSessionCompat.QueueItem>()
    private val musicQueue = mutableListOf<MusicData>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action!!){
                ACTION_PLAY_PAUSE -> if (exoplayer?.isPlaying!!) musicPause() else musicPlay()
                ACTION_STOP -> musicStop()
            }
        }
    }

    private fun notify(state: Int) {
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0 ,
            Intent(applicationContext, MusicPlayer::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }, 0)
        val contentView = RemoteViews(packageName, R.layout.notification)
        contentView.setTextViewText(R.id.notificationMusicName, currentMusicNotify?.musicName)
        contentView.setTextViewText(R.id.notificationMusicAlbum, currentMusicNotify?.musicAlbum)
        if (state == PlaybackStateCompat.STATE_PLAYING)
            contentView.setImageViewResource(R.id.notificationPlayPause, android.R.drawable.ic_media_pause)
        else contentView.setImageViewResource(R.id.notificationPlayPause, android.R.drawable.ic_media_play)
        val builder = NotificationCompat.Builder(applicationContext, notificationChannelDescription).apply {
            setSmallIcon(R.drawable.ic_baseline_music_note_24)
            setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_baseline_music_note_24))
            setContentIntent(pendingIntent)
            setCustomContentView(contentView)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        contentView.setOnClickPendingIntent(R.id.notificationPlayPause, PendingIntent.getBroadcast(applicationContext, 0, Intent().setAction("ACTION_PLAY_PAUSE"), 0))
        contentView.setOnClickPendingIntent(R.id.notificationExit, PendingIntent.getBroadcast(applicationContext, 0, Intent().setAction("ACTION_STOP"), 0))
        val notification = builder.build()
        notificationTarget = NotificationTarget(applicationContext, R.id.notificationMusicArt, contentView, notification, notifyId)
        Glide.with(applicationContext)
            .asBitmap()
            .load(currentMusicNotify?.musicArtUri)
            .error(R.drawable.ic_baseline_music_note_24)
            .transform(RoundedCornersTransformation(80, 0))
            .into(notificationTarget)
        startForeground(notifyId, notification)
    }

    private val handler = Handler()
    private val run = object : Runnable {
        override fun run() {
            try {
                if (exoplayer?.isPlaying!!) updatePlayBackState(PlaybackStateCompat.STATE_PLAYING)
                else {
                    updatePlayBackState(PlaybackStateCompat.STATE_PAUSED)
                    notify(PlaybackStateCompat.STATE_PAUSED)
                }
            }catch (e: Exception){}
            handler.postDelayed(this, 1000)
        }
    }
    private val player = object : Player.EventListener{
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            when(playbackState){
                SimpleExoPlayer.STATE_ENDED -> {
                    when {
                        musicQueue.size > 1 && currentMusic < (musicQueue.size -1) -> musicNext()
                        musicQueue.size == 1 ->{
                            musicSeekTo(0)
                            musicPause()
                        }
                        currentMusic == (musicQueue.size -1) ->{
                            moveCurrentPlaying(musicQueue[0].musicId, false)
                            Handler().postDelayed({
                                musicPause()
                            }, 500)
                        }
                    }
                }
                else ->{}
            }
        }
    }

    private val mediaSessionCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP) object: MediaSessionCompat.Callback() {
        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            musicSeekTo(pos)
        }
        override fun onPause() {
            super.onPause()
            musicPause()
        }
        override fun onPlay() {
            super.onPlay()
            musicPlay()
        }
        override fun onStop() {
            super.onStop()
            musicStop()
        }
        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            playFromUri(uri, extras)
        }
        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            moveCurrentPlaying(id)
        }
        override fun onSkipToNext() {
            super.onSkipToNext()
            musicNext()
        }
        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            musicPrevious()
        }
        override fun onCustomAction(action: String?, extras: Bundle?) {
            super.onCustomAction(action, extras)
            when(action){
                add_Queue -> addQueue(extras)
                update_Queue -> updateQueue(extras)
            }
        }
    }

    private fun playFromUri(uri: Uri?, extras: Bundle?){
        extras?.apply {
            try {
                getParcelable<MusicData>("music")?.let { music->
                    musicQueue.clear()
                    musicQueue.add(music)
                    currentMusic = musicQueue.indexOf(music)

                    initPlayer(analyseSource(uri!!)!!, music, getBoolean(isplayable, true))
                    queue.clear()
                    MediaSessionCompat.QueueItem(getDescription(music), music.musicId).let {
                        queue.add(it)
                    }
                    mediaSession?.setQueue(queue)
                }
            }catch (e:Exception){}
        }
    }

    private fun updateQueue(extras: Bundle?){
        extras?.apply {
            try {
                val current = musicQueue[currentMusic]
                getParcelableArrayList<MusicData>(updatedQ).let {
                    musicQueue.clear()
                    queue.clear()
                    musicQueue.addAll(it as MutableList<MusicData>)
                    it.forEach { music -> MediaSessionCompat.QueueItem(getDescription(music), music.musicId).let { item -> queue.add(item) } }
                    mediaSession?.setQueue(queue)
                }
                currentMusic = musicQueue.indexOf(current)
            }catch (e: Exception){}
        }
    }

    private fun getDescription(music: MusicData) =
        MediaDescriptionCompat.Builder()
            .setExtras(Bundle().apply { putParcelable("music", music) })
            .build()

    private fun moveCurrentPlaying(id: Long, isPlayable: Boolean = true){
        musicQueue.forEach {
            try {
                if (it.musicId == id) {
                    initPlayer(analyseSource(returnMusicUri(it.musicId))!!, it, isPlayable)
                    currentMusic = musicQueue.indexOf(it)
                    return
                }
            }catch (e: Exception){}
        }
    }

    private fun musicPrevious(){
        if (currentMusic > 0){
            try {
                currentMusic -= 1
                musicQueue.elementAt(currentMusic).let {music->
                    initPlayer(analyseSource(returnMusicUri(music.musicId))!!, music,true)
                }
            }catch (e: Exception){}
        }
    }

    private fun musicNext() {
        if (musicQueue.size > 1 && currentMusic < (musicQueue.size -1)){
            try {
                currentMusic += 1
                musicQueue.elementAt(currentMusic).let { music->
                    initPlayer(analyseSource(returnMusicUri(music.musicId))!!, music, true)
                }
            }catch (e: Exception){}
        }
    }

    private fun musicPlay(){
        exoplayer?.apply {
            playWhenReady = true
            updatePlayBackState(PlaybackStateCompat.STATE_PLAYING)
            mediaSession?.isActive = true
            notify(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    private fun musicPause(isNotify:Boolean=true){
        exoplayer?.apply {
            playWhenReady = false
            updatePlayBackState(PlaybackStateCompat.STATE_PAUSED)
            if (isNotify) notify(PlaybackStateCompat.STATE_PAUSED)
        }
    }

    private fun addQueue(extras: Bundle?){
        extras?.getParcelable<MusicData>(musicForService)?.let {
            try {
                musicQueue.add(it)
                MediaSessionCompat.QueueItem(getDescription(it), it.musicId).let { item-> queue.add(item) }
                mediaSession?.setQueue(queue)
            }catch (e: Exception){}
        }
    }

    private fun musicPosition(): Long = try { exoplayer?.currentPosition!! }catch (e: Exception) { 0 }

    private fun musicStop(){
        updatePlayBackState(PlaybackStateCompat.STATE_STOPPED)
        stopForeground(true)
        musicQueue.clear()
        queue.clear()
        mediaSession?.setQueue(null)
        handler.removeCallbacks(run)
        exoplayer?.removeListener(player)
        exoplayer?.playWhenReady = false
        exoplayer?.release()
        exoplayer = null
        mediaSession?.isActive = false
        mediaSession?.release()
        stopSelf()
    }

    private fun musicSeekTo(seek: Long){ try{ exoplayer?.seekTo(seek)}catch (e: Exception){} }

    private fun analyseSource(uri: Uri): MediaSource?{
        val mediaDataSourceFactory = DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, getString(R.string.app_name)), DefaultBandwidthMeter())
        return when(Util.inferContentType(uri)){
            C.TYPE_DASH -> DashMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri)
            C.TYPE_HLS -> HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri)
            C.TYPE_SS -> SsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER-> ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                .setExtractorsFactory(DefaultExtractorsFactory()).createMediaSource(uri)
            else -> {
                Toast.makeText(baseContext, "Unsupported format!!!", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }


    private fun setMetaData(music: MusicData){
        MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.musicName)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM, music.musicAlbum)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, music.duration)
            putString(MediaMetadataCompat.METADATA_KEY_ART_URI, music.musicArtUri?.toString())
            mediaSession?.setMetadata(build())
        }
        currentMusicNotify = music
    }

    private fun initPlayer(mediaSource: MediaSource, music: MusicData, isPlayable: Boolean=true){
        exoplayer?.apply {
            setAudioAttributes(initAudioAttr(), true)
            setHandleAudioBecomingNoisy(true)
            prepare(mediaSource)
            setMetaData(music)
            if (isPlayable) musicPlay()
            else musicPause(false)
            sharedPref.saveMusic(music)
        }
    }

    private fun initAudioAttr(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC).build()

    private fun getPlayStateAction(): Long =
        PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY_FROM_URI or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null){
            val keyEvent = MediaButtonReceiver.handleIntent(mediaSession, intent)
            if (keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE){
                if (exoplayer?.isPlaying!!) musicPause()
                else musicPlay()
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        exoplayer = SimpleExoPlayer.Builder(applicationContext).build()
        mediaSession = MediaSessionCompat(applicationContext, mediaService).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
            stateBuilder = PlaybackStateCompat.Builder().setActions(getPlayStateAction())
            setPlaybackState(stateBuilder.build())
            setCallback(mediaSessionCallback)
            setSessionToken(sessionToken)
            isActive = true
            setMediaButtonReceiver(PendingIntent.getBroadcast(applicationContext, 0, Intent().setAction("ACTION_PLAY_PAUSE"), 0))
            //setMediaButtonReceiver(null)
        }
        exoplayer?.addListener(player)
        registerReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_STOP)
            addAction(ACTION_PLAY_PAUSE)
        })
        handler.postDelayed(run, 1000)
    }

    private fun updatePlayBackState(state: Int){
        mediaSession?.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(state, musicPosition(), 1.0F, SystemClock.elapsedRealtime())
            .setActiveQueueItemId(currentMusic.toLong())
            .build())
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        musicStop()
        super.onDestroy()
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) { result.sendResult(null) }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? = BrowserRoot(getString(R.string.app_name), null)

}