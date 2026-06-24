# Task 1: Material 3 Expressive 设计基础

## 目标
统一整个 App 的设计语言：颜色系统、圆角规范、Edge-to-Edge、动画。

## 颜色系统（Color.kt + Theme.kt）
### 浅色模式
- background: #F8F9FC
- surface: #FFFFFF
- surfaceContainer: 按 M3 标准计算
- surfaceContainerLow: 按 M3 标准计算
- primary: 保持动态色 (Material You)
- onPrimary: White
- onBackground: #1B1B1F
- onSurface: #1B1B1F

### 深色模式
- 按 M3 标准 dark scheme
- 保持 Material You 动态色

### 移除
- 删除 Blue700, Blue300, Teal600, Teal400 等硬编码色值
- 全部用 M3 动态色或 surface 系列

## 圆角规范
- Card: 28dp
- Chip: 16dp  
- Button: 20dp
- BottomBar: 24dp
- NavigationDrawerItem: 12dp (已有)

## Edge-to-Edge
- enableEdgeToEdge() 保留
- 状态栏透明
- 导航栏透明
- TopAppBar 背景色延伸到状态栏后面

## 动画
- 页面切换: 300ms FastOutSlowIn
- 列表项入场: fadeIn + slideInVertically

## ⚠️ 注意事项
1. 只修改 `~/ntfy-app/app/src/main/java/com/flypigs/ntfyapp/ui/theme/` 目录下的文件
2. 不要修改业务逻辑文件
3. 修改后运行 `./gradlew assembleRelease` 验证编译
4. 不要 git push
5. JAVA_HOME=/home/flypigs/ntfy-app/.android-sdk/jdk-17.0.2
