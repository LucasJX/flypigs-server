package com.flypigs.ntfyapp.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 分类信息 — 动态分类系统
 * 替代原来的 MessageCategory enum，支持插件端动态定义分类
 */
data class CategoryInfo(
    val name: String,
    val displayName: String,
    val icon: ImageVector,
    val fallbackColor: Color  // 非 Composable 上下文的 fallback
) {
    /**
     * Composable 上下文中使用 MaterialTheme 角色，自适应暗色/亮色主题
     * 非 Composable 上下文（如 Room DAO、Service）使用 fallbackColor
     */
    val color: Color
        @Composable get() = when (name) {
            "节点监控" -> MaterialTheme.colorScheme.primary
            "系统监控" -> MaterialTheme.colorScheme.error
            "设备监控" -> MaterialTheme.colorScheme.primaryContainer
            "订阅提醒" -> MaterialTheme.colorScheme.tertiary
            "配置变更" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        }
}

/**
 * 分类注册表 — 管理所有已知分类
 * 插件端通过 ec:xxx 标签定义分类，App 端自动识别
 */
object CategoryRegistry {
    // 默认分类映射
    private val defaultCategories = mapOf(
        "节点监控" to CategoryInfo("节点监控", "节点监控", Icons.Default.SwapHoriz, Color(0xFF1976D2)),
        "系统监控" to CategoryInfo("系统监控", "系统监控", Icons.Default.Warning, Color(0xFFD32F2F)),
        "设备监控" to CategoryInfo("设备监控", "设备监控", Icons.Default.Devices, Color(0xFF00897B)),
        "订阅提醒" to CategoryInfo("订阅提醒", "订阅提醒", Icons.Default.CardMembership, Color(0xFF7B1FA2)),
        "配置变更" to CategoryInfo("配置变更", "配置变更", Icons.Default.Build, Color(0xFFF57C00)),
        "其他" to CategoryInfo("其他", "其他", Icons.Default.ChatBubbleOutline, Color(0xFF757575))
    )

    // 旧版英文枚举名 + 常见英文变体 → 中文分类名（兼容数据库历史数据）
    private val legacyMapping = mapOf(
        // 旧版 MessageCategory 枚举名（全大写，数据库实际存储值）
        "NODE_CHANGE" to "节点监控",
        "SYSTEM_ALERT" to "系统监控",
        "RECOVERY" to "节点监控",
        "UPDATE" to "配置变更",
        "DEVICE" to "设备监控",
        "SUBSCRIPTION" to "订阅提醒",
        "OTHER" to "其他",
        // 其他可能的英文变体
        "Device" to "设备监控",
        "Subscription" to "订阅提醒",
        "Node" to "节点监控",
        "System" to "系统监控",
        "Clash" to "配置变更"
    )

    /**
     * 获取分类信息，未知分类返回默认值
     * 自动兼容旧版英文枚举名
     */
    fun getCategory(name: String): CategoryInfo {
        val resolvedName = legacyMapping[name] ?: name
        return defaultCategories[resolvedName] ?: CategoryInfo(
            name = resolvedName,
            displayName = resolvedName,
            icon = Icons.Default.Label,
            fallbackColor = Color(0xFF9E9E9E)
        )
    }

    /**
     * 获取所有默认分类名称
     */
    fun getDefaultCategoryNames(): Set<String> = defaultCategories.keys
}
