package org.bitkernel.tcp;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.User;
import org.bitkernel.server.Server;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class TcpClient extends Tcp {
    public TcpClient(@NotNull User user) throws IOException {
        super(new Socket(Server.ip, TcpServer.PORT));
        String userStr = JSONObject.toJSONString(user);
        getPw().println(userStr);
        logger.debug("Successfully connected to TCP server: {}:{}",
                Server.ip, TcpServer.PORT);
    }

    public static boolean testConnection() {
        try (Socket socket = new Socket(Server.ip, TcpServer.PORT)) {
            if (socket.isConnected()) {
                logger.info("TCP server {}:{} is connectable",
                        Server.ip, TcpServer.PORT);
                System.out.println("TCP server is connectable");
                return true;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        logger.error("TCP server {}:{} is unconnectable",
                Server.ip, TcpServer.PORT);
        System.out.println("TCP server is unconnectable");
        return false;
    }
}
