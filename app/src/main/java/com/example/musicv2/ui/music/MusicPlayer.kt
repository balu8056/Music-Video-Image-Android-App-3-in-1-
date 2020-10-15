package com.example.musicv2.ui.music

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.WindowManager
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.musicv2.R
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.MediaChange
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.ui.music.fragments.ImageFragment
import com.example.musicv2.ui.music.fragments.PlayList
import com.example.musicv2.utils.getClockTime
import com.example.musicv2.utils.setImageGlide
import com.example.musicv2.utils.viewVisibilityOn
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_music.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.system.exitProcess

class AdapterForViewPager(fragment: FragmentManager): FragmentPagerAdapter(fragment, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getItem(position: Int): Fragment {
        return when(position){
            0 -> ImageFragment()
            1 -> PlayList()
            else -> ImageFragment()
        }
    }
    override fun getCount(): Int = 2
}

class MusicPlayer: AppCompatActivity(), KodeinAware, MediaChange{
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private lateinit var musicViewModel: MainViewModel

    private var mainController: MediaControllerCompat? = null

    private var isUnderDuration: Int = -1

    private val target = object : CustomTarget<Drawable>(){
        override fun onLoadCleared(placeholder: Drawable?) {}
        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            window.setBackgroundDrawable(resource)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        window.statusBarColor = getColor(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        musicViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        musicViewModel.mediaChange = this

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        startPlayer()

    }

    private fun startPlayer(){
        musicViewModel.controller.observe(this, Observer {
            mainController = it
            MediaControllerCompat.setMediaController(this@MusicPlayer, mainController)
            mainController?.registerCallback(musicViewModel.mediaControllerCompat)
            init()
        })
    }

    private fun check(){
        mainController?.metadata?.let {
            it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt().let { duration->
                selectedMusicSeekBar.max = duration
                isUnderDuration = duration
                selectedMusicDuration.text = getClockTime(duration)
            }
            toolbar.title = it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            toolbar.subtitle = it.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            setBlurImage(it.getString(MediaMetadataCompat.METADATA_KEY_ART_URI).toUri())
        }
    }

    private fun isPlaying(){
        when(mainController?.playbackState?.state){
            PlaybackStateCompat.STATE_PLAYING -> {
                if (!barEqu.isAnimating) {
                    if (!barEqu.isVisible) barEqu.viewVisibilityOn()
                    barEqu.animateBars()
                }
                selectedMusicPlay.setBackgroundResource(R.drawable.pause_btn)
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                if (barEqu.isAnimating) barEqu.stopBars()
                selectedMusicPlay.setBackgroundResource(R.drawable.play_btn)
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                finishAffinity()
                exitProcess(0)
            }
        }
    }

    private fun init(){
        isPlaying()
        check()
        selectedMusicNext.setOnClickListener { mainController?.transportControls?.skipToNext() }
        selectedMusicPlay.setOnClickListener {
            when(mainController?.playbackState?.state!!){
                PlaybackStateCompat.STATE_PAUSED -> {
                    mainController?.transportControls?.play()
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    mainController?.transportControls?.pause()
                }
            }
        }
        selectedMusicPrev.setOnClickListener { mainController?.transportControls?.skipToPrevious() }

        selectedMusicSeekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) selectedMusicCurrentPos.text = getClockTime(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                mainController?.transportControls?.seekTo(seekBar?.progress!!.toLong())
            }
        })
        viewPager.adapter = AdapterForViewPager(supportFragmentManager)
        viewPagerTab.setupWithViewPager(viewPager, true)

        selectedMusicSeekBar.progress = try { mainController?.playbackState?.position?.toInt()!! }catch (e: Exception){0}
        selectedMusicCurrentPos.text = try { getClockTime(mainController?.playbackState?.position?.toInt()!!) }catch (e: Exception){getClockTime(0)}

    }

    private fun setBlurImage(uri: Uri?){
        setImageGlide(uri,false).transform(BlurTransformation(25,3)).into(target)
    }

    override fun onStart() {
        super.onStart()
        if (!musicViewModel.mediaBrowserCompat?.isConnected!!)
            musicViewModel.mediaBrowserCompat?.connect()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        if (barEqu.isAnimating) barEqu.stopBars()
        mainController?.unregisterCallback(musicViewModel.mediaControllerCompat)
        if (musicViewModel.mediaBrowserCompat?.isConnected!!) musicViewModel.mediaBrowserCompat?.disconnect()
        releaseInstance()
        clearFindViewByIdCache()
        super.onDestroy()
    }

    override fun onMetaChanged(metadata: MediaMetadataCompat?) { check() }

    override fun onStateChanged(state: PlaybackStateCompat?) {
        isPlaying()
        state?.position?.toInt()?.let {
            if (isUnderDuration!=-1 && it <= isUnderDuration){
                selectedMusicSeekBar.progress = it
                selectedMusicCurrentPos.text = getClockTime(it)
            }
        }
    }
}