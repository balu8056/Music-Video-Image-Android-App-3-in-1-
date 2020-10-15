package com.example.musicv2.ui.image

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
import com.example.musicv2.data.ImageByFolder
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.ui.music.MusicActivity
import com.example.musicv2.ui.music.ViewHolder
import com.example.musicv2.ui.video.VideoActivity
import com.example.musicv2.utils.*
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_image.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.collections.ArrayList

class AdapterForImageRecycler(private val ctx: Context, private val array: ArrayList<ImageByFolder>): RecyclerView.Adapter<ViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.musicitem, parent, false))
    override fun getItemCount(): Int = array.size
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.more.viewGone()
        array.elementAt(position).run {
            holder.name.text = imageFolder
            holder.album.text = "${imageList?.size} pictures"
            ctx.setImageGlide(returnImageUri(imageList?.elementAt(0)!!.imageId), fit=false, center=true).into(holder.image)
            holder.itemView.setOnClickListener {
                ctx.startActivity(Intent(ctx, OpenAlbumImage::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(albumName, imageFolder)
                })
            }
        }
    }
    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.itemView.clearFindViewByIdCache()
        super.onViewDetachedFromWindow(holder)
    }
}

class ImageActivity: AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val factory: ViewModelFactory by instance()

    private lateinit var imageViewModel: MainViewModel
    private val imageFromViewModel = ArrayList<ImageByFolder>()
    private val dummyImageViewModel = ArrayList<ImageByFolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        setSupportActionBar(tool)
        tool.navigationIcon = null
        imageViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        orientationCheck(imageRecyclerView, 2, 3)
        imageRecyclerView.adapter = AdapterForImageRecycler(applicationContext, imageFromViewModel)

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        if (checkPermission()) ActivityCompat.requestPermissions(this, permissionsArray, storageRequestCode)
        else imageViewModel.getImageByFolder(imageViewModel.getImageFromContent()!!)
        setImageObserver()
        super.onResume()
    }

    private fun setImageObserver(){
        imageViewModel.imageByFolder.observe(this, Observer {
            Log.e("imageByFolder", it.toString())
            imageProgress.viewVisibilityOn()
            imageFromViewModel.clear()
            dummyImageViewModel.clear()
            imageFromViewModel.addAll(it)
            dummyImageViewModel.addAll(it)
            imageRecyclerView.adapter?.notifyDataSetChanged()
            imageProgress.viewGone()
        })
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.vOff(3)
        menu?.getItem(5)?.isChecked = imageViewModel.sharedPref.isBlackTheme()
        super.onPrepareOptionsMenu(menu)
        return true
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when(item.itemId){
            R.id.homeMusicPlayer -> {
                startActivityFromUtil(MusicActivity::class.java)
                finish()
            }
            R.id.homeMenuRefresh -> imageViewModel.getImageByFolder(imageViewModel.getImageFromContent()!!)
            R.id.homeMenuDarkMode -> checkMenu(item, imageViewModel.sharedPref)
            R.id.homeVideoPlayer -> {
                startActivityFromUtil(VideoActivity::class.java)
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
                        imageFromViewModel.clear()
                        dummyImageViewModel.forEach {
                            if (it.imageFolder?.toLowerCase(Locale.ROOT)?.contains(newText.toLowerCase(Locale.ROOT))!!)
                                imageFromViewModel.add(it)
                        }
                    }else {
                        imageFromViewModel.clear()
                        imageFromViewModel.addAll(dummyImageViewModel)
                    }
                    imageRecyclerView.adapter?.notifyDataSetChanged()
                    return true
                }
            })
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        imageFromViewModel.clear()
        dummyImageViewModel.clear()
        clearFindViewByIdCache()
        releaseInstance()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == storageRequestCode)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                imageViewModel.getImageByFolder(imageViewModel.getImageFromContent()!!)
            else Log.e("permission denied" , "$permissions")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}