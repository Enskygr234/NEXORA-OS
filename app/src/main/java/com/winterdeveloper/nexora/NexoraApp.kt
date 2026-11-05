package com.winterdeveloper.nexora

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import com.winterdeveloper.nexora.core.AppContainer

class NexoraApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            container.iconLoader.clear()
        }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as NexoraApp).container
