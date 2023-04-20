package org.bitkernel.server;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.common.ChatType;
import org.bitkernel.common.Data;
import org.bitkernel.common.JsonUtil;
import org.bitkernel.common.User;
import org.bitkernel.tcp.Tcp;
import org.bitkernel.udp.UdpServer;

import java.io.IOException;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.bitkernel.common.ChatType.*;

@Slf4j
public class Server {
    public static final int TCP_LISTEN_PORT = 9091;
    private static final int MAX_THREAD = 1000;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD);
    private ServerSocket serverSocket;
    private final Map<User, Tcp> tcpConnMap = new LinkedHashMap<>();
    private final Map<User, SocketAddress> udpSocAddrMap = new LinkedHashMap<>();
    private final Map<User, String> udpAddrMap = new LinkedHashMap<>();

    private final UdpServer udpServer = new UdpServer();
    public static final InetAddress ip;

    public static final User sys;
    private boolean isRunning = true;

    static {
        try {
            sys = new User("Server", "Server");
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public Server() {
        try {
            serverSocket = new ServerSocket(TCP_LISTEN_PORT);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        logger.info("--------------START----------------");
        Server server = new Server();
        server.start();
    }

    @NotNull
    private String collectTcpOnlineUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total %d online users: ", tcpConnMap.size()));
        tcpConnMap.keySet().forEach(u -> sb.append(u.getName()).append(", "));
        return sb.toString();
    }

    @NotNull
    private String collectUdpOnlineUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total %d online users: ", udpSocAddrMap.size()));
        udpSocAddrMap.keySet().forEach(n -> sb.append(n).append(", "));
        return sb.toString();
    }

    class TcpListener implements Runnable {
        @Override
        public void run() {
            logger.info("Tcp listener started successfully");
            try {
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    Tcp tcpServer = new Tcp(socket);
                    String string = tcpServer.getBr().readLine();
                    if (string == null) {
                        socket.close();
                        logger.info("{} test tcp server port", socket.getRemoteSocketAddress());
                        continue;
                    }
                    logger.debug("TCP listener receive message: {}", string);
                    User user = JSONObject.parseObject(string, User.class);
                    tcpConnMap.put(user, tcpServer);
                    logger.info("New tcp client connection: {}{}", user, socket.getRemoteSocketAddress());
                    tcpBroadcast("server", user.getName() + " online");
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    class UdpListener implements Runnable {
        @Override
        public void run() {
            logger.info("UDP listener started successfully");
            while (isRunning) {
                try {
                    DatagramPacket pkt = udpServer.receivePacket();
                    Data data = JsonUtil.parseData(new String(pkt.getData()));
                    response(pkt, data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void start() {
        Thread t1 = new Thread(new TcpListener());
        Thread t2 = new Thread(new UdpListener());
        t1.start();
        t2.start();
    }

    public synchronized void tcpBroadcast(@NotNull String source,
                                          @NotNull String msg) {
        String str = source + ":" + msg;
        tcpConnMap.values().forEach(tcp -> tcp.getPw().println(str));
    }

    public synchronized void udpBroadcast(@NotNull User user,
                                          @NotNull String msg) {
        String str = user.getName() + ":" + msg;
        Data data = new Data(user, str);
        String json = JSONObject.toJSONString(data);
        udpSocAddrMap.values().forEach(addr -> udpServer.send(addr, json));
    }

    private void response(@NotNull DatagramPacket pkt,
                          @NotNull Data data) {
        ChatType type = typeToEnumMap.get(data.getType());
        Tcp tcpServer = tcpConnMap.get(data.getUser());
//        SocketAddress udpAddr = udpAddressMap.get(data.getUser().getName());
//        System.out.println(type);
        SocketAddress pktAddr = pkt.getSocketAddress();
        switch (type) {
            case HEART_BEAT:
                udpServer.heartBeating(pktAddr);
                break;
            case LOGIN:
                String addrStr = pkt.getAddress().getHostAddress() + ":" + pkt.getPort();
                Data loginData = new Data(NEW_USER.type, data.getUser(), addrStr);

                udpSocAddrMap.values().forEach(addr -> udpServer.send(addr, JSONObject.toJSONString(loginData)));

                udpAddrMap.forEach((u, loc) -> {
                    Data d = new Data(NEW_USER.type, u, loc);
                    udpServer.send(pktAddr, JSONObject.toJSONString(d));
                });
                udpSocAddrMap.put(data.getUser(), pktAddr);
                udpAddrMap.put(data.getUser(), addrStr);
                logger.debug("New udp client registered: {}{}", data.getUser().getName(), pktAddr);
                break;
            case UDP_ONLINE_USERS:
                String udpUsers = collectUdpOnlineUsers();
                Data rsp = new Data(UDP_ONLINE_USERS.type, sys, udpUsers);
                udpServer.send(pktAddr, JSONObject.toJSONString(rsp));
                logger.info(udpUsers);
                break;
            case TCP_ONLINE_USERS:
                String tcpUser = collectTcpOnlineUsers();
                tcpServer.send(tcpUser);
                logger.info(tcpUser);
                break;
            case UDP_PRIVATE_MSG:
            case TCP_PRIVATE_MSG:
            case UDP_BROADCAST:
                udpBroadcast(data.getUser(), data.getMsg());
                logger.debug("{} broadcast message {}", data.getUser().getName(), data.getMsg());
                break;
            case TCP_BROADCAST:
                break;
            case FILE_TRANSFER:
            case EXIT:
                close(data.getUser());
                break;
            default:
                System.out.println("Invalid selection, please re-enter");
        }
    }

    private void close(@NotNull User user) {
        Tcp tcp = tcpConnMap.get(user);
        tcp.close();
        tcpConnMap.remove(user);
        udpSocAddrMap.remove(user);
        String msg = user.getName() + " offline";
        tcpBroadcast("server", msg);
        logger.info(msg);
    }
}
