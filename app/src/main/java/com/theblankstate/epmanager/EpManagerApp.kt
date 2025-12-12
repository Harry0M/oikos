package com.theblankstate.epmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EpManagerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-wide services here
    }
}
