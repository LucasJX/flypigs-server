# Task 4: 分类筛选器 — ScrollableTabRow

## 目标
从 AssistChip 改为 ScrollableTabRow，更清爽。

## 当前
- LazyRow + CategoryChip (AssistChip 风格)
- 分类: 全部 / 节点变更 / 系统告警 / 恢复通知 / 更新通知 / 其他

## 目标
- ScrollableTabRow
- Tab 项: 全部 | 节点 | 订阅 | 告警 | 恢复 | 更新
- containerColor: surface
- 选中 indicator: primary 色
- 选中文字: primary 色
- 未选中文字: onSurfaceVariant

## 分类标签简化
- NODE_CHANGE → 「节点」
- SYSTEM_ALERT → 「告警」
- RECOVERY → 「恢复」
- UPDATE → 「更新」
- OTHER → 「其他」
- 新增: SUBSCRIPTION → 「订阅」(预留)

## MessageCategory.kt 更新
- displayName 改为简短中文
- 新增 SUBSCRIPTION 枚举值 (图标: Sync, 颜色: 蓝)

## ⚠️ 注意事项
1. 修改 `HomeScreen.kt` 中的分类筛选部分
2. 修改 `MessageCategory.kt` 的 displayName
3. 修改 `CategoryChip.kt` 或创建新的 Tab 组件
4. 不要删除 MessageCategory 枚举值，只改 displayName
5. JAVA_HOME=/home/flypigs/ntfy-app/.android-sdk/jdk-17.0.2
6. 不要 git push
