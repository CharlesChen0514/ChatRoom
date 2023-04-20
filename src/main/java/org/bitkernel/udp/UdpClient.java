package org.bitkernel.udp;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.Data;
import org.bitkernel.server.Server;

import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

@Slf4j
public class UdpClient extends Udp {
    static final int TIME_OUT = Integer.MAX_VALUE;

    public UdpClient(@NotNull Data data) {
        try {
            socket = new DatagramSocket();
            socket.connect(Server.ip, UdpServer.PORT);
            socket.setSoTimeout(UdpClient.TIME_OUT);
            this.send(JSONObject.toJSONString(data));
            logger.debug("Successfully registered to UDP server: {}:{}",
                    Server.ip, UdpServer.PORT);
        } catch (SocketException e) {
            logger.error(e.getMessage());
        }
    }

    public static boolean testServer() {
        return testConnection(Server.ip, UdpServer.PORT);
    }

    public void send(@NotNull String msg) {
        SocketAddress addr = socket.getRemoteSocketAddress();
        send(addr, msg);
    }
}
