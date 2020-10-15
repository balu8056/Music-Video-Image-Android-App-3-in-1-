package com.example.musicv2.ui.music.fragments

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.musicv2.R
import com.example.musicv2.ui.MainViewModel
import com.example.musicv2.ui.MediaChange
import com.example.musicv2.ui.ViewModelFactory
import com.example.musicv2.utils.setImageGlide
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.fragment_image.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class ImageFragment : Fragment(), KodeinAware, MediaChange {
    override val kodein: Kodein by kodein()
    private val factory : ViewModelFactory by instance()

    private lateinit var viewModel: MainViewModel

    private var mainController: MediaControllerCompat? = null

    private fun check(){
        mainController?.metadata?.let {
            requireContext()
                .setImageGlide(it.getString(MediaMetadataCompat.METADATA_KEY_ART_URI).toUri(), fit = true)
                .into(selectedMusicArt)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)
        viewModel.mediaChange = this
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedMusicArt.setOnClickListener {}

        startMusicPlayer()
    }

    private fun startMusicPlayer(){
        viewModel.controller.observe(viewLifecycleOwner, Observer {
            mainController = it
            MediaControllerCompat.setMediaController(requireActivity(), mainController)
            mainController?.registerCallback(viewModel.mediaControllerCompat)
            check()
        })
    }

    override fun onStart() {
        super.onStart()
        if (!viewModel.mediaBrowserCompat?.isConnected!!)
            viewModel.mediaBrowserCompat?.connect()
    }

    override fun onDestroy() {
        mainController?.unregisterCallback(viewModel.mediaControllerCompat)
        if (viewModel.mediaBrowserCompat?.isConnected!!) viewModel.mediaBrowserCompat?.disconnect()
        clearFindViewByIdCache()
        super.onDestroy()
    }

    override fun onMetaChanged(metadata: MediaMetadataCompat?) {
        check()
    }

    override fun onStateChanged(state: PlaybackStateCompat?) {}

}