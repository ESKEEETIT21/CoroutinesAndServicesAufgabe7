package com.example.jetpackcompose.service

import android.app.*
import android.content.*
import android.os.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import androidx.core.content.ContextCompat
import com.example.jetpackcompose.MainActivity
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.jetpackcompose.ui.views.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PopupService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var delayMillis: Long = -1L
    private var i = 1
    private val dataStore by lazy { applicationContext.dataStore }
    private var isNotificationEnabled: Boolean = false

    /**
     * A broadcast receiver that listens for updates to the timer option.
     * When a new timer option is received, it updates the service's timer configuration.
     */
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newTimerOption = intent?.getStringExtra("timer_option") ?: "Deactivated"
            updateTimerOption(newTimerOption)
        }
    }

    /**
     * Called when the service is created. Sets up the notification channel, starts the service in the foreground,
     * registers the update receiver, and initializes the timer from the settings.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        //TODO starte den Service hier
        val notification = getNotification("Start popup service")
        startForeground(i, notification)
        registerUpdateReceiver()
        initializeTimerFromSettings()
    }

    /**
     * Called when the service is destroyed. Removes any pending callbacks and unregisters the update receiver.
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(showNotificationRunnable)
        unregisterReceiver(updateReceiver)
    }

    /**
     * Called when the service is started. If a valid delay is set, it restarts the notification handler with the new delay.
     *
     * @param intent The intent that started the service.
     * @param flags Additional flags provided with the start request.
     * @param startId The unique ID for this start request.
     * @return The return value determines how the system should handle the service if it is killed.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (delayMillis != -1L) {
            handler.removeCallbacks(showNotificationRunnable)
            handler.post(showNotificationRunnable)
        }
        return START_STICKY
    }

    /**
     * Called when the service is bound. This service does not support binding, so it returns null.
     *
     * @param intent The intent used to bind to the service.
     * @return Always returns null since this service is not bound.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * A runnable that periodically sends notifications if notifications are enabled.
     * The notification content increments with each execution.
     */
    private val showNotificationRunnable = object : Runnable {
        override fun run() {
            if (isNotificationEnabled) {
                sendNotification("Hello World $i")
                i++
            }
            handler.postDelayed(this, delayMillis)
        }
    }

    /**
     * Updates the timer option based on the provided string, adjusts the notification frequency,
     * and restarts or stops the service accordingly.
     *
     * @param option The new timer option to set.
     */
    private fun updateTimerOption(option: String) {
        delayMillis = timerOptionToMillis(option)
        isNotificationEnabled = delayMillis != -1L
        handler.removeCallbacks(showNotificationRunnable)

        if (delayMillis == -1L) {
            stopSelf()
        } else {
            handler.postDelayed(showNotificationRunnable, delayMillis)
        }
    }

    /**
     * Fetches the timer option from the app's settings using DataStore.
     *
     * @return The stored timer option value, or "Deactivated" if not set.
     */
    private suspend fun fetchTimerOptionFromSettings(): String {
        val key = stringPreferencesKey("timer_option_key")
        val timerOption = dataStore.data.map { preferences ->
            preferences[key] ?: "Deactivated"
        }.first()

        return timerOption
    }

    /**
     * Registers the update receiver to listen for timer updates via broadcast intents.
     */
    private fun registerUpdateReceiver() {
        ContextCompat.registerReceiver(
            this,
            updateReceiver,
            IntentFilter("com.example.jetpackcompose.UPDATE_TIMER"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Converts a timer option string to its corresponding delay in milliseconds.
     *
     * @param option The timer option as a string (e.g., "10s", "30 min").
     * @return The corresponding delay in milliseconds, or -1L if the option is invalid.
     */
    private fun timerOptionToMillis(option: String): Long {
        return when (option) {
            "10s" -> 10_000L
            "30s" -> 30_000L
            "60s" -> 60_000L
            "30 min" -> 30 * 60 * 1000L
            "60 min" -> 60 * 60 * 1000L
            else -> -1L
        }
    }

    /**
     * Initializes the timer based on the stored settings, fetching the timer option from DataStore
     * and setting the notification frequency accordingly.
     */
    private fun initializeTimerFromSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val timerOption = fetchTimerOptionFromSettings()
            delayMillis = timerOptionToMillis(timerOption)

            if (delayMillis != -1L) {
                isNotificationEnabled = true
                handler.post(showNotificationRunnable)
            }
        }
    }


    /**
     * Sends a notification with the specified message if the necessary permissions are granted.
     *
     * @param message The message to be displayed in the notification.
     */
    private fun sendNotification(message: String) {
        if (ActivityCompat.checkSelfPermission(
                this@PopupService,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)
        val notification = getNotification(message)
        notificationManager.notify(1, notification)
    }

    /**
     * Creates a notification with the specified content text and returns it.
     * The notification includes a pending intent that opens the MainActivity when clicked.
     *
     * @param contentText The text to be displayed in the notification.
     * @return The created Notification.
     */
    private fun getNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "popup_service_channel")
            .setContentTitle("Popup Service")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    /**
     * Creates a notification channel for the Popup Service if the Android version is Oreo or higher.
     * The channel is configured with high importance, lights, vibration, and visibility for the lockscreen.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "popup_service_channel",
                "Popup Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Popup Service"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
