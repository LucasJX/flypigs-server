package com.flypigs.ntfyapp.data.local

import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flypigs.ntfyapp.data.local.AppDatabase

/**
 * 密码迁移工具 — 将 Room 数据库中残留的明文 password 迁移到 SecureStorage
 *
 * 由于 Room Migration 无法访问 Context（SecureStorage 需要 Context），
 * 密码迁移必须在 AppDatabase 打开后、且 SecureStorage 已初始化后执行。
 *
 * 执行时机：NtfyApplication.onCreate() 中 SecureStorage.init() 之后
 */
object PasswordMigration {

    private const val TAG = "PasswordMigration"
    private const val KEY_MIGRATION_DONE = "password_migration_v5_done"

    /**
     * 执行密码迁移（仅在首次升级到 v5 时执行一次）
     *
     * @param context 用于 SecureStorage 和 SharedPreferences
     * @param database AppDatabase 实例（通过 Hilt 注入获取）
     */
    suspend fun migrateIfNeeded(context: Context, database: AppDatabase) {
        val prefs = context.getSharedPreferences("ntfy_migration_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATION_DONE, false)) {
            Log.d(TAG, "Password migration already done, skipping")
            return
        }

        Log.i(TAG, "Starting password migration from Room to SecureStorage...")
        try {
            // 通过 Room 的 openHelper 获取可读数据库
            val db = database.openHelper.readableDatabase

            // 查询所有有密码的 server
            val cursor: Cursor = db.query(
                "SELECT id, password FROM servers WHERE password IS NOT NULL AND password != ''"
            )

            cursor.use {
                var migratedCount = 0
                while (it.moveToNext()) {
                    val serverId = it.getString(it.getColumnIndexOrThrow("id"))
                    val password = it.getString(it.getColumnIndexOrThrow("password"))

                    if (password.isNotBlank()) {
                        SecureStorage.savePassword(serverId, password)
                        migratedCount++
                        Log.d(TAG, "Migrated password for server $serverId")
                    }
                }
                Log.i(TAG, "Password migration complete: $migratedCount servers migrated")
            }

            // 标记迁移完成
            prefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Password migration failed", e)
            // 不标记完成，下次启动会重试
        }
    }
}
