package com.example.mycalllogsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class CallLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, CallLogService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(serviceIntent)
        } else {
            context?.startService(serviceIntent)
        }
    }
}
