package com.tether.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TetherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
