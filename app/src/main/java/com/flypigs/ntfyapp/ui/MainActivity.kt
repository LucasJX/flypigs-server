package com.flypigs.ntfyapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.flypigs.ntfyapp.service.NtfyService
import com.flypigs.ntfyapp.ui.navigation.BottomNavBar
import com.flypigs.ntfyapp.ui.navigation.NtfyNavGraph
import com.flypigs.ntfyapp.ui.theme.NtfyAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startNtfyService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startNtfyService()
            }
        } else {
            startNtfyService()
        }

        setContent {
            NtfyAppTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NtfyNavGraph(navController)
                    }
                }
            }
        }
    }

    private fun startNtfyService() {
        val prefs = getSharedPreferences("ntfy_prefs", MODE_PRIVATE)
        val serverUrl = prefs.getString(NtfyService.PREF_SERVER_URL, NtfyService.DEFAULT_SERVER)
            ?: NtfyService.DEFAULT_SERVER
        val topic = prefs.getString(NtfyService.PREF_TOPIC, NtfyService.DEFAULT_TOPIC)
            ?: NtfyService.DEFAULT_TOPIC
        val token = prefs.getString(NtfyService.PREF_TOKEN, null)

        NtfyService.start(this, serverUrl, topic, token)
    }
}
