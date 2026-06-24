# Task 3: 侧边栏重构 — 简洁面板风格

## 目标
去掉大蓝块渐变，改为简洁的 surfaceContainer 面板。

## Drawer Header
### 当前
- 200dp 渐变头部 (primary → primaryContainer → surface)
- 蓝色背景 + 铃铛图标 + 标题 + 副标题

### 目标
- 高度: 96dp (不超过 120dp)
- 颜色: surfaceContainer (不要渐变! 不要纯蓝!)
- 内容:
  - 🔔 图标 (onSurfaceVariant 色)
  - 「Flypigs EventCenter」 标题 (titleMedium, Bold)
  - 「统一管理 OpenWrt 事件」 副标题 (bodySmall, onSurfaceVariant)

## Drawer 菜单
### 结构
- 分区标题: 「消息来源」 (labelSmall, onSurfaceVariant, 全大写)
- 全部消息 (Inbox icon, 带未读数)
- OpenClash (Topic icon)
- 未来: PassWall, MosDNS, HomeProxy, Docker, Watchcat

### 底部
- 分割线
- 「关于」
- 「v1.0.0」

## ⚠️ 注意事项
1. 只修改 `HomeScreen.kt` 中的 `ModalNavigationDrawer` 部分
2. 不要删除 drawerContent 的功能逻辑
3. 不要用 Brush.verticalGradient
4. 不要用 MaterialTheme.colorScheme.primary 作为 drawer 背景
5. JAVA_HOME=/home/flypigs/ntfy-app/.android-sdk/jdk-17.0.2
6. 不要 git push
