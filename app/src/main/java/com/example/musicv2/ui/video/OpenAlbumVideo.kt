package com.example.musicv2.ui.video

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.example.musicv2.R
import com.example.musicv2.data.VideoData
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.ui.music.ViewHolder
import com.example.musicv2.utils.*
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_video.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*

class AdapterForVideoAlbumRecycler(private val act: Activity, private val array: ArrayList<VideoData>, private val videoModel: MainViewModel): RecyclerView.Adapter<ViewHolder>(){
    private fun startActivity(video: VideoData, position: Int){
        act.startActivity(Intent(act.applicationContext, VideoPlay::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("position", position)
            putExtra("folderName", video.folderName)
        })
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun popUp(v: View, video: VideoData, position: Int){
        PopupMenu(v.context, v, Gravity.CENTER).run {
            inflate(R.menu.image_album_menu)
            setOnMenuItemClickListener { item ->
                when(item.itemId){
                    R.id.imageAlbumOpen -> startActivity(video, position)
                    R.id.imageAlbumShare->
                        act.applicationContext.startActivity(
                            Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                type = "video/*"
                                putExtra(Intent.EXTRA_STREAM, returnVideoUri(video.videoId))
                            },"${video.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    R.id.imageAlbumDelete -> {
                        AlertDialog.Builder(act)
                            .setCancelable(false)
                            .setTitle("Delete")
                            .setMessage("The following file will be deleted permanently.\n ${video.name}")
                            .setPositiveButton("Delete") { d, _ ->
                                act.contentResolver.delete(returnVideoUri(video.videoId),null,null)
                                videoModel.getVideoFromContent(video.folderName)
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
    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        array[position].run {
            act.applicationContext.setImage(returnVideoUri(videoId), fit=false, center=true).into(holder.image)
            holder.name.text = name
            if (duration>=3600000) holder.album.text = "${getTime(duration)}  ${SimpleDateFormat("dd/MM/yyyy").format(date*1000L)}"
            else holder.album.text = "${getClockTime(duration.toInt())}  ${SimpleDateFormat("dd/MM/yyyy").format(date*1000L)}"
            holder.itemView.setOnClickListener { startActivity(this, position) }
            holder.itemView.setOnLongClickListener {
                popUp(it,this, position)
                true
            }
            holder.more.setOnClickListener { popUp(it, this, position) }
        }
    }
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.itemView.clearFindViewByIdCache()
        super.onViewDetachedFromWindow(holder)
    }
}

class OpenAlbumVideo: AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private val arrayVideoAlbum = arrayListOf<VideoData>()
    private val dummyVideoAlbum = arrayListOf<VideoData>()

    private var albumName: String? = null
    private lateinit var videoModel: MainViewModel

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        setSupportActionBar(tool)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        videoModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        videoProgress.viewVisibilityOn()
        orientationCheck(videoRecycler, 2, 3)
        videoRecycler.adapter = AdapterForVideoAlbumRecycler(this, arrayVideoAlbum, videoModel)

        intent.extras?.getString(videoAlbumName)?.let {
            tool.title = it
            albumName = it
            videoModel.getVideoFromContent(it)
        }

        videoModel.videoContent.observe(this, androidx.lifecycle.Observer {
            arrayVideoAlbum.clear()
            dummyVideoAlbum.clear()
            arrayVideoAlbum.addAll(it)
            dummyVideoAlbum.addAll(it)
            videoRecycler.adapter?.notifyDataSetChanged()
            videoProgress.viewGone()
        })

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.vOff(1);menu?.vOff(2);menu?.vOff(3);menu?.vOff(5);menu?.vOff(6)
        super.onPrepareOptionsMenu(menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> finish()
            R.id.homeMenuRefresh -> if (albumName != null) videoModel.getVideoFromContent(albumName)
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
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText!!.isNotEmpty()){
                        arrayVideoAlbum.clear()
                        dummyVideoAlbum.forEach { if (it.name?.toLowerCase(Locale.ROOT)?.contains(newText.toLowerCase(Locale.ROOT))!!) arrayVideoAlbum.add(it) }
                        videoRecycler.adapter?.notifyDataSetChanged()
                    }else {
                        arrayVideoAlbum.clear()
                        arrayVideoAlbum.addAll(dummyVideoAlbum)
                        videoRecycler.adapter?.notifyDataSetChanged()
                    }
                    return true
                }
            })
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        arrayVideoAlbum.clear()
        dummyVideoAlbum.clear()
        clearFindViewByIdCache()
        releaseInstance()
        super.onDestroy()
    }

}