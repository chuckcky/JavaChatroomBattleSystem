package TCPdemo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


//房價注冊表
public class RoomManager {
    //ConcurrentHashMap   存所有房间
    private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private int roomCounter = 0;

    //创建房间
    public synchronized String createRoom(ClientHandler host) {
        roomCounter++;
        String roomId = "room-" + String.format("%03d", roomCounter);
        GameRoom room = new GameRoom(roomId, host);
        rooms.put(roomId, room);
        System.out.println("房间创建成功：" + roomId);
        return roomId;
    }

    //加入房间
    public boolean joinRoom(String roomId, ClientHandler player) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return false;
        }
        synchronized (room){
        if (room.getPlayerCount() >= 2) {
            return false;
        }
        if (room.containsHandler(player)) {
            return false;
        }
        room.addPlayer(player);
        }
        System.out.println("玩家加入房间成功：" + roomId);
        return true;
    }

    //查找房间
    public GameRoom findRoom(String roomId) {
        return rooms.get(roomId);
    }

    //移除房间
    public void removeRoom(String roomId) {
        rooms.remove(roomId);
        System.out.println("房间已移除：" + roomId);
    }

    //客户端断线：找到它所在的房间，通知房间处理并移除
    public void clientDisconnect(ClientHandler handler) {
        for (GameRoom room : rooms.values()) {
            if (room.containsHandler(handler)) {
                room.handleDisconnect(handler);
                rooms.remove(room.getRoomId());
                System.out.println("玩家 " + handler.getUsername() + " 断线，房间 " + room.getRoomId() + " 已解散");
                break;
            }
        }
    }
}
