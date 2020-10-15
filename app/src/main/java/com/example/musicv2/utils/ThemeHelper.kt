package com.example.musicv2.utils

import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    fun applyTheme(theme: String){
        when(theme){
            lightMode -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            darkMode -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}