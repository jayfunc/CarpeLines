package io.github.jayfunc.carpelines.data.external.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.util.Log
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor

/**
 * Binds to the system notification listener so [ExternalMediaMonitor] may read the media
 * sessions of other players. Notification access is granted by the user in system settings.
 */
class ExternalMediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("ExternalMediaService", "onListenerConnected")
        ExternalMediaMonitor.attach(
            context = this,
            component = ComponentName(this, ExternalMediaListenerService::class.java),
        )
    }

    override fun onListenerDisconnected() {
        Log.d("ExternalMediaService", "onListenerDisconnected")
        ExternalMediaMonitor.detach()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        ExternalMediaMonitor.detach()
        super.onDestroy()
    }
}
