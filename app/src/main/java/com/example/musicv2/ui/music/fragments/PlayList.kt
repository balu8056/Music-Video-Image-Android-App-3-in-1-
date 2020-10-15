package com.example.musicv2.ui.music.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicv2.R
import com.example.musicv2.data.MusicData
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.utils.musicForService
import com.example.musicv2.utils.setImageGlide
import com.example.musicv2.utils.update_Queue
import com.example.musicv2.utils.updatedQ
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.fragment_play_list.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance
import java.util.*

class ViewHolderForPlayList(v: View): RecyclerView.ViewHolder(v){
    val image: ImageView = v.findViewById(R.id.playListArt)
    val name: TextView = v.findViewById(R.id.playListSongName)
    val album: TextView = v.findViewById(R.id.playListSongAlbum)
}

class AdapterForPlayList(private val ctx: Context, private val array: MutableList<MusicData>, private val mainController: MediaControllerCompat?, private val drag: ItemTouchHelper): RecyclerView.Adapter<ViewHolderForPlayList>(){
    fun updateQ(updatedQueue: MutableList<MusicData>){
        mainController?.transportControls?.sendCustomAction(update_Queue,
            Bundle().apply { putParcelableArrayList(updatedQ, updatedQueue as ArrayList<MusicData>) })
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderForPlayList =
        ViewHolderForPlayList(LayoutInflater.from(parent.context).inflate(R.layout.play_list, parent, false))
    override fun getItemCount(): Int = array.size
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolderForPlayList, position: Int) {
        array.elementAt(position).run {
            ctx.setImageGlide(musicArtUri).into(holder.image)
            holder.image.setImageURI(musicArtUri)
            holder.name.text = musicName
            holder.album.text = musicAlbum
            holder.image.setOnTouchListener { _, _ ->
                drag.startDrag(holder)
                false
            }
            holder.itemView.setOnClickListener { mainController?.transportControls?.skipToQueueItem(musicId) }
        }
    }
    override fun onViewDetachedFromWindow(holder: ViewHolderForPlayList) {
        holder.itemView.clearFindViewByIdCache()
        super.onViewDetachedFromWindow(holder)
    }
}

class PlayList : Fragment(), KodeinAware{
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private lateinit var viewModel: MainViewModel
    private val playList= mutableListOf<MusicData>()

    private var mainController: MediaControllerCompat? = null

    private val drag = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(p0: RecyclerView, p1: RecyclerView.ViewHolder, p2: RecyclerView.ViewHolder): Boolean {
            val from = p1.adapterPosition
            val to = p2.adapterPosition
            Collections.swap(playList, from, to)
            (playListRecycler.adapter as AdapterForPlayList).notifyItemMoved(from, to)
            (playListRecycler.adapter as AdapterForPlayList).updateQ(playList)
            return true
        }
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    }

    private fun chkList(){
        playList.clear()
        mainController?.queue?.forEach {
            it.description.extras?.getParcelable<MusicData>(musicForService)?.let { music-> playList.add(music) }
        }
        if (playList.size == mainController?.queue?.size)
            playListRecycler.adapter?.notifyDataSetChanged()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        return inflater.inflate(R.layout.fragment_play_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startMusicPlayer()
    }

    private fun startMusicPlayer(){

        viewModel.controller.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            mainController = it
            MediaControllerCompat.setMediaController(requireActivity(), mainController)

            playListRecycler.layoutManager = LinearLayoutManager(requireContext())
            val dragHelper = ItemTouchHelper(drag)
            dragHelper.attachToRecyclerView(playListRecycler)
            playListRecycler.adapter = AdapterForPlayList(requireContext(), playList, mainController, dragHelper)

            chkList()
        })
    }

    override fun onStart() {
        super.onStart()
        if (!viewModel.mediaBrowserCompat?.isConnected!!) viewModel.mediaBrowserCompat?.connect()
    }

    override fun onDestroy() {
        if (viewModel.mediaBrowserCompat?.isConnected!!) viewModel.mediaBrowserCompat?.disconnect()
        playList.clear()
        clearFindViewByIdCache()
        super.onDestroy()
    }

}