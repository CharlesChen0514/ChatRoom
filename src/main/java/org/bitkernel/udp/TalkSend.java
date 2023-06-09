package org.bitkernel.udp;

import com.sun.istack.internal.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

@Slf4j
public class TalkSend implements Runnable {
    @Getter
    private DatagramSocket send;
    private BufferedReader br;
    private final String toIp;
    private final int toPort;
    private boolean isRunning = true;
    private final InetSocketAddress toAddr;

    public TalkSend(int port, @NotNull String toIp, int toPort) {
        this.toIp = toIp;
        this.toPort = toPort;
        this.toAddr = new InetSocketAddress(this.toIp, this.toPort);
        try {
            send = new DatagramSocket(port);
            br = new BufferedReader(new InputStreamReader(System.in));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public TalkSend(@NotNull DatagramSocket send, @NotNull String toIp, int toPort) {
        this.send = send;
        this.toIp = toIp;
        this.toPort = toPort;
        this.toAddr = new InetSocketAddress(this.toIp, this.toPort);
        br = new BufferedReader(new InputStreamReader(System.in));
    }

    public void send(@NotNull String msg) {
        byte[] bytes = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, toAddr);
        try {
            send.send(packet);
            logger.debug("UDP send message [{}] to {}", msg, toAddr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        isRunning = false;
        send.close();
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            try {
                String data = br.readLine();
                send(data);
                if (data.equals("bye")) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
