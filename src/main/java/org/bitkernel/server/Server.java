package org.bitkernel.server;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.ChatType;
import org.bitkernel.common.User;
import org.bitkernel.tcp.Tcp;
import org.bitkernel.udp.TalkReceive;
import org.bitkernel.udp.TalkSend;

import java.io.IOException;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.bitkernel.common.User.parse;

@Slf4j
public class Server {
    public static final int TCP_LISTEN_PORT = 9091;
    public static final int UDP_RECEIVE_PORT = 9090;
    public static final int UDP_SEND_PORT = 9092;
    private ServerSocket serverSocket;
    private TalkReceive talkReceive;
    private final Map<String, Tcp> tcpConnMap = new LinkedHashMap<>();
    private final Map<String, User> userMap = new LinkedHashMap<>();
    public static final String ip;
    private boolean isRunning = true;

    static {
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public Server() {
        try {
            serverSocket = new ServerSocket(TCP_LISTEN_PORT);
            talkReceive = new TalkReceive(UDP_RECEIVE_PORT);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        logger.info("--------------START----------------");
        Server server = new Server();
        server.start();
    }

    class TcpListener implements Runnable {
        @Override
        public void run() {
            logger.info("Tcp listener started successfully");
            try {
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    Tcp tcpServer = new Tcp(socket);
                    String str = tcpServer.getBr().readLine();
                    User user = parse(str);
                    logger.debug("TCP receive message: {}", str);
                    tcpConnMap.put(user.getName(), tcpServer);
                    userMap.put(user.getName(), user);
                    logger.info("New tcp client connection: {}", str);
                    tcpBroadcast("server", user.getName() + " online");
                }
                serverSocket.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
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
            talkReceive.close();
        }
    }

    private void rsp(@NotNull DatagramPacket pkt, @NotNull String msg) {
        String[] split = msg.split("@");
        ChatType type = ChatType.typeToEnumMap.get(split[1].trim());
        User user = userMap.get(split[0].trim());
        String info = String.format("%s:%s -> %s", pkt.getAddress().getHostAddress(),
                pkt.getPort(), msg);
        logger.info(info);
        switch (type) {
            case ONLINE_USERS:
                rspOnlineUsers(pkt, user);
                break;
            case PRIVATE_MSG:
                rspPmReq(pkt, user, msg);
                break;
            case FILE_TRANSFER:
            case EXIT:
                removeUser(user);
                break;
            default:
                System.out.println("Invalid selection, please re-enter");
        }
    }

    private void rspPmReq(@NotNull DatagramPacket pkt, @NotNull User user,
                          @NotNull String msg) {
        // lele@-upm@chen@hello
        String[] split = msg.split("@");
        String toName = split[2].trim();
        User toUser = userMap.get(toName);
        String str = msg + "@" + toUser.toString();
        logger.debug(str);
        rspMsg(pkt, user, str);
    }

    private void rspMsg(@NotNull DatagramPacket pkt, @NotNull User user,
                        @NotNull String str) {
        TalkSend send = new TalkSend(UDP_SEND_PORT, pkt.getAddress().getHostAddress(), user.getReceivePort());
        send.send(str);
        send.close();
    }

    private void removeUser(@NotNull User user) {
        String name = user.getName();
        tcpConnMap.get(name).close();
        tcpConnMap.remove(name);
        userMap.remove(name);
        tcpBroadcast("server", user.getName() + " offline");
    }

    private void rspOnlineUsers(@NotNull DatagramPacket pkt, @NotNull User user) {
        String str = collectOnlineUsers();
        rspMsg(pkt, user, str);
    }

    @NotNull
    private String collectOnlineUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total %d online users: ", userMap.size()));
        userMap.keySet().forEach(u -> sb.append(u).append(" "));
        return sb.toString();
    }

    public void start() {
        Thread t1 = new Thread(new TcpListener());
        Thread t2 = new Thread(new UdpProcessor());
        t1.start();
        t2.start();
    }

    public synchronized void tcpBroadcast(@NotNull String source,
                                          @NotNull String msg) {
        String str = source + ":" + msg;
        tcpConnMap.values().forEach(tcp -> tcp.getPw().println(str));
        logger.info("Broadcast {}", str);
    }
}
