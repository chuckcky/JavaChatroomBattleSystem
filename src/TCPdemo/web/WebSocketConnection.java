package TCPdemo.web;

import TCPdemo.net.Connection;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * WebSocket版Connection：一条WebSocket文本消息=客户端的一条指令。
 * 握手已经在WebSocketServer里完成，这里只负责帧的读写。
 */
public class WebSocketConnection implements Connection {

    private static final int OP_TEXT  = 0x1;
    private static final int OP_BIN   = 0x2;
    private static final int OP_CLOSE = 0x8;
    private static final int OP_PING  = 0x9;
    private static final int OP_PONG  = 0xA;
    private static final int MAX_PAYLOAD = 1 << 20; // 1MB 上限

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final String remote;
    private final Deque<String> pending = new ArrayDeque<>();

    public WebSocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.remote = String.valueOf(socket.getRemoteSocketAddress());
    }

    @Override
    public String readLine() throws IOException {
        while (pending.isEmpty()) {
            if (!readMessage()) return null;
        }
        return pending.poll();
    }

    //读一条完整消息，按换行拆成多行放进pending
    private boolean readMessage() throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        while (true) {
            int b0 = in.read();
            if (b0 < 0) return false;
            int b1 = in.read();
            if (b1 < 0) return false;

            boolean fin    = (b0 & 0x80) != 0;
            int     opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long    len    = b1 & 0x7F;

            if (len == 126) {
                len = ((long) readByte() << 8) | readByte();
            } else if (len == 127) {
                len = 0;
                for (int i = 0; i < 8; i++) len = (len << 8) | readByte();
            }
            if (len < 0 || len > MAX_PAYLOAD) throw new IOException("WebSocket 帧过大: " + len);

            byte[] mask = new byte[4];
            if (masked) readFully(mask);

            byte[] data = new byte[(int) len];
            readFully(data);
            if (masked) {
                for (int i = 0; i < data.length; i++) data[i] ^= mask[i & 3];
            }

            //控制帧
            if (opcode == OP_CLOSE) return false;
            if (opcode == OP_PING)  { writeFrame(OP_PONG, data); continue; }
            if (opcode == OP_PONG)  continue;

            //数据帧（含分片续帧）
            if (opcode == OP_TEXT || opcode == OP_BIN || opcode == 0x0) {
                payload.write(data, 0, data.length);
            }

            if (fin) break;
        }

        String msg = payload.toString(StandardCharsets.UTF_8);
        for (String line : msg.split("\r?\n")) {
            String t = line.trim();
            if (!t.isEmpty()) pending.add(t);
        }
        return true;
    }

    private int readByte() throws IOException {
        int b = in.read();
        if (b < 0) throw new IOException("连接已关闭");
        return b;
    }

    private void readFully(byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) throw new IOException("连接已关闭");
            off += n;
        }
    }

    @Override
    public void writeLine(String line) {
        try {
            writeFrame(OP_TEXT, line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            //对端已断开，忽略
        }
    }

    private synchronized void writeFrame(int opcode, byte[] payload) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(payload.length + 10);
        buf.write(0x80 | opcode); // FIN=1

        int len = payload.length;
        if (len < 126) {
            buf.write(len);
        } else if (len < 65536) {
            buf.write(126);
            buf.write((len >>> 8) & 0xFF);
            buf.write(len & 0xFF);
        } else {
            buf.write(127);
            for (int i = 7; i >= 0; i--) {
                buf.write((int) (((long) len >>> (8 * i)) & 0xFF));
            }
        }
        buf.write(payload, 0, payload.length);

        out.write(buf.toByteArray());
        out.flush();
    }

    @Override
    public void close() {
        try { writeFrame(OP_CLOSE, new byte[0]); } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
    }

    @Override
    public String getRemoteAddress() {
        return remote;
    }
}