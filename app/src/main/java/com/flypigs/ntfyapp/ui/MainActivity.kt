package com.flypigs.ntfyapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.flypigs.ntfyapp.data.local.dao.ServerDao
import com.flypigs.ntfyapp.data.local.dao.TopicDao
import com.flypigs.ntfyapp.service.NtfyService
import com.flypigs.ntfyapp.ui.navigation.BottomNavBar
import com.flypigs.ntfyapp.ui.navigation.NtfyNavGraph
import com.flypigs.ntfyapp.ui.theme.NtfyAppTheme
import com.flypigs.ntfyapp.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serverDao: ServerDao

    @Inject
    lateinit var topicDao: TopicDao

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startNtfyService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 请求通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startNtfyService()
            }
        } else {
            startNtfyService()
        }

        setContent {
            val prefs = remember { getSharedPreferences("ntfy_prefs", MODE_PRIVATE) }
            // 同步读取初始值，避免闪烁
            var themeMode by remember {
                val saved = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
                mutableStateOf(try { ThemeMode.valueOf(saved) } catch (_: Exception) { ThemeMode.SYSTEM })
            }

            // DisposableEffect 保持 listener 强引用，防 GC
            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "theme_mode") {
                        val v = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
                        themeMode = try { ThemeMode.valueOf(v) } catch (_: Exception) { ThemeMode.SYSTEM }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            NtfyAppTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        NtfyNavGraph(navController)
                    }
                }
            }
        }
    }


    private fun startNtfyService() {
        lifecycleScope.launch {
            val server = serverDao.getAllServers().first().firstOrNull()
            val topic = topicDao.getAllTopics().first().firstOrNull()
            if (server != null && topic != null) {
                val prefs = getSharedPreferences("ntfy_prefs", MODE_PRIVATE)
                prefs.edit().apply {
                    putString(NtfyService.PREF_SERVER_URL, server.url)
                    putString(NtfyService.PREF_TOPIC, topic.name)
                    putString(NtfyService.PREF_USERNAME, server.username)
                    putString(NtfyService.PREF_PASSWORD, server.password)
                    apply()
                }
                NtfyService.start(
                    this@MainActivity,
                    server.url,
                    topic.name,
                    server.username,
                    server.password
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startNtfyService()
    }
}
