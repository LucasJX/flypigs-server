package com.flypigs.ntfyapp.ui

import androidx.compose.runtime.*

/**
 * CompositionLocal 用于跨路由传递 drawer 内容和关闭回调
 * HomeScreen 写入，MainActivity 读取并渲染
 */
typealias DrawerContentLambda = @Composable () -> Unit

data class DrawerStateHolder(
    val content: DrawerContentLambda = {},
    val closeDrawer: () -> Unit = {}
)

val LocalDrawerState = staticCompositionLocalOf { mutableStateOf(DrawerStateHolder()) }

@Composable
fun DrawerContentHost(
    content: @Composable () -> Unit
) {
    val state = remember { mutableStateOf(DrawerStateHolder()) }
    CompositionLocalProvider(LocalDrawerState provides state) {
        content()
    }
}
