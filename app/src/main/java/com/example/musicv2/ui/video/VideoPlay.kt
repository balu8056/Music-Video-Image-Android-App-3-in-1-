package com.example.musicv2.ui.video

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.example.musicv2.R
import com.example.musicv2.data.VideoData
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.utils.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.controls_exoplayer.view.*
import kotlinx.android.synthetic.main.video_play.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class VideoPlay: AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()
    private lateinit var videoModel: MainViewModel

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var audioManager: AudioManager

    private var leftOrRight = 0

    private var resWind = 0
    private var lastPos = 0L

    private var currentVideo = 0
    private val videoPlayList = arrayListOf<VideoData>()

    private lateinit var ges :GestureDetector

    private val playerListener = object: Player.EventListener{
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
        override fun onSeekProcessed() {}
        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}
        override fun onPlayerError(error: ExoPlaybackException) {
            Log.e("play", error.message.toString())
        }//finish() }
        override fun onLoadingChanged(isLoading: Boolean) {}
        override fun onPositionDiscontinuity(reason: Int) {}
        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
        override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when(playbackState){
                Player.STATE_BUFFERING -> progress_bar.viewVisibilityOn()
                Player.STATE_READY -> progress_bar.viewGone()
                ExoPlayer.STATE_ENDED->{
                    progress_bar.viewGone()
                    when{videoPlayList.size > 1 && currentVideo < (videoPlayList.size -1) ->{ nextVideo() } }
                }
                else ->{}
            }
        }
    }

    private val handler = Handler()
    private val run = Runnable {
        playerView.hideController()
    }

    private val run2 = Runnable {
        if (!playerView.isControllerVisible){
            playerView.showController()
            handler.postDelayed(run, 5000)
        }else{
            handler.removeCallbacks(run)
            playerView.hideController()
        }
    }

    private val hideVolumeSeek = Runnable { labelVolume.viewGone() }
    private val hideBrightnessSeek = Runnable { labelBright.viewGone() }

    private val hideRW10 = Runnable { labelRW.viewGone() }
    private val hideFF10 = Runnable { labelFF.viewGone() }

    private val hideSeek = Runnable { labelSeek.viewGone() }

    private var oldBrightness = -1
    private fun getBrightness() = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
    private fun setBrightness(value: Int){
        try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        }catch (e: Exception){}
    }

    private val scale = object: GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            handler.removeCallbacks(run2)
            playerView.hideController()
            try {
                if (e?.x!! < leftOrRight ) {
                    handler.removeCallbacks(hideRW10)
                    labelRW.viewVisibilityOn()
                    try { simpleExoPlayer.seekTo(simpleExoPlayer.currentPosition - 10000) }catch (e: Exception){}
                    handler.postDelayed(hideRW10, 500)
                }else if (e.x > leftOrRight) {
                    handler.removeCallbacks(hideFF10)
                    labelFF.viewVisibilityOn()
                    try{ simpleExoPlayer.seekTo(simpleExoPlayer.currentPosition + 10000) }catch (e: Exception){}
                    handler.postDelayed(hideFF10, 500)
                }
            }catch (e: Exception){}
            return true
        }
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            handler.removeCallbacks(run2)
            playerView.hideController()
            try {
                val y2 = e2?.y!!
                val diffX = (ceil(e2.x - e1?.x!!).toDouble())
                val diffY = (ceil(e2.y - e1.y).toDouble())
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > 0){
                        handler.removeCallbacks(hideSeek)
                        labelSeek.viewVisibilityOn()
                        if (diffX > 0) {
                            try{
                                if ((simpleExoPlayer.currentPosition + 2) < simpleExoPlayer.duration){
                                    (simpleExoPlayer.currentPosition + 2000).let {
                                        labelSeek.text =
                                            if (simpleExoPlayer.duration >=3600000) getTime(it)
                                            else getClockTime(it.toInt())
                                        simpleExoPlayer.seekTo(it)
                                    }

                                }
                            }catch (e: Exception){}
                        }
                        else {
                            try {
                                if ((simpleExoPlayer.currentPosition - 2) > 0 ) {
                                    (simpleExoPlayer.currentPosition - 2000).let {
                                        labelSeek.text =
                                            if (simpleExoPlayer.duration >= 3600000) getTime(it)
                                            else getClockTime(it.toInt())
                                        simpleExoPlayer.seekTo(it)
                                    }
                                }
                            }catch (e: Exception){}
                        }
                        handler.postDelayed(hideSeek, 500)
                    }
                }
                if (abs(diffY) > abs(diffX)) {
                    if (e1.x < leftOrRight) {
                        handler.removeCallbacks(hideBrightnessSeek)
                        labelBright.viewVisibilityOn()
                        if (e1.y < y2) {
                            try {
                                getBrightness().let {
                                    if (it >= 5) setBrightness(it - 5)
                                    else setBrightness(0)
                                }
                                brightnessSeekBar.progress = getBrightness()
                            }catch (e: Exception){}
                        }          //down swipe brightness decrease
                        else if (e1.y > y2) {
                            try {
                                getBrightness().let {
                                    if (it <= 250) setBrightness(it + 5)
                                    else setBrightness(255)
                                }
                                brightnessSeekBar.progress = getBrightness()
                            }catch (e: Exception){}
                        }          //up  swipe brightness increase
                        handler.postDelayed(hideBrightnessSeek, 500)
                    } else if (e1.x > leftOrRight ) {
                        handler.removeCallbacks(hideVolumeSeek)
                        labelVolume.viewVisibilityOn()
                        if (e1.y < y2) {
                            try {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                                volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }catch (e: Exception){}
                        }//down swipe volume decrease
                        else if (e1.y > y2) {
                            try {
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                                volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }catch (e: Exception){}
                        }//up  swipe volume increase
                        handler.postDelayed(hideVolumeSeek,500)
                    }
                }
            }catch (e: Exception){}
            return true
        }
        override fun onDown(e: MotionEvent?): Boolean {
            handler.removeCallbacks(run2)
            handler.postDelayed(run2, 650)
            return true
        }
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_play)
        window.full()

        videoModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        leftOrRight = (Resources.getSystem().displayMetrics.widthPixels) * 50 / 100

        ges = GestureDetector(applicationContext, scale)

        playerView.setOnTouchListener{ _, event -> ges.onTouchEvent(event) }

        playerView.rotateScreen.setOnClickListener {
            when(resources.configuration.orientation){
                Configuration.ORIENTATION_LANDSCAPE -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    leftOrRight = (Resources.getSystem().displayMetrics.widthPixels) * 40 / 100
                }
                Configuration.ORIENTATION_PORTRAIT -> {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    leftOrRight = (Resources.getSystem().displayMetrics.widthPixels) * 40 / 100
                }
            }
        }
        playerView.exoControlToolBar.exoControlExit.setOnClickListener { finish() }

        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        getBrightness().let {
            brightnessSeekBar.progress = getBrightness()
            oldBrightness = it
        }

        if (!Settings.System.canWrite(applicationContext))
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) { playerView.pipMode.viewGone() }
        else { playerView.pipMode.viewVisibilityOn() }

        playerView.pipMode.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                val display = windowManager.defaultDisplay
                val p = Point()
                display.getSize(p)
                val pip = PictureInPictureParams.Builder()
                pip.setAspectRatio(Rational(p.x, p.y))
                enterPictureInPictureMode(pip.build())
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if (isInPictureInPictureMode) playerView.setOnTouchListener{ _, _ -> true }
        else playerView.setOnTouchListener{ _, event -> ges.onTouchEvent(event) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startPlayer(){
        if (resWind == 0 && lastPos == 0L){
            intent.extras?.getString("folderName")?.let{ videoModel.getVideoFromContent(it) }
            videoModel.videoContent.observe(this, androidx.lifecycle.Observer {
                videoPlayList.clear()
                videoPlayList.addAll(it)
                intent.extras?.getInt("position", 0)?.let {position->
                    currentVideo = position
                    initializePlayer(currentVideo)
                    Log.e("onResume", "$position")
                }
            })
        }else initializePlayer(currentVideo)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        startPlayer()
        super.onResume()
    }

    @Suppress("DEPRECATION")
    private fun analyseSource(uri: Uri): MediaSource?{
        val mediaDataSourceFactory = DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, getString(R.string.app_name)), DefaultBandwidthMeter())
        return when(Util.inferContentType(uri)){
            C.TYPE_DASH ->DashMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri)
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

    private fun previousVideo(){
        if (currentVideo > 0){
            try {
                releasePlayer()
                resWind = 0;lastPos = 0L
                currentVideo -= 1
                initializePlayer(currentVideo)
            }catch (e: Exception){}
        }
    }

    private fun nextVideo(){
        if (videoPlayList.size > 1 && currentVideo < (videoPlayList.size -1)){
            try{
                releasePlayer()
                resWind = 0;lastPos = 0L
                currentVideo += 1
                initializePlayer(currentVideo)
            }catch (e: Exception){}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializePlayer(current: Int) {
        simpleExoPlayer = SimpleExoPlayer.Builder(applicationContext).build()

        simpleExoPlayer.setAudioAttributes(initAudioAttr(), true)
        simpleExoPlayer.setHandleAudioBecomingNoisy(true)
        simpleExoPlayer.addListener(playerListener)

        try { analyseSource(returnVideoUri(videoPlayList[current].videoId))?.let { simpleExoPlayer.prepare(it) }
        } catch (e: Exception){}

        if (current > 0 && videoPlayList.size > 0) playerView.controlPrevious.viewVisibilityOn()
        else playerView.controlPrevious.viewGone()
        if (videoPlayList.size > 1 && currentVideo < (videoPlayList.size -1)) playerView.controlNext.viewVisibilityOn()
        else playerView.controlNext.viewGone()
        playerView.controlPrevious.setOnClickListener { previousVideo() }
        playerView.controlNext.setOnClickListener { nextVideo() }

        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.player = simpleExoPlayer
        playerView.keepScreenOn = true

        try { playerView.exoControlToolBar.exoTitle.text = videoPlayList[current].name
        }catch (e: Exception){}

        playerView.requestFocus()

        if (resWind != C.INDEX_UNSET && lastPos != 0L) {
            simpleExoPlayer.seekTo(resWind, lastPos)
            simpleExoPlayer.playWhenReady = false
        }
        else simpleExoPlayer.playWhenReady = true
    }

    private fun initAudioAttr(): AudioAttributes =
        AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MUSIC).build()

    private fun releasePlayer() {
        simpleExoPlayer.release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && hasFocus)
            window.decorView.systemUiVisibility = videoFullScreenFlag
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onPause() {
        if (oldBrightness != -1) setBrightness(oldBrightness)
        resWind = simpleExoPlayer.currentWindowIndex
        lastPos = if(simpleExoPlayer.isCurrentWindowSeekable) max(0L, simpleExoPlayer.currentPosition) else C.TIME_UNSET
        releasePlayer()
        super.onPause()
    }

    override fun onStop() {
        releasePlayer()
        super.onStop()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        videoPlayList.clear()
        releasePlayer()
        simpleExoPlayer.removeListener(playerListener)
        releaseInstance()
        clearFindViewByIdCache()
        super.onDestroy()
    }

}