package TCPdemo.net;

import java.io.IOException;

/**
 * 传输层抽象：屏蔽原生Socket/WebSocket的差异。
 * ClientHandler只依赖这个接口
 */
public interface Connection {
    //读一行(不含换行符)，对端关闭返回null
    String readLine() throws IOException;

    //写一行(实现方自行补分隔符)
    void writeLine(String line);

    //关闭连接
    void close();

    //远端地址，仅用于日志
    String getRemoteAddress();
}