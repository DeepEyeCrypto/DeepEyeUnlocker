package com.deepeye.otg.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.deepeye.otg.ui.OtgActivity
import com.deepeye.otg.usb.SessionCoordinator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service ensuring the USB session is not killed by Android OEM battery-savers
 * during long operations like full flashing or forensic dumps.
 */
@AndroidEntryPoint
class UsbForegroundService : Service() {

    @Inject lateinit var coordinator: SessionCoordinator

    companion object {
        const val CHANNEL_ID = "DeepEyeUsbSession"
        const val NOTIF_ID = 1337
        const val ACTION_START = "ACTION_START_USB_PERSIST"
        const val ACTION_STOP  = "ACTION_STOP_USB_PERSIST"
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startUsbPersist()
            ACTION_STOP  -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startUsbPersist() {
        // High-assurance: Keep CPU awake during potentially destructive flashes
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DeepEye:UsbSessionWakeLock").apply {
            acquire(120 * 60 * 1000L) // 2 hours max for huge dumps
        }

        createNotificationChannel()
        
        val mainIntent = Intent(this, OtgActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, UsbForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Safe generic icon
            .setContentTitle("DeepEye Unlocker")
            .setContentText("Active USB session: Maintaining connection stability.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Kill Session", stopPending)
            .build()

        startForeground(NOTIF_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "USB Persistence Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
