package com.cyb.payten_windowsxp_terminalapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppCore: Application() {
    override fun onCreate() {
        super.onCreate()
    }
}