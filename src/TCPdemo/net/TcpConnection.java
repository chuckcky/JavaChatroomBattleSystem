package TCPdemo.net;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

//原生Socket的实现，行为和以前完全一致
public class TcpConnection implements Connection {

    private final Socket socket;
    private final BufferedReader reader;
    private final OutputStream out;

    public TcpConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = socket.getOutputStream();
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public void writeLine(String line) {
        try {
            synchronized (this) {
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException e) {
            //对端已断开：读线程会感知到，这里静默忽略
        }
    }

    @Override
    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public String getRemoteAddress() {
        return String.valueOf(socket.getRemoteSocketAddress());
    }
}