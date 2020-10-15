package com.example.musicv2.ui.music

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.example.musicv2.MusicService
import com.example.musicv2.R
import com.example.musicv2.data.MusicData
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.MediaChange
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.ui.image.ImageActivity
import com.example.musicv2.ui.video.VideoActivity
import com.example.musicv2.utils.*
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_main.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.system.exitProcess

class ViewHolder(v: View): RecyclerView.ViewHolder(v){
    val image: ImageView = v.findViewById(R.id.musicImage)
    val name: TextView = v.findViewById(R.id.musicName)
    val album: TextView = v.findViewById(R.id.musicAlbum)
    val more: ImageButton = v.findViewById(R.id.musicMore)
}

class AdapterForRecycler(private val act: Activity, private val array: MutableList<MusicData>, private val mainController: MediaControllerCompat?, private val musicViewModel: MainViewModel): RecyclerView.Adapter<ViewHolder>(){
    private val openMusic = { music: MusicData ->
        mainController?.transportControls?.playFromUri(returnMusicUri(music.musicId), Bundle().apply {
            putBoolean(isplayable, true)
            putParcelable(musicForService, music)
        })
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openMenu(v: View, music: MusicData ){
        PopupMenu(v.context, v).run {
            inflate(R.menu.long_press)
            setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.menuPlay -> openMusic(music)
                    R.id.menuPlayNext -> {
                        val ba = Bundle()
                        ba.putParcelable(musicForService, music)
                        mainController?.transportControls?.sendCustomAction(add_Queue, ba)
                    }
                    R.id.menuShare ->
                        act.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, returnMusicUri(music.musicId))
                            }, "${music.musicName}"
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    R.id.menuDelete -> {
                        AlertDialog.Builder(act)
                            .setCancelable(false)
                            .setTitle("Delete")
                            .setMessage("The following file will be deleted permanently.\n ${music.musicName}")
                            .setPositiveButton("Delete") { d, _ ->
                                act.contentResolver.delete(returnMusicUri(music.musicId),null,null)
                                musicViewModel.getMusicFromContent()
                                d.dismiss()
                            }
                            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                            .show()
                    }
                }
                true
            }
            show()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.musicitem, parent, false))
    override fun getItemCount(): Int = array.size
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        array[position].run {
            act.setImageGlide(musicArtUri).into(holder.image)
            holder.name.text = musicName
            holder.album.text = musicAlbum
            holder.itemView.setOnClickListener { openMusic(this) }
            holder.more.setOnClickListener { openMenu(it,this) }
            holder.itemView.setOnLongClickListener {
                openMenu(it,this)
                true
            }
        }
    }
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.itemView.clearFindViewByIdCache()
        super.onViewDetachedFromWindow(holder)
    }
}

class MusicActivity : AppCompatActivity(), KodeinAware, MediaChange {
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private var mainController: MediaControllerCompat? = null

    private lateinit var viewModel: MainViewModel
    private val musicFromViewModel = mutableListOf<MusicData>()
    private var dummyMusicData = mutableListOf<MusicData>()

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(tool)

        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        viewModel.mediaChange = this

        startMusicPlayer()

        if (checkPermission()) ActivityCompat.requestPermissions(this, permissionsArray, storageRequestCode)
        else viewModel.getMusicFromContent()

