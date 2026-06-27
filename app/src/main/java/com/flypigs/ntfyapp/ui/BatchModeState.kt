package com.flypigs.ntfyapp.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局批量模式状态 — 用于 MainActivity 感知批量模式
 */
val LocalBatchMode = staticCompositionLocalOf { false }
