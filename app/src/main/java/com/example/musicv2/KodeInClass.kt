package com.example.musicv2

import android.app.Application
import com.example.musicv2.ui.SharedPreferenceForMusic
import com.example.musicv2.ui.ViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

class KodeInClass: Application(), KodeinAware {

    override val kodein: Kodein = Kodein.lazy {
        import(androidXModule(this@KodeInClass))

        bind() from singleton { ViewModelFactory(applicationContext) }
        bind() from singleton { SharedPreferenceForMusic(applicationContext) }
    }
}