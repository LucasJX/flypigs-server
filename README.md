```
 ███████╗██╗  ██╗██████╗ ██████╗ ██╗ ██████╗ ███████╗
 ██╔════╝╚██╗██╔╝██╔══██╗██╔══██╗██║██╔═══██╗██╔════╝
 █████╗   ╚███╔╝ ██████╔╝██████╔╝██║██║   ██║███████╗
 ██╔══╝   ██╔██╗ ██╔═══╝ ██╔══██╗██║██║   ██║╚════██║
 ███████╗██╔╝ ██╗██║     ██║  ██║██║╚██████╔╝███████║
 ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝  ╚═╝╚═╝ ╚═════╝ ╚══════╝
```

# 🛰️ FlyPigs Server · 事件中心移动端

> **「路由器在说话，你得听见。」**

当你的 OpenWrt 路由器检测到节点故障、设备异动、系统过载——
那些关乎网络安全的 **关键事件**，不应该淹没在日志的深渊里。

**FlyPigs Server** 是 [EventCenter](https://github.com/LucasJX/luci-app-eventcenter) 的移动端神经末梢——
一个运行在你口袋里的 **实时事件监控终端**，
通过 WebSocket 长连接与 ntfy 服务端保持心跳，
将路由器的每一次脉搏，转化为你掌心的通知推送。

---

## ⚡ 核心能力

```
┌─────────────────────────────────────────────────────────────┐
│                    🧠 NEURAL INTERFACE                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐    WebSocket     ┌──────────┐    ┌────────┐  │
│  │  ntfy    │◄═══════════════► │  FlyPigs │───►│ 你     │  │
│  │  Server  │   实时双向通道    │  Server  │    │ 的手机 │  │
│  └────▲─────┘                  └──────────┘    └────────┘  │
│       │                                                     │
│       │  HTTP POST                                         │
│       │                                                     │
│  ┌────┴─────┐                  ┌──────────┐                 │
│  │ OpenWrt  │────────────────► │ ntfy     │                 │
│  │ EventC-  │  事件推送         │ Server   │                 │
│  │ enter    │                  │          │                 │
│  └──────────┘                  └──────────┘                 │
│                                                             │
│  ◆ 节点故障转移  ◆ 设备上下线  ◆ 系统告警  ◆ 订阅到期      │
└─────────────────────────────────────────────────────────────┘
```

| 能力 | 描述 |
|:-----|:-----|
| 🔔 **实时推送** | WebSocket 长连接 + 自动重连 + 断线历史补拉 |
| 📋 **消息中心** | 分页加载、未读标记、批量操作、全文搜索 |
| 🗂️ **智能分类** | 自动识别 `ec:` 标签，节点/系统/设备/订阅/配置 五大分类 |
| 📊 **数据分析** | 日/周/月事件趋势图、分类饼图、Topic 热力图 |
| 🎨 **Material 3** | Dynamic Color、深色模式、Expressive 动效 |
| 🔋 **后台不死** | `START_STICKY` + `AlarmManager` + 电池优化白名单 |
| 🔄 **断线自愈** | 指数退避重连（5s→10s→15s→30s→60s），20次失败后 5 分钟重置 |

---

## 🏗️ 技术架构

```
╔═══════════════════════════════════════════════════════════════╗
║                      PRESENTATION LAYER                       ║
║  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             ║
║  │ HomeScreen  │ │ StatsScreen │ │ DetailScreen│             ║
║  │  消息列表    │ │  数据分析    │ │  消息详情    │             ║
║  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘             ║
║         │               │               │                     ║
║  ═══════╪═══════════════╪═══════════════╪═══════════════════  ║
║                      DOMAIN LAYER                             ║
║  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             ║
║  │ MessageRepo │ │  TopicRepo  │ │  ServerRepo │             ║
║  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘             ║
║         │               │               │                     ║
║  ═══════╪═══════════════╪═══════════════╪═══════════════════  ║
║                        DATA LAYER                             ║
║  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             ║
║  │    Room     │ │   Secure    │ │   OkHttp    │             ║
║  │  Database   │ │  Storage    │ │  WebSocket  │             ║
║  └─────────────┘ └─────────────┘ └─────────────┘             ║
╚═══════════════════════════════════════════════════════════════╝
```

| 层级 | 技术栈 |
|:-----|:-------|
| **UI** | Jetpack Compose + Material 3 Expressive |
| **架构** | MVVM + Clean Architecture + Hilt DI |
| **网络** | OkHttp WebSocket（Basic Auth / Bearer Token） |
| **持久化** | Room + Paging 3 + EncryptedSharedPreferences |
| **图表** | 自绘 Canvas（BarChart / PieChart / LineChart） |
| **最低版本** | Android 8.0（API 26） |

---

## 🚀 快速开始

### 1. 配置签名

```properties
# local.properties
RELEASE_STORE_FILE=../release.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=flypigs
RELEASE_KEY_PASSWORD=your_password
```

### 2. 编译

```bash
./gradlew assembleRelease
# 输出 → app/build/outputs/apk/release/app-release.apk
```

### 3. 安装 & 配置

1. 安装 APK 到手机
2. 打开 App → **设置** → 添加服务器
3. 填入 ntfy 服务器地址、用户名、密码
4. 添加订阅 Topic（如 `Openwrt`）
5. 开启通知权限 + 电池优化白名单

---

## 🔧 服务端部署

```bash
docker run -d \
  --name ntfy \
  -p 2586:80 \
  -v /opt/ntfy:/etc/ntfy \
  ntfy/ntfy serve
```

```yaml
# server.yml
base-url: "http://your-server:2586"
listen-http: ":80"
auth-default-access: "deny-all"
enable-signup: false
enable-login: true
```

---

## 🛡️ 权限矩阵

| 权限 | 用途 | 必要性 |
|:-----|:-----|:------:|
| `INTERNET` | WebSocket 连接 ntfy 服务端 | ✅ |
| `POST_NOTIFICATIONS` | 显示推送通知（Android 13+） | ✅ |
| `RECEIVE_BOOT_COMPLETED` | 开机自启动 | ✅ |
| `FOREGROUND_SERVICE` | 前台服务保活 | ✅ |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 电池优化白名单 | 推荐 |

---

## 📡 事件流协议

```
┌──────────┐  WebSocket (wss://)  ┌──────────┐
│  ntfy    │◄════════════════════►│  FlyPigs │
│  Server  │                      │  Server  │
└────┬─────┘                      └────┬─────┘
     │                                 │
     │  GET /topic/json?since=T        │  onMessage()
     │  (历史补拉)                      │  ↓
     │                                 │  Room.insertMessage()
     │                                 │  ↓
     │                                 │  showNotification()
     │                                 │
     │  {"event":"message",            │
     │   "id":"xxx",                   │
     │   "time":1782611144,            │
     │   "topic":"Openwrt",            │
     │   "title":"节点故障转移",        │
     │   "message":"...",              │
     │   "priority":3,                 │
     │   "tags":["ec:节点监控"]}        │
     ▼                                 ▼
```

---

## 📜 许可

```
MIT License

Copyright (c) 2026 FlyPigs

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

<div align="center">

**「在网络的暗流中，做你的守夜人。」**

*Built with ❤️ by FlyPigs*

</div>
