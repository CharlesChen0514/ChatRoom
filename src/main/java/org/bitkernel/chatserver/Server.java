package org.bitkernel.chatserver;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

@Slf4j
public class Server {
    private static final int UDP_PORT = 9090;
    private static final int MAX_LEN = 4096;
    private byte[] inBuff = new byte[MAX_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
    private DatagramPacket outPacket;
    private DatagramSocket socket;

    public Server() {
        try {
            socket = new DatagramSocket(UDP_PORT);
        } catch (SocketException e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server s = new Server();
        while (true) {
            s.receive();
        }
    }

    private void receive() {
        try {
            socket.receive(inPacket);
            String msg = new String(inPacket.getData());
            react(msg);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * code field meaning: <br>
     * 0 --> heart beat detection
     * @param msg Json string
     */
    private void react(@NotNull String msg) {
        JSONObject json = JSONObject.parseObject(msg);
        int code = Integer.parseInt(json.getString("code"));
        try {
            switch (code) {
                case 0:
                    heartBeating(msg);
                    break;
                default:
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void heartBeating(@NotNull String msg) throws IOException {
        byte[] bytes = msg.getBytes();
        outPacket = new DatagramPacket(bytes, bytes.length, inPacket.getSocketAddress());
        socket.send(outPacket);
    }
}
