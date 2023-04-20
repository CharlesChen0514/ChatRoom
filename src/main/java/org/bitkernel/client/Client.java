package org.bitkernel.client;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.ChatType;
import org.bitkernel.common.Data;
import org.bitkernel.common.User;
import org.bitkernel.tcp.TcpClient;
import org.bitkernel.udp.UdpClient;

import java.io.IOException;
import java.util.Scanner;

import static org.bitkernel.common.ChatType.*;

@Slf4j
public class Client {
    private final Scanner sc = new Scanner(System.in);
    private TcpClient tcpClient;
    private UdpClient udpClient;
    private User user;

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
        String name = sc.next();
//        System.out.println("Please input password：");
//        String passwd = sc.nextLine();
        user = new User(name, name);
        Data data = new Data(LOGIN.type, user);
        connectServer(data);
        startLocalServer();
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

    private void startLocalServer() {
        udpClient.startReceiver();
        tcpClient.startReceiver();
    }

    private void chatGuide() {
        System.out.println("Function menu:");
        menu.forEach(System.out::println);
        boolean flag = false;
        while (true) {
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
                case TCP_PRIVATE_MSG:
                case UDP_BROADCAST:
                    System.out.println("Input information to broadcast:");
                    sc.nextLine();
                    String msg = sc.nextLine();
                    // 如何
                    data = new Data(UDP_BROADCAST.type, user, msg);
                    break;
                case TCP_BROADCAST:
                case FILE_TRANSFER:
                case EXIT:
                    data = new Data(EXIT.type, user);
                    flag = true;
                    break;
                default:
                    System.out.println("Invalid selection, please re-enter");
            }
            udpClient.send(JSONObject.toJSONString(data));
            if (flag) {
                break;
            }
        }
        logger.info("Exit chat menu");
    }

    private static boolean testServer() {
        boolean tcp = TcpClient.testConnection();
        boolean udp = UdpClient.testServer();
        return tcp && udp;
    }
}
