package com.example.sahakarbharati

import android.app.Application
import com.example.sahakarbharati.data.AppContainer

class SahakarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
