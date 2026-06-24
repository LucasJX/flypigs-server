# Task 6: 统计页面重构

## 目标
先展示关键数据卡片，再放图表。

## 页面结构
```
━━━━━━━━━━━━━━━━━━━━━━
LargeTopAppBar: 「分析」
━━━━━━━━━━━━━━━━━━━━━━

[总事件]  [124]     ← 统计卡片 (surfaceContainerLow, 28dp圆角)
[今日事件] [12]
[未读]    [2]

━━━ 事件趋势 ━━━      ← 分区标题
[折线图]              ← 7天趋势

━━━ 分类分布 ━━━
[饼图]                ← 分类占比
```

## 统计卡片
- 3 列等宽
- 每个卡片: surfaceContainerLow 背景, 28dp 圆角
- 标签: bodySmall, onSurfaceVariant
- 数字: headlineMedium, Bold, primary

## TopAppBar
- LargeTopAppBar
- 标题: 「分析」
- 背景: surface

## ⚠️ 注意事项
1. 修改 `StatsScreen.kt` (如果存在) 或 `StatisticsScreen.kt`
2. 不要删除图表组件
3. 统计卡片放在图表上方
4. JAVA_HOME=/home/flypigs/ntfy-app/.android-sdk/jdk-17.0.2
5. 不要 git push
