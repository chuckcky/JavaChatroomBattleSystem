# 好友对战卡牌游戏

## 项目简介

基于Java SE开发的双人联机卡牌对战游戏。从**纯控制台的原生Socket版**升级为**传输层可插拔的双协议游戏服务器+Web可视化前端**——服务端同时支持原生TCP与WebSocket接入，浏览器端与桌面端可同房对战。

**项目演进**：
- **v1**（tag：`v1-console`）：基于原生Socket的C/S架构，纯控制台交互
- **v2**（master）：抽象`Connection`传输层接口，新增WebSocket接入+Web可视化前端

> 本项目是 [A-simple-card-game-based-on-Java-SE](https://github.com/chuckcky/A-simple-card-game-based-on-Java-SE) 的延续与升级：
> - 复用了`model/`和`service/`包下的卡牌逻辑与游戏引擎
> - 新增网络通信层，将单机交互升级为双人实时对战
> - 抽象传输层，支持TCP/WebSocket双协议接入

## 演示

浏览器打开 `http://localhost:8080/`，开两个标签页即可对战。

## 技术亮点

| 亮点 | 说明 |
|---|---|
| **传输层可插拔** | 抽象`Connection`接口，`ClientHandler`只依赖接口，游戏核心逻辑与传输实现完全解耦 |
| **双协议接入** | 原生TCP与WebSocket两种实现，浏览器端与桌面端可同房对战 |
| **WebSocket 实现** | 基于RFC 6455实现握手与帧编解码（含掩码处理、控制帧、分片续帧） |
| **服务端权威** | 所有指令经服务端校验（费用/手牌/回合合法性），客户端无法作弊 |
| **状态裁剪** | 按玩家视角下发状态报文，对手手牌只显示数量，不泄露具体牌面 |

## 架构设计
```
┌─────────────────────────────────────────────────────┐
│  前端层  web/index.html (原生HTML/CSS/JS)            │
└───────────────┬─────────────────────────────────────┘
                │ ws://localhost:8080/ws
┌───────────────▼─────────────────────────────────────┐
│  接入层                                              │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │ WebSocketServer  │  │  (可扩展 HTTP)   │          │
│  │ 握手+静态页服务   │  │                  │         │
│  └────────┬─────────┘  └──────────────────┘         │
│           │                                         │
│  ┌────────▼─────────┐                               │
│  │ Connection 接口  │  ← 传输层抽象                  │
│  │ ├ TcpConnection  │                               │
│  │ └ WsConnection   │                               │
│  └────────┬─────────┘                               │
└───────────┼─────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────┐
│  业务层                                              │
│  ClientHandler → RoomManager → GameRoom → Game      │
│                              → GameStateFormatter   │
└─────────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────┐
│  领域层                                              │
│  model/(Player, Card, Minion...)                    │
│  service/(Game引擎)                                 │
└─────────────────────────────────────────────────────┘
```
**关键设计**：
- `GameRoom`不感知传输层，只跟`ClientHandler`打交道
- `Connection`接口位于 `ClientHandler` 下方（更靠近网络）
- 加WebSocket时，`model/`、`service/`、`GameRoom` **一行没改**

## 项目结构
```
JavaChatroomBattleSystem/
├── src/
│ ├── TCPdemo/
│ │ ├── net/ # 传输层抽象
│ │ │ ├── Connection.java # 接口
│ │ │ └── TcpConnection.java # 原生Socket实现
│ │ ├── web/ # WebSocket接入层
│ │ │ ├── WebSocketServer.java
│ │ │ └── WebSocketConnection.java
│ │ ├── Client.java # 原生Socket客户端（控制台）
│ │ ├── Server.java # 服务器入口（TCP+WS双协议）
│ │ ├── ClientHandler.java # 协议解析+指令分发
│ │ ├── RoomManager.java # 房间管理（ConcurrentHashMap）
│ │ ├── GameRoom.java # 单局状态机+状态广播
│ │ └── GameStateFormatter.java # 状态序列化
│ ├── model/ # 领域实体
│ │ ├── Player.java
│ │ ├── Card.java / AttackCard / HealCard / Minion / SingleAttackCard / AOEAttackCard
│ │ └── CardFactory.java
│ └── service/
│ └── Game.java # 对战引擎（回合、伤害、胜负）
└── web/
└── index.html # Web前端
```

## 如何运行

### 环境要求
- JDK 21+
- IntelliJ IDEA（或其他Java IDE）

### 启动服务端

1. 用IDEA打开项目
2. 运行`src/TCPdemo/Server.java`
3. 控制台输出：
```
服务器启动，等待连接...
WebSocket 服务已启动，端口 8080
  浏览器测试：ws://localhost:8080/ws
```

### 方式 1：浏览器对战

1. 打开浏览器访问`http://localhost:8080/`
2. 开**两个标签页**，分别输入不同用户名
3. 标签页 A：创建房间 → 记下房间号
4. 标签页 B：输入房间号 → 加入房间
5. 标签页 A：点击"开始游戏"

### 方式 2：控制台对战（原生Socket）

1. 运行`src/TCPdemo/Client.java`（可启动多个实例）
2. 按提示输入指令

| 指令 | 格式 | 说明 |
|---|---|---|
| `user` | `user 小明` | 设置用户名 |
| `create` | `create` | 创建房间（房主） |
| `join` | `join room-001` | 加入房间 |
| `start` | `start` | 开始游戏（房主） |
| `play` | `play 1` | 打出第 1 张手牌 |
| `attack` | `attack 1 h` | 第 1 个随从攻击主战者 |
| `attack` | `attack 1 2` | 第 1 个随从攻击对方第 2 个随从 |
| `end` | `end` | 结束回合 |

## 核心设计说明

### 1. 传输层抽象

```java
public interface Connection {
    String readLine() throws IOException;
    void writeLine(String line);
    void close();
    String getRemoteAddress();
}
```

`ClientHandler` 只依赖 `Connection`，不感知底层是 TCP 还是 WebSocket。新增协议实现只需新增一个 `Connection` 的实现类。

### 2. 服务端权威

所有游戏逻辑在服务端执行，客户端只发"意图"（如 `play 1`），服务端校验合法性后广播状态：

- 回合归属校验（拒绝非当前玩家操作）
- 费用 / 手牌序号校验
- 单体法术目标校验

### 3. 状态裁剪

`GameStateFormatter` 按玩家视角生成两份报文：自己的手牌显示完整牌面，对手的手牌只显示数量，**不泄露具体牌面**。





