# FlyPigs Server

自建通知推送 Android 客户端，配合 [ntfy](https://ntfy.sh) 服务端使用，专为 **Flypigs EventCenter** 设计——统一管理 OpenWrt 插件事件。

## 功能

- 🔔 实时推送通知（WebSocket 长连接）
- 📋 消息列表 + 详情查看
- 🗂 分类标签（节点变更 / 系统告警 / 恢复通知 / 更新通知）
- ✅ 一键已读
- 🔍 消息搜索
- 📊 事件统计分析
- 🎨 Material 3 主题（支持浅色/深色/跟随系统）
- 🔋 后台保活（基于 onTaskRemoved + AlarmManager）

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 Expressive |
| 架构 | MVVM + Clean Architecture |
| 网络 | OkHttp WebSocket（Basic Auth） |
| 数据库 | Room + Paging 3 |
| DI | Hilt |
| 图表 | 自绘 Canvas（BarChart / PieChart） |

## 环境要求

- Android 8.0+（API 26）
- ntfy 服务端（需开启认证，关闭匿名访问）

## 构建

```bash
# 配置签名（首次）
# 在 local.properties 中添加：
# RELEASE_STORE_FILE=../release.jks
# RELEASE_STORE_PASSWORD=<your_password>
# RELEASE_KEY_ALIAS=flypigs
# RELEASE_KEY_PASSWORD=<your_password>

# 编译 Release APK
./gradlew assembleRelease

# 输出路径
# app/build/outputs/apk/release/app-release.apk
```

## 服务端配置

ntfy Docker 部署参考：

```bash
docker run -d \
  --name ntfy \
  -p 2586:80 \
  -v /opt/ntfy:/etc/ntfy \
  ntfy/ntfy serve
```

`server.yml` 关键配置：

```yaml
base-url: "http://your-server:2586"
listen-http: ":80"
auth-default-access: "deny-all"
enable-signup: false
enable-login: true
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `INTERNET` | WebSocket 连接 ntfy 服务端 |
| `POST_NOTIFICATIONS` | 显示推送通知（Android 13+） |
| `RECEIVE_BOOT_COMPLETED` | 开机自启动 |
| `FOREGROUND_SERVICE` | 前台服务保活 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 电池优化白名单 |

## 许可

MIT License
