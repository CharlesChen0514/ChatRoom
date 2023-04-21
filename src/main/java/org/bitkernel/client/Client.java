package org.bitkernel.client;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.ChatType;
import org.bitkernel.common.User;
import org.bitkernel.server.Server;
import org.bitkernel.tcp.Tcp;
import org.bitkernel.udp.TalkReceive;
import org.bitkernel.udp.TalkSend;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static org.bitkernel.common.ChatType.*;
import static org.bitkernel.common.FileUtil.*;
import static org.bitkernel.tcp.Tcp.testServer;

@Slf4j
public class Client {
    private final Scanner sc = new Scanner(System.in);
    private Tcp serverTcp;
    private TalkReceive talkReceive;
    private TalkSend serverUdp;
    private final ConcurrentHashMap<String, TalkSend> sendMap = new ConcurrentHashMap<>();
    private User user;
    private boolean isRunning = true;
    private static final int MAX_SIZE = 100;
    private String fileDir;
    private final Map<String, User> userMap = new LinkedHashMap<>();

    public static void main(String[] args) {
        if (!testServer()) {
            System.exit(-1);
        }
        logger.debug("Start chat room client");
        Client c = new Client();
        c.login();
        c.chatGuide();
    }

    private void inputUserMsg() {
        logger.debug("Start input msg");
        System.out.println("Welcome to chat room, please login");
        System.out.print("Input username: ");
        String name = sc.next();

        while (true) {
            System.out.print("UDP send port: ");
            int sendPort = sc.nextInt();
            System.out.print("UDP receiver port: ");
            int receivePort = sc.nextInt();
            System.out.print("Tcp listener port: ");
            int listenerPort = sc.nextInt();
            boolean valid = checkIpPort(Server.ip, sendPort) && checkIpPort(Server.ip, receivePort)
                    && checkIpPort(Server.ip, listenerPort);
            if (!valid) {
                System.out.println("Input port unavailable, please re-entered");
                continue;
            }
            user = new User(name, Server.ip, receivePort, sendPort, listenerPort);
            fileDir = System.getProperty("user.dir") + File.separator + "file" +
                    File.separator + user.getName() + File.separator;
            createFolder(fileDir);
            logger.debug("Create user: {}", user.toString());
            break;
        }
    }

    public static boolean checkIpPort(@NotNull String ip, int port) {
        return checkTcpPort(ip, port) && checkUdpPort(port);
    }

