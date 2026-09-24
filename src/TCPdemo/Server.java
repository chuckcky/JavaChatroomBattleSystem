package TCPdemo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import TCPdemo.net.TcpConnection;
import TCPdemo.web.WebSocketServer;

public class Server {
    //服务器最大同时在线连接数
    private static final int MAX = 50;

    public static void main(String[] args) throws IOException {

        //创建服务器Socket，监听10000端口
        ServerSocket ss = new ServerSocket(10000);
        System.out.println("服务器启动，等待连接...");

        //创建唯一的房间管理器
        RoomManager roomManager = new RoomManager();

        //启动WebSocket服务（独立线程，端口8080）
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new WebSocketServer(8080, roomManager).start();
                } catch (IOException e) {
                    System.out.println("WebSocket 服务启动失败：" + e.getMessage());
                }
            }
        }).start();

        //核心线程数=最大线程数=50
        //底层用的是无界队列LinkedBlockingQueue
        //如果不做额外限制的话第51个客户端会进入队列无限等待
        //所以我们使用AtomicInteger手动限制在线连接数
        ExecutorService pool = Executors.newFixedThreadPool(50);
        AtomicInteger onlineCount = new AtomicInteger(0);

        while(true) {
            //一直循环等待
            Socket socket = ss.accept();
            System.out.println("新客户端上线：" + socket.getRemoteSocketAddress());
            if (onlineCount.get() >= MAX){
                try {
                    //给客户端发送一条提示
                    OutputStream os = socket.getOutputStream();
                    os.write("服务器已满，请稍后再试\n".getBytes());
                    os.flush();
                }catch (IOException e){
                    //客户端可能已经断开，忽略
                }finally {
                    try {
                        socket.close();
                    }catch (IOException e) {
                        //忽略关闭异常
                    }
                }
                //跳过本次循环，不接受这个客户端
                continue;
            }
            //在线数+1
            onlineCount.incrementAndGet();
            pool.execute(new Runnable(){
                @Override
                public void run() {
                    try {
                        new ClientHandler(new TcpConnection(socket), roomManager).run();
                    } catch (IOException e) {
                        System.out.println("连接初始化失败：" + e.getMessage());
                    } finally {
                        //无论任务正常结束还是异常结束，在线数都要 -1
                        onlineCount.decrementAndGet();
                    }
                }
            });
        }
    }
}
