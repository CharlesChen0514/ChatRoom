package org.bitkernel.udp;

import com.sun.istack.internal.NotNull;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class TalkSend implements Runnable {
    @Getter
    private DatagramSocket send;
    private BufferedReader br;
    private String toIp;
    private int toPort;
    private boolean isRunning = true;
    private InetSocketAddress toAddr;
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
                if(data.equals("bye")) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
