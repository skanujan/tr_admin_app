package com.trelegantboutique.admin.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trelegantboutique.admin.AdminApp
import com.trelegantboutique.admin.MainActivity
import com.trelegantboutique.admin.R
import com.trelegantboutique.admin.data.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives push notifications (new order placed, low stock, etc.) sent from
 * the server's own admin panel in the future. This service only shows
 * notifications and registers the device's token with the API - deciding
 * what to push, and when, stays entirely server-side, in whatever code sends
 * to Firebase using the token stored in admin_push_tokens.
 *
 * This only does anything once a Firebase project exists (google-services.json
 * is in place) - see README.md. Without it, the app still works; it simply
 * never receives a push.
 */
class AdminFirebaseMessagingService : FirebaseMessagingService() {

    private val channelId = "tr_admin_default"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? AdminApp ?: return
        if (!app.session.isSignedIn) return // registered again right after the next sign-in instead
        CoroutineScope(Dispatchers.IO).launch {
            when (app.repository.registerPush(token)) {
                is ApiResult.Success -> {}
                is ApiResult.Failure -> {} // best effort - a missed registration just means no push until next token refresh or sign-in
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "TR Elegant Admin"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "General", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
