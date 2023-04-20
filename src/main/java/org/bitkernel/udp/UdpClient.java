package org.bitkernel.udp;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.Data;
import org.bitkernel.server.Server;

import java.io.IOException;
import java.net.*;

@Slf4j
public class UdpClient extends Udp {
    static final int TIME_OUT = Integer.MAX_VALUE;

    public UdpClient(@NotNull Data data) {
        try {
            socket = new DatagramSocket();
//            socket.connect(Server.ip, UdpServer.PORT);
            socket.setSoTimeout(UdpClient.TIME_OUT);
            this.sendServer(JSONObject.toJSONString(data));
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

    public void sendServer(@NotNull String msg) {
        byte[] bytes = msg.getBytes();
        DatagramPacket pkt = new DatagramPacket(bytes, bytes.length, Server.ip, UdpServer.PORT);
        try {
            socket.send(pkt);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void sendDataToServer(@NotNull Data data) {
        sendServer(JSONObject.toJSONString(data));
    }

    public void sendDataTo(@NotNull Data data, @NotNull String ip, int port) {
        String msg = JSONObject.toJSONString(data);
        byte[] bytes = msg.getBytes();
        try {
            InetAddress addr = InetAddress.getByAddress(ip.getBytes());
            DatagramPacket pkt = new DatagramPacket(bytes, bytes.length, addr, port);
            socket.send(pkt);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
