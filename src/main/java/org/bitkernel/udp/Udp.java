package org.bitkernel.udp;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.Data;
import org.bitkernel.common.JsonUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;

@Slf4j
public class Udp {
    private static final int MAX_LEN = 4096;
    @Getter
    protected DatagramSocket socket;

    class Receiver implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Data data = receiveData();
                    System.out.println(data.getMsg());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @NotNull
    public DatagramPacket newDataPacket() {
        byte[] buffer = new byte[MAX_LEN];
        return new DatagramPacket(buffer, buffer.length);
    }

    public void startReceiver() {
        Thread t1 = new Thread(new Receiver());
        t1.start();
        logger.info("Start udp receiver");
    }

    @NotNull
    public DatagramPacket receivePacket() throws IOException {
        DatagramPacket pkt = newDataPacket();
        socket.receive(pkt);
        return pkt;
    }

    @NotNull
    public String receiveString() throws IOException {
        DatagramPacket pkt = receivePacket();
        return new String(pkt.getData());
    }

    @NotNull
    public Data receiveData() throws IOException {
        DatagramPacket pkt = receivePacket();
        return JsonUtil.parseData(new String(pkt.getData()));
    }

    public void send(@NotNull SocketAddress addr,
                     @NotNull String msg) {
        try {
            byte[] bytes = msg.getBytes();
            DatagramPacket outPacket = new DatagramPacket(bytes, bytes.length, addr);
            socket.send(outPacket);
        } catch (IOException e) {
            logger.error("Occur error when send message({}) to {}", msg, addr);
        }
    }

    public void heartBeating(@NotNull SocketAddress addr) {
        send(addr, "{}");
        logger.info("{} test udp server port", addr);
    }

    public static boolean testConnection(@NotNull InetAddress ip,
                                         int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(ip, port);
            socket.setSoTimeout(UdpClient.TIME_OUT);

            Data data = new Data();
            String msg = JSONObject.toJSONString(data);
            byte[] bytes = msg.getBytes();
            DatagramPacket outPacket = new DatagramPacket(bytes, bytes.length);
            DatagramPacket inPacket = new DatagramPacket(bytes, bytes.length);

            socket.send(outPacket);
            socket.receive(inPacket);
            logger.info("UDP server {}:{} is available.", ip, port);
            System.out.println("UDP server is available");
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("UDP server {}:{} is unavailable.", ip, port);
            System.out.println("UDP server is unavailable");
        }
        return false;
    }
}
