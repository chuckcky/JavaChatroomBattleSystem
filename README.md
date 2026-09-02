# 基于Java Socket的好友对战卡牌游戏

## 项目简介
这是基于Java SE开发的好友对战卡牌游戏，采用C/S架构，支持双人在线实时对战。该项目在单机版卡牌游戏的基础上，运用Socket+多线程技术实现了网络通信，玩家可以通过控制台进行房间创建、加入房间、出牌、攻击等操作，体验完整的回合制对战流程。

本项目是(https://github.com/chuckcky/A-simple-card-game-based-on-Java-SE)的延续与升级：
- 直接复用了 `model/` 和 `service/` 包下的卡牌逻辑与游戏引擎
- 新增网络通信层，将单机交互升级为双人实时对战
- 服务器端担任“裁判”角色，统一管理游戏状态并广播给双方客户端

## 项目结构
```
src/
├── TCPdemo/
│   ├── Client.java
│   ├── Server.java
│   ├── ClientHandler.java
│   ├── RoomManager.java
│   ├── GameRoom.java
│   └── GameStateFormatter.java
├── model/ 
│   ├── Player.java
│   ├── Card.java
│   ├── AttackCard.java
│   ├── HealCard.java
│   ├── Minion.java
│   └── CardFactory.java
└── service/
    └── Game.java
```
## 如何运行
1. 克隆项目到本地
2. 用 IDEA 打开项目
3. 先运行 `TCPdemo/Server.java` 启动服务器
4. 再运行 `TCPdemo/Client.java` 启动客户端（可启动多个）
5. 每个客户端按提示输入指令进行对战

### 指令说明
| 指令 | 格式 | 说明 |
| :--- | :--- | :--- |
| `user` | `user XXX` | 设置用户名 |
| `create` | `create` | 创建房间（房主） |
| `join` | `join room-00X` | 加入房间 |
| `start` | `start` | 开始游戏（房主） |
| `play` | `play 0` | 打出第0张手牌 |
| `attack` | `attack 1 h` | 第1个随从攻击主战者 |
| `attack` | `attack 1 1` | 第1个随从攻击对方第1个随从 |
| `end` | `end` | 结束回合 |
| `886` | `886` | 断开连接 |

