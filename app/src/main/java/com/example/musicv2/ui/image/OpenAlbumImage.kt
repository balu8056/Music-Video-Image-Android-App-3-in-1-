package com.example.musicv2.ui.image

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.example.musicv2.R
import com.example.musicv2.data.ImageData
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.utils.*
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_image.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ImageViewHolder(v: View): RecyclerView.ViewHolder(v) {
    val image: ImageView = v.findViewById(R.id.imageImage)
    val fab: Button = v.findViewById(R.id.imageFAB)
}

class AdapterForImageAlbumRecycler(
    private val act: Activity,
    private val array: ArrayList<ImageData>,
    private val imageViewModel: MainViewModel
): RecyclerView.Adapter<ImageViewHolder>(){
    private fun openImageActivity(position: Int){
        act.startActivity(Intent(act, OpenImage::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(imagePosition, position)
            putExtra("fol", array[position].folderName)
        })
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun popUp(v: View, imageData: ImageData, position: Int){
        PopupMenu(v.context, v, Gravity.CENTER).run {
            inflate(R.menu.image_album_menu)
            setOnMenuItemClickListener { item ->
                when(item.itemId){
                    R.id.imageAlbumOpen -> openImageActivity(position)
                    R.id.imageAlbumShare -> {
                        act.applicationContext.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, returnImageUri(imageData.imageId))
                            }, "${imageData.name}").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    R.id.imageAlbumDelete -> {
                        AlertDialog.Builder(act)
                            .setCancelable(false)
                            .setTitle("Delete")
                            .setMessage("The following file will be deleted permanently.\n ${imageData.name}")
                            .setPositiveButton("Delete") { d, _ ->
                                act.contentResolver.delete(returnImageUri(imageData.imageId),null,null)
                                imageViewModel.getImageFromContent(imageData.folderName)
                                d.dismiss()
                            }
                            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                            .show()                    }
                }
                true
            }
            show()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder =
        ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.image, parent, false))
    override fun getItemCount(): Int = array.size
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        array.elementAt(position).run {
            act.setGalleryImage(returnImageUri(imageId), fit=false, center=true).into(holder.image)
            holder.itemView.setOnClickListener { openImageActivity(position) }
            holder.fab.setOnClickListener { popUp(it,this, position) }
            holder.itemView.setOnLongClickListener {
                popUp(it,this, position)
                true
            }
        }
    }
    override fun onViewDetachedFromWindow(holder: ImageViewHolder) {
        holder.itemView.clearFindViewByIdCache()
        super.onViewDetachedFromWindow(holder)
    }
}

class OpenAlbumImage : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    private val factory: ViewModelFactory by instance()
    private val arrayAlbumList = arrayListOf<ImageData>()

    private lateinit var imageViewModel: MainViewModel

    private var imageAlbum: String? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        setSupportActionBar(tool)

        imageViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        imageProgress.viewVisibilityOn()
        orientationCheck(imageRecyclerView,3,5)
        imageRecyclerView.adapter = AdapterForImageAlbumRecycler(
            this,
            arrayAlbumList,
            imageViewModel
        )

        intent.extras?.getString(albumName)?.let {
            tool.title = it
            imageAlbum = it
            imageViewModel.getImageFromContent(it)
        }

        imageViewModel.imageContent.observe(this, Observer {
            arrayAlbumList.clear()
            arrayAlbumList.addAll(it)
            imageRecyclerView.adapter?.notifyDataSetChanged()
            imageProgress.viewGone()
        })

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> finish()
            R.id.homeMenuRefresh -> if (imageAlbum != null) imageViewModel.getImageFromContent(
                imageAlbum
            )
        }
        super.onOptionsItemSelected(item)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.vOff(0);menu?.vOff(1);menu?.vOff(2);menu?.vOff(3);menu?.vOff(5);menu?.vOff(6)
        super.onPrepareOptionsMenu(menu)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        arrayAlbumList.clear()
        clearFindViewByIdCache()
        releaseInstance()
        super.onDestroy()
    }
}