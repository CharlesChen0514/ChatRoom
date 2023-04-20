package org.bitkernel.udp;

import lombok.extern.slf4j.Slf4j;

import java.net.DatagramSocket;
import java.net.SocketException;

@Slf4j
public class UdpServer extends Udp {
    public static final int PORT = 9090;

    public UdpServer() {
        try {
            socket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            logger.error(e.getMessage());
        }
    }
}
