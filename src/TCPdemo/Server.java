package TCPdemo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) throws IOException {

        ServerSocket ss = new ServerSocket(10000);
        System.out.println("服务器启动，等待连接...");

        //创建唯一的房间管理员
        RoomManager roomManager = new RoomManager();

        //线程池 限制并发线程数，防止恶意/异常连接耗尽系统资源
        ExecutorService pool = Executors.newFixedThreadPool(50);

        while(true) {
            //一直循环等待
            Socket socket = ss.accept();
            System.out.println("新客户端上线：" + socket.getRemoteSocketAddress());
            //连接交给线程池处理，主线程立刻回去accept下一个
            pool.execute(new ClientHandler(socket,roomManager));
        }
    }
}