        fragmentSeekBar.setOnTouchListener { _, _ -> true }

    }


    private fun startMusicPlayer(){
        viewModel.controller.observe(this, Observer {
            mainController = it
            MediaControllerCompat.setMediaController(this, mainController)
            mainController?.registerCallback(viewModel.mediaControllerCompat)
            if (mainController?.playbackState?.state == PlaybackStateCompat.STATE_NONE) showFragHistory()
            observe()
        })
    }

    private fun checkFile(){
        mainController?.metadata?.let {
            if (!frameHolder.isVisible) {
                frameHolder.viewVisibilityOn()
                summaRel.setPadding(0, 0, 0, (60*resources.displayMetrics.density).toInt())
            }
            fragmentSeekBar.progress = 0
            fragmentSeekBar.max = it.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
            fragmentSongAlbum.text = it.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            fragmentSongName.text = it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            setImageGlide(it.getString(MediaMetadataCompat.METADATA_KEY_ART_URI).toUri()).into(fragmentSongArt)
        }
    }

    private fun showFragHistory(){
        if (viewModel.sharedPref.returnMusic() == null){
            frameHolder.viewGone()
            summaRel.setPadding(0, 0, 0, 0)
        }else
            viewModel.sharedPref.returnMusic()?.let {
                fragmentSongName.text = it.musicName
                fragmentSongAlbum.text = it.musicAlbum
                setImageGlide(it.musicArtUri).into(fragmentSongArt)
                Bundle().apply {
                    putBoolean(isplayable, false)
                    putParcelable(musicForService, it)
                    mainController?.transportControls?.playFromUri(returnMusicUri(it.musicId), this)
                }
                frameHolder.viewVisibilityOn()
            }
    }

    private fun isPlaying(){
        when(mainController?.playbackState?.state){
            PlaybackStateCompat.STATE_PLAYING -> fragmentPlayPause.setBackgroundResource(R.drawable.pause_btn)
            PlaybackStateCompat.STATE_PAUSED -> fragmentPlayPause.setBackgroundResource(R.drawable.play_btn)
            PlaybackStateCompat.STATE_STOPPED -> {
                finishAffinity()
                exitProcess(0)
            }
        }
    }

    private fun observe(){
        isPlaying()
        checkFile()
        orientationCheck(musicRecycler,2,3)
        musicRecycler.adapter = AdapterForRecycler(
            this,
            musicFromViewModel,
            mainController,
            viewModel
        )
        setObservers()
        fragmentPlayPause.setOnClickListener {
            when(mainController?.playbackState?.state!!){
                PlaybackStateCompat.STATE_PAUSED -> mainController?.transportControls?.play()
                PlaybackStateCompat.STATE_PLAYING -> mainController?.transportControls?.pause()
            }
        }
        frame.setOnClickListener{ startActivityFromUtil(MusicPlayer::class.java) }
    }

    private fun setObservers(){
        viewModel.musicFromContent.observe(this, Observer {
            homeProgress.viewVisibilityOn()
            musicFromViewModel.clear()
            dummyMusicData.clear()
            musicFromViewModel.addAll(it)
            dummyMusicData.addAll(it)
            musicRecycler.adapter!!.notifyDataSetChanged()
            homeProgress.viewGone()
        })
    }

    override fun onStart() {
        if (!viewModel.getRunService())
            startService(Intent(applicationContext, MusicService::class.java))
        if (!viewModel.mediaBrowserCompat?.isConnected!!)
            viewModel.mediaBrowserCompat?.connect()
        super.onStart()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        viewModel.sharedPref.isBlackTheme().let {
            setFragmentColor(it)
            settingTheme(it)
        }
        super.onResume()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setFragmentColor(it: Boolean){
        if (it) {
            frameHolder.setBackgroundColor(getColor(R.color.frameBackgroundDark))
            fragmentSongName.setTextColor(getColor(android.R.color.white))
            fragmentSongAlbum.setTextColor(getColor(android.R.color.white))
        }else{
            frameHolder.setBackgroundColor(getColor(android.R.color.white))
            fragmentSongName.setTextColor(getColor(android.R.color.black))
            fragmentSongAlbum.setTextColor(getColor(android.R.color.black))
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.vOff(1)
        menu?.getItem(5)?.isChecked = viewModel.sharedPref.isBlackTheme()
        super.onPrepareOptionsMenu(menu)
        return true
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.homeMenuRefresh -> viewModel.getMusicFromContent()
            R.id.homeMenuImages -> {
                startActivityFromUtil(ImageActivity::class.java)
                finish()
            }
            R.id.homeMenuDarkMode -> {
                setFragmentColor(checkMenu(item, viewModel.sharedPref))
            }
            R.id.homeVideoPlayer -> {
                startActivityFromUtil(VideoActivity::class.java)
                finish()
            }
            R.id.homeClearCache -> cacheDir.deleteRecursively()
        }
        super.onOptionsItemSelected(item)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.home_menu, menu)
        (menu?.findItem(R.id.searchMusic)?.actionView as SearchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText!!.isNotEmpty()){
                        musicFromViewModel.clear()
                        dummyMusicData.forEach {
                            if (it.musicName?.toLowerCase(Locale.ROOT)?.contains(newText.toLowerCase(Locale.ROOT))!! || it.musicAlbum?.toLowerCase(Locale.ROOT)?.contains(newText.toLowerCase(Locale.ROOT))!!)
                                musicFromViewModel.add(it)
                        }
                    }else {
                        musicFromViewModel.clear()
                        musicFromViewModel.addAll(dummyMusicData)
                    }
                    musicRecycler.adapter?.notifyDataSetChanged()
                    return true
                }
            })
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        mainController?.unregisterCallback(viewModel.mediaControllerCompat)
        if (viewModel.mediaBrowserCompat?.isConnected!!) viewModel.mediaBrowserCompat?.disconnect()
        musicFromViewModel.clear()
        dummyMusicData.clear()
        releaseInstance()
        clearFindViewByIdCache()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == storageRequestCode)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                viewModel.getMusicFromContent()
            else Log.e("permission denied" , "$permissions")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMetaChanged(metadata: MediaMetadataCompat?) {
        checkFile()
    }

    override fun onStateChanged(state: PlaybackStateCompat?) {
        isPlaying()
        state?.position?.let { fragmentSeekBar.progress = it.toInt() }
    }

}