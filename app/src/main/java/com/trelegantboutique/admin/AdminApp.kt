package com.trelegantboutique.admin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.trelegantboutique.admin.data.ApiClient
import com.trelegantboutique.admin.data.ApiRepository
import com.trelegantboutique.admin.data.SessionStore

class AdminApp : Application() {
    lateinit var session: SessionStore
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var repository: ApiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionStore(this)
        apiClient = ApiClient(session)
        repository = ApiRepository(apiClient, session)

        // The server names this channel in every push, so it has to exist
        // before the first one arrives (also while the app is closed).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel("tr_admin_default", "Orders and alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
    }
}
