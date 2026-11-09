package com.clusterlink.app

import android.app.Application
import com.clusterlink.app.media.MediaHub

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        com.clusterlink.app.core.AppSettings.init(this)
        com.clusterlink.app.core.VehicleStore.init(this)
        com.clusterlink.app.core.ThemeSettings.init(this)
        // Try to attach media now; it silently no-ops until notification access is on.
        MediaHub.ensureStarted(this)
    }
}
