package br.com.taskgo.taskgo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TaskGoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Data seeding removed for now
    }
}




