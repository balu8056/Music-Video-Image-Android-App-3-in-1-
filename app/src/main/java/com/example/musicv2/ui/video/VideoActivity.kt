package com.example.musicv2.ui.video

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.example.musicv2.R
import com.example.musicv2.data.VideoByFolder
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.ui.image.ImageActivity
import com.example.musicv2.ui.music.MusicActivity
import com.example.musicv2.ui.music.ViewHolder
import com.example.musicv2.utils.*
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_video.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*

class AdapterForVideoRecycler(private val ctx: Context, private val array: ArrayList<VideoByFolder>): RecyclerView.Adapter<ViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.musicitem, parent,false))
    override fun getItemCount(): Int = array.size
    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.more.viewGone()
        array[position].run {
            holder.image.setImageResource(R.drawable.video_folder)
            holder.name.text = videoFolder
            holder.album.text = "${videoList?.size} videos"
            holder.itemView.setOnClickListener {
                ctx.startActivity(Intent(ctx, OpenAlbumVideo::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(videoAlbumName, videoFolder)
                })
            }
        }
    }
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.itemView.clearFindViewByIdCache()
        super.onViewDetachedFromWindow(holder)
    }
}

class VideoActivity : AppCompatActivity(), KodeinAware{
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private lateinit var videoModel: MainViewModel
    private val videos = arrayListOf<VideoByFolder>()
    private val dummy = arrayListOf<VideoByFolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        setSupportActionBar(tool)
        tool.navigationIcon = null
        videoModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        orientationCheck(videoRecycler, 2, 3)
        videoRecycler.adapter = AdapterForVideoRecycler(applicationContext, videos)

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        if (checkPermission()) ActivityCompat.requestPermissions(this, permissionsArray, storageRequestCode)
        else videoModel.getVideoByFolder(videoModel.getVideoFromContent()!!)
        setObservers()
        super.onResume()
    }

    private fun setObservers(){
        videoModel.videoByFolder.observe(this, Observer {
            videoProgress.viewVisibilityOn()
            videos.clear()
            dummy.clear()
            videos.addAll(it)
            dummy.addAll(it)
            videoRecycler.adapter?.notifyDataSetChanged()
            videoProgress.viewGone()
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.vOff(2)
        menu?.getItem(5)?.isChecked = videoModel.sharedPref.isBlackTheme()
        super.onPrepareOptionsMenu(menu)
        return true
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when(item.itemId){
            R.id.homeMenuRefresh -> { videoModel.getVideoByFolder(videoModel.getVideoFromContent()!!) }
            R.id.homeMenuImages -> {
                startActivityFromUtil(ImageActivity::class.java)
                finish()
            }
            R.id.homeMenuDarkMode -> { checkMenu(item, videoModel.sharedPref) }
            R.id.homeMusicPlayer -> {
                startActivityFromUtil(MusicActivity::class.java)
                finish()
            }
            R.id.homeClearCache -> cacheDir.deleteRecursively()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.home_menu, menu)
        (menu?.findItem(R.id.searchMusic)?.actionView as SearchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean = true
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText!!.isNotEmpty()){
                        videos.clear()
                        dummy.forEach { if (it.videoFolder?.toLowerCase(Locale.ROOT)?.contains(newText.toLowerCase(Locale.ROOT))!!) videos.add(it) }
                    }else {
                        videos.clear()
                        videos.addAll(dummy)
                    }
                    videoRecycler.adapter?.notifyDataSetChanged()
                    return true
                }
            })
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        videos.clear()
        dummy.clear()
        releaseInstance()
        clearFindViewByIdCache()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == storageRequestCode)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                videoModel.getVideoByFolder(videoModel.getVideoFromContent()!!)
            else Log.e("permission denied" , "$permissions")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}