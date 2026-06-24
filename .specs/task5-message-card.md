# Task 5: 消息卡片重构

## 目标
更现代的卡片设计，支持来源徽章。

## 卡片样式
- 圆角: 28dp
- 阴影: 1dp
- tonalElevation: 2.dp
- 背景: surfaceContainerLow
- 内边距: 16dp

## 卡片布局
```
┌─────────────────────────┐
│ 🔄 订阅更新              │  ← 图标 + 标题 (titleMedium, Bold)
│   新增 3 个节点           │  ← 正文摘要 (bodyMedium, onSurfaceVariant)
│   移除 1 个节点           │
│ [OpenClash]  ·  2分钟前  │  ← 来源徽章 + 时间
└─────────────────────────┘
```

## 来源徽章
- 格式: [来源名] 
- 颜色映射:
  - OpenClash → 蓝色 (#1976D2)
  - PassWall → 紫色 (#7B1FA2)
  - MosDNS → 绿色 (#388E3C)
  - Docker → 橙色 (#F57C00)
- 样式: 小圆角矩形背景 + 白色文字

## 消息图标映射
- NODE_CHANGE → SwapHoriz (↔️)
- SYSTEM_ALERT → Warning (⚠️)
- RECOVERY → CheckCircle (✅)
- UPDATE → SystemUpdate (🔄)
- OTHER → Info (ℹ️)

## ⚠️ 注意事项
1. 修改 `MessageCard.kt` 
2. 修改 `MessageCategory.kt` 的 icon 和 color
3. 不要删除 MessageEntity 的字段
4. 来源徽章用 topic 字段映射
5. JAVA_HOME=/home/flypigs/ntfy-app/.android-sdk/jdk-17.0.2
6. 不要 git push
