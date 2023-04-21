package org.bitkernel.udp;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

@Slf4j
public class TalkReceive implements Runnable {
    private DatagramSocket receive;
    private static final int MAX_LEN = 4096;
    private byte[] buff = new byte[MAX_LEN];
    private int port;
    private boolean isRunning = true;

    public TalkReceive(int port) {
        try {
            this.port = port;
            receive = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            String msg = receiveString();
            System.out.println(msg);
        }
    }

    public void close() {
        isRunning = false;
        receive.close();
    }

    public String receiveString() {
        DatagramPacket packet = receivePkt();
        return pktToString(packet);
    }

    public DatagramPacket receivePkt() {
        try {
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            receive.receive(packet);
            return packet;
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @NotNull
    public String pktToString(@NotNull DatagramPacket pkt) {
        byte[] bytes = pkt.getData();
        return new String(bytes, 0, pkt.getLength());
    }
}
