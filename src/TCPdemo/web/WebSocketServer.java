package TCPdemo.web;

import TCPdemo.ClientHandler;
import TCPdemo.RoomManager;
import TCPdemo.net.Connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 极简WebSocket服务：处理HTTP握手，握手成功后交给ClientHandler。
 */
public class WebSocketServer {

    /** RFC 6455规定的魔法字符串 */
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_CONN = 50;
    private final AtomicInteger onlineCount = new AtomicInteger(0);

    private final int port;
    private final RoomManager roomManager;

    public WebSocketServer(int port, RoomManager roomManager) {
        this.port = port;
        this.roomManager = roomManager;
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket(port);
        System.out.println("WebSocket 服务已启动，端口 " + port);
        System.out.println("  浏览器测试：ws://localhost:" + port + "/ws");

        ExecutorService pool = Executors.newFixedThreadPool(MAX_CONN);
        while (true) {
            final Socket socket = ss.accept();
            if (onlineCount.get() >= MAX_CONN) {
                try{
                    OutputStream out = socket.getOutputStream();
                    out.write("HTTP/1.1 503 Service Unavailable\r\n\r\n服务器已满".getBytes());
                    out.flush();
                }catch (IOException ignored){
                }finally {
                    try { socket.close(); } catch (IOException ignored) {}
                }
                continue;
            }
            onlineCount.incrementAndGet();
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        handle(socket);
                    }finally {
                        onlineCount.decrementAndGet();
                    }
                }
            });
        }
    }

    private void handle(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            //读HTTP请求行
            String requestLine = readHttpLine(in);
            if (requestLine == null) { socket.close(); return; }

            //解析路径：例如"GET /ws HTTP/1.1" -> "/ws"
            String[] parts = requestLine.split(" ");
            String path = parts.length > 1 ? parts[1] : "/";

            //读HTTP请求头
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = readHttpLine(in)) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    headers.put(line.substring(0, idx).trim().toLowerCase(),
                            line.substring(idx + 1).trim());
                }
            }

            //根据路径分发
            if (path.equals("/ws") || path.startsWith("/ws?")) {
                //WebSocket 升级
                handleWebSocket(socket, headers, out);
            } else if (path.equals("/") || path.equals("/index.html")) {
                //返回 index.html
                serveIndexHtml(out);
                socket.close();
            } else {
                //404
                String resp = "HTTP/1.1 404 Not Found\r\n"
                        + "Content-Type: text/plain; charset=utf-8\r\n"
                        + "Content-Length: 9\r\n"
                        + "Connection: close\r\n\r\n"
                        + "404 Not Found";
                out.write(resp.getBytes(StandardCharsets.UTF_8));
                out.flush();
                socket.close();
            }

        } catch (Exception e) {
            System.out.println("WebSocket 处理失败：" + e.getMessage());
        }
    }

    //处理WebSocket升级
    private void handleWebSocket(Socket socket, Map<String, String> headers, OutputStream out) throws Exception {
        String key = headers.get("sec-websocket-key");
        if (key == null) {
            socket.close();
            return;
        }

        String accept = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-1")
                        .digest((key + WS_GUID).getBytes(StandardCharsets.UTF_8)));

        String resp = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(resp.getBytes(StandardCharsets.UTF_8));
        out.flush();

        System.out.println("WebSocket 客户端连接：" + socket.getRemoteSocketAddress());

        Connection conn = new WebSocketConnection(socket);
        new ClientHandler(conn, roomManager).run();
    }

    //返回index.html
    private void serveIndexHtml(OutputStream out) throws IOException {
        byte[] body;
        java.nio.file.Path path = java.nio.file.Paths.get("web", "index.html");
        if (java.nio.file.Files.exists(path)) {
            body = java.nio.file.Files.readAllBytes(path);
        } else {
            String fallback = "<h1>index.html 未找到</h1><p>请把前端页面放到项目根目录的 web/ 下</p>";
            body = fallback.getBytes(StandardCharsets.UTF_8);
        }

        String head = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    /** 逐字节读一行，不能用BufferedReader（会预读吃掉帧数据） */
    private String readHttpLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        boolean any = false;
        while ((b = in.read()) != -1) {
            any = true;
            if (b == '\n') break;
            if (b != '\r') buf.write(b);
        }
        if (!any) return null;
        return buf.toString(StandardCharsets.UTF_8);
    }
}