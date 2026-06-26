package com.flypigs.ntfyapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flypigs.ntfyapp.service.NtfyService
import com.flypigs.ntfyapp.ui.navigation.BottomNavBar
import com.flypigs.ntfyapp.ui.navigation.NtfyNavGraph
import com.flypigs.ntfyapp.ui.theme.NtfyAppTheme
import com.flypigs.ntfyapp.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serverDao: com.flypigs.ntfyapp.data.local.dao.ServerDao

    @Inject
    lateinit var topicDao: com.flypigs.ntfyapp.data.local.dao.TopicDao

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
            val prefs = remember { getSharedPreferences("ntfy_prefs", MODE_PRIVATE) }
            var themeMode by remember {
                val saved = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
                mutableStateOf(try { ThemeMode.valueOf(saved) } catch (_: Exception) { ThemeMode.SYSTEM })
            }

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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val mainScreens = listOf("home", "stats", "settings")
                val showBottomBar = currentRoute in mainScreens

                val drawerHolder = LocalDrawerState.current
                val isHome = currentRoute == "home"
                val drawerState = rememberDrawerState(
                    initialValue = DrawerValue.Closed,
                    confirmStateChange = { isHome }  // 只有首页允许操作 drawer
                )
                val scope = rememberCoroutineScope()

                drawerHolder.value = drawerHolder.value.copy(
                    closeDrawer = { scope.launch { drawerState.close() } }
                )

                // 还原版本：ModalNavigationDrawer + 纯 Box drawer 容器
                // 底部栏覆盖已确认 OK
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            drawerHolder.value.content()
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavBar(navController)
                            }
                        }
                    ) { innerPadding ->
                        NtfyNavGraph(
                            navController = navController,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun startNtfyService() {
        // 不再通过 Intent 传密码，Service 通过 Hilt 注入的 Repository 从 SecureStorage 读取
        NtfyService.start(this@MainActivity)
    }

    override fun onResume() {
        super.onResume()
        startNtfyService()
    }
}
