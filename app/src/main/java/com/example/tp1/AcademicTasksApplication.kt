package com.example.tp1

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AcademicTasksApplication : Application() {
    private val adsReady = MutableStateFlow(false)
    val isAdsReady = adsReady.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        // Application is created once per process, including after Activity recreation.
        Thread {
            MobileAds.initialize(this) { adsReady.value = true }
        }.start()
    }
}
