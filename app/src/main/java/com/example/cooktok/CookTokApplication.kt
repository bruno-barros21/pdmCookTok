package com.example.cooktok

import android.app.Application
import com.example.data.notifications.CookTokNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CookTokApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Regista os canais de notificação uma única vez no arranque (Week 9)
        CookTokNotificationHelper.createNotificationChannels(this)
    }
}