package org.bitkernel.client;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.Data;
import org.bitkernel.common.User;
import org.bitkernel.udp.UdpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.bitkernel.common.ChatType.UDP_PRIVATE_MSG;

@Slf4j
public class Channel {
    private UdpClient localUdp;
    private String ip;
    private int port;
    private BufferedReader reader;
    private boolean isRunning = true;
    private User user;
    private String toName;

    public Channel(@NotNull UdpClient udpClient, @NotNull User user,
                   @NotNull Data data) {
        this.localUdp = udpClient;
        this.user = user;
        this.toName = data.getToName();
        String[] split = data.getMsg().split(":");
        ip = split[0];
        port = Integer.parseInt(split[1]);
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void start() {
        isRunning = true;
        Thread t1 = new Thread(new Send());
        Thread t2 = new Thread(new Receive());
        t1.start();
        t2.start();
    }

    public void close() {
        isRunning = false;
    }

    class Send implements Runnable {

        @Override
        public void run() {
            while (isRunning) {
                try {
                    String msg = reader.readLine();
                    Data data = new Data(UDP_PRIVATE_MSG.type, user, msg);
                    localUdp.sendDataTo(data, ip, port);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    class Receive implements Runnable {

        @Override
        public void run() {
            try {
                while (isRunning) {
                    Data data = localUdp.receiveData();
                    System.out.println(data.say());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
