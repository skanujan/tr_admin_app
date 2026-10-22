package com.trelegantboutique.admin

import android.app.Application
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
    }
}