    public static boolean checkTcpPort(@NotNull String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.bind(new InetSocketAddress(ip, port));
            logger.debug("Tcp {}:{} is available", ip, port);
            return true;
        } catch (Exception e) {
            logger.debug("Tcp {}:{} is unavailable", ip, port);
            return false;
        }
    }

    public static boolean checkUdpPort(int port) {
        try  (DatagramSocket receive = new DatagramSocket(port)){
            logger.debug("Udp {} is available", port);
            return true;
        } catch (Exception e) {
            logger.debug("Udp {} is unavailable", port);
            return false;
        }
    }

    private void login() {
        inputUserMsg();
        startLocalServer();
    }

    private void startLocalServer() {
        try {
            serverTcp = new Tcp(new Socket(Server.ip, Server.TCP_LISTEN_PORT));
            logger.debug("Successfully connected to TCP server: {}:{}",
                    Server.ip, Server.TCP_LISTEN_PORT);
            serverTcp.send(user.toString());
            serverUdp = new TalkSend(user.getSendPort(), Server.ip, Server.UDP_RECEIVE_PORT);
            talkReceive = new TalkReceive(user.getReceivePort());

            Thread t1 = new Thread(new TcpProcessor());
            Thread t2 = new Thread(new UdpProcessor());
            t1.start();
            t2.start();
        } catch (IOException e) {
            logger.error("Start local server error {}", e.getMessage());
            System.exit(-1);
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
            String[] args = cmdLine.split(sym);
            ChatType type = ChatType.typeToEnumMap.get(args[0].trim());
            if (type == null) {
                System.out.println("Command error, please re-entered");
                continue;
            }
            String pktStr = user.getName() + sym + cmdLine;
            switch (type) {
                case ONLINE_USERS:
                    serverUdp.send(pktStr);
                    break;
                case PRIVATE_MSG:
                    pm(pktStr);
                    break;
                case FILE_TRANSFER:
                    fileTransferReq(pktStr);
                    break;
                case ACCEPTED_FILES:
                    String out = "Accepted file: " + getAllFileNameString(fileDir);
                    System.out.println(addTime(out));
                    break;
                case EXIT:
                    serverUdp.send(pktStr);
                    String goodbye = String.format("Goodbye %s ~%n", user.getName());
                    System.out.printf(addTime(goodbye));
                    System.exit(-1);
                    break;
                case HELP:
                    menu.forEach(System.out::println);
                    break;
                default:
                    System.out.println("Invalid selection, please re-enter");
            }
        }
        logger.info("Exit chat menu");
    }

    private void fileTransferReq(@NotNull String pktStr) {
        // from@-f@chen@file
        String[] split = pktStr.split(sym);
        if (split.length != 4) {
            System.out.println("Command error, please re-entered");
            return;
        }
        String from = split[0].trim();
        String to = split[2].trim();
        if (from.equals(to)) {
            System.out.println("Cannot transfer file to yourself, please re-entered");
            return;
        }
        String file = split[3].trim();
        if (!exist(file) && !existInFolder(fileDir, file)) {
            System.out.printf("Invalid file format %s, try again%n", file);
            return;
        }
        serverUdp.send(pktStr);
    }

    private void pm(@NotNull String pktStr) {
        String[] args = pktStr.split(sym);
        if (args.length != 4) {
            System.out.println("Command error, please re-entered");
            return;
        }
        String fromName = args[0].trim();
        String toUser = args[2].trim();
        String msg = args[3].trim();
        String pmStr = fromName + sym +
                PRIVATE_MSG.cmd + sym +
                toUser + sym + msg;
        if (sendMap.get(toUser) != null) {
            TalkSend send = sendMap.get(toUser);
            send.send(pmStr);
            String out = addTime(String.format("To %s: %s%n", toUser, msg));
            System.out.printf(out);
        } else {
            String newChannelCmd = fromName + sym +
                    NEW_CHANNEL.cmd + sym +
                    toUser + sym + msg;
            logger.debug(newChannelCmd);
            serverUdp.send(newChannelCmd);
        }
    }

    private void close() {
        isRunning = false;
        talkReceive.close();
        serverTcp.close();
        serverUdp.close();
    }

    class UdpProcessor implements Runnable {

        @Override
        public void run() {
            logger.info("UDP processor started successfully");
            while (isRunning) {
                DatagramPacket pkt = talkReceive.receivePkt();
                String msg = talkReceive.pktToString(pkt);
                rsp(msg);
            }
        }

        private void rsp(@NotNull String msg) {
            String[] split = msg.split(sym);
            System.out.println(msg);
            ChatType type = ChatType.typeToEnumMap.get(split[1].trim());
            if (type == null) {
                System.out.println(msg);
                return;
            }
            switch (type) {
                case PRIVATE_MSG:
                    String str = split[0].trim() + ": " + split[3].trim();
                    System.out.println(addTime(str));
                    break;
                case NEW_CHANNEL:
                    newChannel(msg);
                    break;
                case FILE_TRANSFER:
                    transferFile(msg);
                    break;
                case START_LISTEN:
                    Tcp tcp = new Tcp(user.getListenPort());
                    tcp.acceptFile(split[0].trim(), fileDir);
                    break;
                case SOUT:
                    System.out.println(addTime(split[0].trim() + ": " + split[2].trim()));
                case EXIT:
                    break;
                default:
                    System.out.println(msg);
            }
        }
    }

    private void transferFile(@NotNull String msg) {
        // from@-f@chen@file@chen@ip@rPort@sPort@tcpPort
        String[] split = msg.split(sym);
        String from = split[0].trim();
        String toName = split[2].trim();
        String file = split[3].trim();
        String ip = split[5].trim();
        int rPort = Integer.parseInt(split[6].trim());
        int tPort = Integer.parseInt(split[8].trim());

        String filePath;
        if (existInFolder(fileDir, file)) {
            filePath = fileDir + file;
        } else {
            filePath = file;
        }
        TalkSend send = getOrNewChannel(toName, ip, rPort);
        String cmd = from + sym + "-sl";
        send.send(cmd);

        Tcp tcp = new Tcp();
        tcp.pushFile(toName, filePath, tPort);
    }

    @NotNull
    private static String addTime(@NotNull String str) {
        return getTime() + ": " + str;
    }

    @NotNull
    public static String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("HH:mm:ss:SSS");
        Date date = new Date();
        return sdf.format(date);
    }

    private void newChannel(@NotNull String pktStr) {
        // lele@-nc@chen@hello@chen@ip@rPort@sPort@tcpPort
        int validLen = 9;
        String[] split = pktStr.split(sym);
        if (split.length != validLen) {
            System.out.println("Command error, please re-entered");
            return;
        }
        String from = split[0].trim();
        String toName = split[2].trim();
        String msg = split[3].trim();
        String ip = split[5].trim();
        String pmCmd = from + sym + PRIVATE_MSG.cmd + sym + toName + sym + msg;
        int rPort = Integer.parseInt(split[6]);
        int sPort = Integer.parseInt(split[7]);
        int tPort = Integer.parseInt(split[8]);
        User toUser = new User(toName, ip, rPort, sPort, tPort);
        userMap.put(toName, toUser);
        TalkSend send = newChannel(toName, ip, rPort);
        send.send(pmCmd);
        String out = addTime(String.format("To %s: %s%n", toName, msg));
        System.out.printf(out);
    }

    @NotNull
    private TalkSend newChannel(@NotNull String toName, @NotNull String ip, int rPort) {
        TalkSend send = new TalkSend(serverUdp.getSend(), ip, rPort);
        if (sendMap.size() >= MAX_SIZE) {
            logger.debug("Exceed MAX Size");
        } else {
            sendMap.put(toName, send);
        }
        return send;
    }

    @NotNull
    private TalkSend getOrNewChannel(@NotNull String toName, @NotNull String ip, int rPort) {
        if (sendMap.get(toName) == null) {
            return newChannel(toName, ip, rPort);
        }
        return sendMap.get(toName);
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
            if (msg.equals("Heart")) {
                serverTcp.getPw().println("Alive");
                return;
            }
            String[] split = msg.split(sym);
            ChatType type = ChatType.typeToEnumMap.get(split[0].trim());
            if (type == null) {
                System.out.println(addTime(msg));
                return;
            }
            switch (type) {
                case FILE_TRANSFER:
                case EXIT:
                    break;
                default:
                    System.out.println(addTime(msg));
            }
        }
    }
}
