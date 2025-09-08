package com.example.taskgoapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class TaskGoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Data seeding removed for now
    }
}




