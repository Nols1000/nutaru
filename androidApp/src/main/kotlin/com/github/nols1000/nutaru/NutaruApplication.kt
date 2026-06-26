package com.github.nols1000.nutaru

import android.app.Application
import com.github.nols1000.nutaru.db.AppContextHolder

class NutaruApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Process-wide Context so commonMain can open the encrypted database
        // without threading Context through every call. Set before any DB use.
        AppContextHolder.context = applicationContext
        System.loadLibrary("sqlcipher")
    }
}
