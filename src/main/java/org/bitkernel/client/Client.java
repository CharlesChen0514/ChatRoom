package org.bitkernel.client;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.ChatType;
import org.bitkernel.common.User;
import org.bitkernel.server.Server;
import org.bitkernel.tcp.Tcp;
import org.bitkernel.udp.TalkReceive;
import org.bitkernel.udp.TalkSend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static org.bitkernel.common.ChatType.menu;

@Slf4j
public class Client {
    private final Scanner sc = new Scanner(System.in);
    private Tcp serverTcp;
    private TalkReceive talkReceive;
    private TalkSend sendServer;
    private ConcurrentHashMap<String, TalkSend> sendMap = new ConcurrentHashMap<>();
    private User user;
    private boolean isRunning = true;
    private static final int MAX_SIZE = 100;

    public static void main(String[] args) {
        System.out.println("Welcome to the chat room");
        Client c = new Client();
        c.login();
        c.chatGuide();
    }

    private void login() {
        System.out.println("Welcome to chat room, please login");
        System.out.print("Input username: ");
        String name = sc.next();
        System.out.print("UDP send port: ");
        int sendPort = sc.nextInt();
        System.out.print("UDP receiver port: ");
        int receivePort = sc.nextInt();
        user = new User(name, Server.ip, receivePort, sendPort);
        try {
            serverTcp = new Tcp();
            serverTcp.send(user.toString());
            talkReceive = new TalkReceive(user.getReceivePort());
            sendServer = new TalkSend(user.getSendPort(), Server.ip, Server.UDP_RECEIVE_PORT);
            Thread t1 = new Thread(new TcpProcessor());
            t1.start();
            Thread t2 = new Thread(new UdpProcessor());
            t2.start();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void chatGuide() {
        System.out.println("Command guide:");
        menu.forEach(System.out::println);
        sc.nextLine();
        while (isRunning) {
            String cmdLine = sc.nextLine();
            if (cmdLine.isEmpty()) {
                System.out.println("Command error, please re-entered");
                continue;
            }
            String[] args = cmdLine.split("@");
            ChatType type = ChatType.typeToEnumMap.get(args[0].trim());
            if (type == null) {
                System.out.println("Command error, please re-entered");
                continue;
            }
            String pktStr = user.getName() + "@" + cmdLine;
            switch (type) {
                case ONLINE_USERS:
                    sendServer.send(pktStr);
                    break;
                case PRIVATE_MSG:
                    pm(pktStr);
                    break;
                case FILE_TRANSFER:
                case EXIT:
                    sendServer.send(pktStr);
                    System.exit(-1);
                    break;
                default:
                    System.out.println("Invalid selection, please re-enter");
            }
        }
        logger.info("Exit chat menu");
    }

    private void pm(@NotNull String pktStr) {
        String[] args = pktStr.split("@");
        if (args.length != 4) {
            System.out.println("Command error, please re-entered");
            return;
        }
        String fromName = args[0].trim();
        String toUser = args[2].trim();
        String msg = args[3].trim();
        String pmStr = fromName + ": " + msg;
        if (sendMap.get(toUser) != null) {
            TalkSend send = sendMap.get(toUser);
            send.send(pmStr);
        } else {
            sendServer.send(pktStr);
        }
    }

    private void close() {
        isRunning = false;
        talkReceive.close();
        serverTcp.close();
        sendServer.close();
    }

    class UdpProcessor implements Runnable {

        @Override
        public void run() {
            logger.info("UDP processor started successfully");
            while (isRunning) {
                DatagramPacket pkt = talkReceive.receivePkt();
                String msg = talkReceive.pktToString(pkt);
                rsp(pkt, msg);
            }
        }

        private void rsp(@NotNull DatagramPacket pkt, @NotNull String msg) {
            String[] split = msg.split("@");
            if (split.length < 2) {
                System.out.println(msg);
                return;
            }
            ChatType type = ChatType.typeToEnumMap.get(split[1].trim());
            if (type == null) {
                System.out.println(msg);
                return;
            }
            switch (type) {
                case PRIVATE_MSG:
                    newChannel(msg);
                    break;
                case FILE_TRANSFER:
                case EXIT:
                    break;
                default:
                    System.out.println(msg);
            }
        }
    }

    private void newChannel(@NotNull String pktStr) {
        // lele@-upm@chen@hello@chen@ip@rPort@sPort
        int validLen = 8;
        String[] split = pktStr.split("@");
        if (split.length != validLen) {
            System.out.println("Command error, please re-entered");
            return;
        }
        String from = split[0].trim();
        String toName = split[2].trim();
        String msg = split[3].trim();
        String ip = split[5].trim();
        String pmStr = from + ": " + msg;
        int rPort = Integer.parseInt(split[6]);
//        int sPort = Integer.parseInt(split[7]);
        TalkSend send = new TalkSend(sendServer.getSend(), ip, rPort);
        if (sendMap.size() >= MAX_SIZE) {
            logger.debug("Exceed MAX Size");
        } else {
            sendMap.put(toName, send);
            send.send(pmStr);
        }
    }

    private void putChannel(@NotNull String name, @NotNull TalkSend send) {
        if (sendMap.size() >= MAX_SIZE) {

        }
    }

    class TcpProcessor implements Runnable {

        @Override
        public void run() {
            logger.info("TCP processor started successfully");
            while (isRunning) {
                String msg = serverTcp.receive();
                if (msg == null) {
                    continue;
                }
                rsp(msg);
            }
        }

        private void rsp(@NotNull String msg) {
            String[] split = msg.split("@");
            ChatType type = ChatType.typeToEnumMap.get(split[0].trim());
            if (type == null) {
                System.out.println(msg);
                return;
            }
            switch (type) {
                case FILE_TRANSFER:
                case EXIT:
                    break;
                default:
                    System.out.println(msg);
            }
        }
    }
}
