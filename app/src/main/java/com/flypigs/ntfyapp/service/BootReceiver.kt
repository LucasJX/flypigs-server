package com.flypigs.ntfyapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting ntfy service")

            val prefs = context.getSharedPreferences("ntfy_prefs", Context.MODE_PRIVATE)
            val serverUrl = prefs.getString(NtfyService.PREF_SERVER_URL, NtfyService.DEFAULT_SERVER)
                ?: NtfyService.DEFAULT_SERVER
            val topic = prefs.getString(NtfyService.PREF_TOPIC, NtfyService.DEFAULT_TOPIC)
                ?: NtfyService.DEFAULT_TOPIC
            val username = prefs.getString(NtfyService.PREF_USERNAME, null)
            val password = prefs.getString(NtfyService.PREF_PASSWORD, null)

            NtfyService.start(context, serverUrl, topic, username, password)
        }
    }
}
