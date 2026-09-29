package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class FileIndexingService : Service() {

    companion object {
        const val CHANNEL_ID = "channel_file_indexing"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "action_start_indexing"
        const val ACTION_STOP = "action_stop_indexing"
        const val ACTION_UPDATE_PROGRESS = "action_update_progress"

        const val EXTRA_PROCESSED = "extra_processed"
        const val EXTRA_TOTAL = "extra_total"
        const val EXTRA_PERCENT = "extra_percent"
        const val EXTRA_FILENAME = "extra_filename"

        fun startService(context: Context, fileName: String) {
            val intent = Intent(context, FileIndexingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FILENAME, fileName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, processed: Long, total: Long, percent: Float) {
            val intent = Intent(context, FileIndexingService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_PROCESSED, processed)
                putExtra(EXTRA_TOTAL, total)
                putExtra(EXTRA_PERCENT, percent)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FileIndexingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentFileName = "password.txt"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentFileName = intent.getStringExtra(EXTRA_FILENAME) ?: "password.txt"
                val notification = buildNotification(0L, 0L, 0f)
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_UPDATE_PROGRESS -> {
                val processed = intent.getLongExtra(EXTRA_PROCESSED, 0L)
                val total = intent.getLongExtra(EXTRA_TOTAL, 0L)
                val percent = intent.getFloatExtra(EXTRA_PERCENT, 0f)
                val notification = buildNotification(processed, total, percent)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(processed: Long, total: Long, percent: Float): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Password List Processing: $currentFileName"
        val content = if (total > 0) {
            "%,d / %,d (%.1f%%)".format(processed, total, percent)
        } else {
            "Processed %,d lines...".format(processed)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, percent.toInt(), total == 0L)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Password List Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during large password list indexing"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
