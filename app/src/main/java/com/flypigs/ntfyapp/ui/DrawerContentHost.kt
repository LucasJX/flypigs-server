package com.flypigs.ntfyapp.ui

import androidx.compose.runtime.*

/**
 * CompositionLocal 用于跨路由传递 drawer 内容
 * HomeScreen 写入，NavGraph 读取并渲染
 */
typealias DrawerContentLambda = @Composable () -> Unit

val LocalDrawerContent = staticCompositionLocalOf<MutableState<DrawerContentLambda>> {
    mutableStateOf({})
}

@Composable
fun DrawerContentHost(
    content: @Composable () -> Unit
) {
    val drawerContent = remember { mutableStateOf<DrawerContentLambda>({}) }
    CompositionLocalProvider(LocalDrawerContent provides drawerContent) {
        content()
    }
}
