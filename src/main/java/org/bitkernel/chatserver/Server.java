package org.bitkernel.chatserver;

//import lombok.extern.slf4j.Slf4j;

import lombok.extern.slf4j.Slf4j;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Slf4j
public class Server {
    private static final int UDP_PORT = 9090;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
            socket.receive(packet);
            System.out.println(packet.getAddress().getHostAddress());
            System.out.println(new String(packet.getData(), 0, packet.getLength()));
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
        }

    }
}
