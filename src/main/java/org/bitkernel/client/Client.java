package org.bitkernel.client;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.ChatType;
import org.bitkernel.common.Data;
import org.bitkernel.common.JsonUtil;
import org.bitkernel.common.User;
import org.bitkernel.tcp.TcpClient;
import org.bitkernel.udp.UdpClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static org.bitkernel.common.ChatType.*;

@Slf4j
public class Client {
    private final Scanner sc = new Scanner(System.in);
    private TcpClient tcpClient;
    private UdpClient udpClient;
    private User user;
    private boolean isRunning = true;
    private final ChannelMap channelMap = new ChannelMap();
    private final Map<String, String> ipMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> portMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Welcome to the chat room");
        Client c = new Client();
        c.login();
        c.chatGuide();
    }

    private void login() {
        if (!testServer()) {
            System.out.println("Server is unavailable, please ensure the server is started");
            System.exit(-1);
        }
        System.out.print("Please input username：");
        String name = sc.nextLine();
        user = new User(name, name);
        Data data = new Data(LOGIN.type, user);
        connectServer(data);
        start();
    }

    private void connectServer(@NotNull Data data) {
        try {
            tcpClient = new TcpClient(data.getUser());
            udpClient = new UdpClient(data);
            return;
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        logger.error("User {} login failed", data.getUser());
        System.out.println("User login failed");
    }

    private void start() {
        isRunning = true;
        Thread t1 = new Thread(new UdpListener());
        t1.start();
    }

    private void chatGuide() {
        System.out.println("Function menu:");
        menu.forEach(System.out::println);
        while (isRunning) {
            int code = sc.nextInt();
            ChatType type = ChatType.typeToEnumMap.get(code);
            System.out.println("Select: " + type.description);
            Data data = new Data();
            switch (type) {
                case UDP_ONLINE_USERS:
                    data = new Data(UDP_ONLINE_USERS.type, user);
                    break;
                case TCP_ONLINE_USERS:
                    data = new Data(TCP_ONLINE_USERS.type, user);
                    break;
                case UDP_PRIVATE_MSG:
                    break;
                case TCP_PRIVATE_MSG:
                case UDP_BROADCAST:
                    System.out.println("Input information to broadcast:");
                    sc.nextLine();
                    String msg = sc.nextLine();
                    data = new Data(UDP_BROADCAST.type, user, msg);
                    break;
                case TCP_BROADCAST:
                case FILE_TRANSFER:
                case EXIT:
                    data = new Data(EXIT.type, user);
                    isRunning = false;
                    break;
                default:
                    System.out.println("Invalid selection, please re-enter");
            }
            udpClient.sendServer(JSONObject.toJSONString(data));
        }
        logger.info("Exit chat menu");
    }

    private static boolean testServer() {
        boolean tcp = TcpClient.testConnection();
        boolean udp = UdpClient.testServer();
        return tcp && udp;
    }

    class TcpListener implements Runnable {

        @Override
        public void run() {

        }
    }

    class UdpListener implements Runnable {
        @Override
        public void run() {
            logger.info("UDP listener started successfully");
            while (isRunning) {
                try {
                    DatagramPacket pkt = udpClient.receivePacket();
                    Data data = JsonUtil.parseData(new String(pkt.getData()));
                    response(pkt.getSocketAddress(), data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void response(@NotNull SocketAddress addr,
                          @NotNull Data data) {
        ChatType type = typeToEnumMap.get(data.getType());
        switch (type) {
            case HEART_BEAT:
                udpClient.heartBeating(addr);
                break;
            case UDP_PRIVATE_MSG:
                System.out.println(data.say());
                break;
            case FILE_TRANSFER:
            case EXIT:
            case NEW_USER:
                String[] split = data.getMsg().split(":");
                int port = Integer.parseInt(split[1]);
                String ip = split[0];
                String name = data.getUser().getName();
                ipMap.put(name, ip);
                portMap.put(name, port);
                logger.debug("New user: {}:{}", name, data.getMsg());
                break;
            default:
                System.out.println("Invalid type, please check");
        }
    }
}
