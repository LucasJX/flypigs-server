package com.flypigs.ntfyapp

import android.app.Application
import android.util.Log
import com.flypigs.ntfyapp.data.local.PasswordMigration
import com.flypigs.ntfyapp.data.local.SecureStorage
import com.flypigs.ntfyapp.data.local.AppDatabase
import com.flypigs.ntfyapp.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class NtfyApplication : Application() {

    @Inject lateinit var appDatabase: AppDatabase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 初始化加密存储（必须在首次访问 SecureStorage 之前）
        SecureStorage.init(this)

        // 密码迁移：将 Room 中的明文密码迁移到 SecureStorage
        appScope.launch {
            PasswordMigration.migrateIfNeeded(this@NtfyApplication, appDatabase)
        }

        // Timber 日志初始化：Debug 植树，Release 只记录错误
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Release 只记录 WARN 及以上级别
            Timber.plant(ReleaseTree())
        }

        Timber.i("NtfyApplication initialized")
    }

    /**
     * Release 日志树 — 只记录 WARN/ERROR，不泄露 debug 信息
     */
    private class ReleaseTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            // 只记录 WARN(5)、ERROR(6) 及以上
            return priority >= Log.WARN
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Release 可接入 Crashlytics / 自定义日志后端
            // 当前仅保留 logcat 输出，但优先级 >= WARN
            if (priority >= Log.WARN) {
                Log.println(priority, tag ?: "NtfyApp", message)
                if (t != null) {
                    Log.println(priority, tag ?: "NtfyApp", t.stackTraceToString())
                }
            }
        }
    }
}
