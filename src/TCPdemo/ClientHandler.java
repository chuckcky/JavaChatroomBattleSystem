package TCPdemo;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader br;
    private OutputStream os;
    private RoomManager roomManager;
    private String username;
    private GameRoom currentRoom;


    public ClientHandler(Socket socket, RoomManager roomManager) throws IOException {
        this.socket = socket;
        this.roomManager = roomManager;
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.os = socket.getOutputStream();
        this.username = null;
        this.currentRoom = null;
    }


    public String getUsername() {
        return username;
    }

    //发送消息
    public void sendMessage(String msg) {
        try {
            os.write((msg + "\n").getBytes());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String str;
            while ((str = br.readLine()) != null) {
                System.out.println("收到指令：" + str);

                if (username == null && str.startsWith("user")) {
                    String[] parts = str.split("\\s+");
                    if (parts.length == 2) {
                        username = parts[1];
                        sendMessage("用户名设置成功：" + username);
                        System.out.println("玩家 " + username + " 上线了");
                        continue;
                    } else {
                        sendMessage("格式错误，请使用：user 你的名字");
                        continue;
                    }
                }

                //如果还没设置用户名，拒绝其他操作
                if (username == null) {
                    sendMessage("请先设置用户名（指令：user 你的名字）");
                    continue;
                }

                //创建房间
                if ("create".equals(str)) {
                    String roomId = roomManager.createRoom(this);
                    currentRoom = roomManager.findRoom(roomId);
                    sendMessage("房间创建成功！房间号：" + roomId);
                    System.out.println("玩家 " + username + " 创建了房间 " + roomId);
                    continue;
                }

                //加入房间
                if (str.startsWith("join")) {
                    String[] parts = str.split("\\s+");
                    if (parts.length == 2) {
                        String roomId = parts[1];
                        boolean success = roomManager.joinRoom(roomId, this);
                        if (success) {
                            currentRoom = roomManager.findRoom(roomId);
                            sendMessage("加入房间 " + roomId + " 成功！");
                            System.out.println("玩家 " + username + " 加入了房间 " + roomId);
                            //通知房主有人加入
                            if (currentRoom.getPlayerCount() == 2) {
                                //两个玩家都到齐了
                                sendMessage("房间已满，可以开始游戏了！房主输入 start 开始游戏");
                            }
                        } else {
                            sendMessage("加入房间失败，房间不存在或已满。");
                        }
                    } else {
                        sendMessage("格式错误，请使用：join 房间号（例如：join room-001）");
                    }
                    continue;
                }
                //开始游戏（房主专用）
                if ("start".equals(str)) {
                    if (currentRoom == null) {
                        sendMessage("你还没有加入任何房间");
                        continue;
                    }
                    if (currentRoom.getPlayerCount() < 2) {
                        sendMessage("房间人数不足2人，无法开始游戏");
                        continue;
                    }
                    //检查是否已经是开始状态
                    currentRoom.startGame();
                    sendMessage("游戏已开始！请等待对手操作...");
                    System.out.println("玩家 " + username + " 开始了游戏");
                    continue;
                }

                //出牌（格式：play 卡牌序号 [目标序号]）
                if (str.startsWith("play")) {
                    if (currentRoom == null) {
                        sendMessage("你还没有加入房间");
                        continue;
                    }
                    handlePlayCard(str);
                    continue;
                }

                //攻击（格式：attack 随从序号 h 或 attack 随从序号 目标序号）
                if (str.startsWith("attack")) {
                    if (currentRoom == null) {
                        sendMessage("你还没有加入房间");
                        continue;
                    }
                    handleAttack(str);
                    continue;
                }

                //结束回合
                if ("end".equals(str) || "endturn".equals(str)) {
                    if (currentRoom == null) {
                        sendMessage("你还没有加入房间");
                        continue;
                    }
                    currentRoom.handleEndTurn(this);
                    continue;
                }

                //未知指令
                sendMessage("未知指令，请重新输入");
                sendMessage("可用指令：user 名字 / create / join 房间号 / start / play 序号 / attack 序号 h或目标序号 / end");
            }
        } catch (IOException e) {
            System.out.println("客户端 " + username + " 断开了");
        } finally {
            //断线清理：通知房间（对手判胜/房间解散），再关闭socket
            if (roomManager != null) {
                roomManager.onClientDisconnect(this);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //处理出牌指令
    private void handlePlayCard(String str) {
        String[] parts = str.split("\\s+");
        if (parts.length < 2) {
            sendMessage("格式错误，请使用：play 卡牌序号（从1开始，例如：play 1）");
            return;
        }

        try {
            int cardIndex = Integer.parseInt(parts[1])-1;
            String targetInfo = (parts.length >= 3) ? parts[2] : null;
            currentRoom.handlePlayCard(this, cardIndex, targetInfo);
        } catch (NumberFormatException e) {
            sendMessage("请输入有效的数字");
        }
    }

    //处理攻击指令
    private void handleAttack(String str) {
        String[] parts = str.split("\\s+");
        if (parts.length < 3) {
            sendMessage("格式错误，请使用：attack 随从序号 h（攻击主战者） 或 attack 随从序号 目标序号");
            return;
        }

        try {
            int minionIndex = Integer.parseInt(parts[1])-1;
            String targetType;
            int targetIndex = -1;

            if ("h".equalsIgnoreCase(parts[2])) {
                targetType = "player";
            } else {
                targetType = "minion";
                targetIndex = Integer.parseInt(parts[2])-1;
            }

            currentRoom.handleAttack(this, minionIndex, targetType, targetIndex);
        } catch (NumberFormatException e) {
            sendMessage("请输入有效的数字");
        }
    }
}