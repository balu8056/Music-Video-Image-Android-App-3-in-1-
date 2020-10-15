package com.example.musicv2.ui.image

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.musicv2.R
import com.example.musicv2.data.ImageData
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.utils.*
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.activity_open_image.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*

class HackyViewPager : ViewPager {
    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return try { super.onInterceptTouchEvent(ev) } catch (e: java.lang.IllegalArgumentException) { false }
    }
}

class PageImage(
    private val array: ArrayList<ImageData>,
    private val par: CoordinatorLayout,
    private val window: Window,
    private val tool: Toolbar
): PagerAdapter(){

    private var isFullscreen: Boolean = false
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun toggle() { if (isFullscreen) hide() else show() }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun hide() {
        isFullscreen = false
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        tool.viewGone()
        par.setBackgroundResource(android.R.drawable.screen_background_dark)
        par.fitsSystemWindows = false
        par.invalidate()
    }
    private fun show() {
        isFullscreen = true
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        tool.viewVisibilityOn()
        par.setBackgroundResource(android.R.drawable.screen_background_light)
        par.fitsSystemWindows = true
        par.invalidate()
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun instantiateItem(container: ViewGroup, position: Int): Any
            = PhotoView(container.context).apply{
        context.setGalleryImage(returnImageUri(array[position].imageId), fit=false, center=false).into(this)
        tool.title = array[position].name
        tool.subtitle = SimpleDateFormat("dd MMM yyyy, hh:mm a").format(array[position].date)
        isFullscreen = true

        setOnClickListener { toggle() }
        container.addView(this, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    }
    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == (`object` as PhotoView)
    override fun getCount(): Int = array.size
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as PhotoView)
    }
}

class OpenImage: AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private val imageArrayList = arrayListOf<ImageData>()

    private lateinit var imageViewModel: MainViewModel

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_image)

        setSupportActionBar(openImageTool)

        openImageTool.setNavigationOnClickListener { finish() }

        imageViewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        slideImageViewPager.pageMargin = 40
        slideImageViewPager.adapter = PageImage(imageArrayList, openImagePar, window, openImageTool)

        intent.extras?.getString("fol")?.let {
            imageViewModel.getImageFromContent(it)
        }

        imageViewModel.imageContent.observe(this, androidx.lifecycle.Observer {
            imageArrayList.addAll(it)
            slideImageViewPager.adapter?.notifyDataSetChanged()
            slideImageViewPager.currentItem = intent.extras?.getInt(imagePosition)!!
        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        super.onOptionsItemSelected(item)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        imageArrayList.clear()
        clearFindViewByIdCache()
        releaseInstance()
        super.onDestroy()
    }

}