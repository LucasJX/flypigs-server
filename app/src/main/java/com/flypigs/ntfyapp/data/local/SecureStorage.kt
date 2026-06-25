package com.flypigs.ntfyapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 加密存储工具 — 用于安全保存服务器密码等敏感信息
 *
 * 使用 Android Keystore + EncryptedSharedPreferences：
 * - MasterKey 由 Keystore 自动管理，不可导出
 * - SharedPreferences 文件内容经 AES 加密
 * - 即使设备被 root 提取数据库，密码也无法解密
 */
object SecureStorage {

    private const val FILE_NAME = "ntfy_secure_prefs"
    private const val KEY_PREFIX = "server_pwd_"

    private lateinit var masterKey: MasterKey
    private lateinit var prefs: SharedPreferences

    /**
     * 初始化（在 Application.onCreate 中调用）
     */
    fun init(context: Context) {
        masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * 保存服务器密码（加密存储）
     */
    fun savePassword(serverId: String, password: String?) {
        prefs.edit()
            .putString(KEY_PREFIX + serverId, password ?: "")
            .apply()
    }

    /**
     * 读取服务器密码（解密）
     */
    fun getPassword(serverId: String): String? {
        val value = prefs.getString(KEY_PREFIX + serverId, null)
        return if (value.isNullOrBlank()) null else value
    }

    /**
     * 删除服务器密码
     */
    fun removePassword(serverId: String) {
        prefs.edit()
            .remove(KEY_PREFIX + serverId)
            .apply()
    }
}
