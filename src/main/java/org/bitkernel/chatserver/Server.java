package org.bitkernel.chatserver;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Server {
    private static final int TCP_PORT = 9091;
    private static final int UDP_PORT = 9090;
    private static final int MAX_THREAD = 1000;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private final Set<PrintWriter> outStreamSet = new LinkedHashSet<>();

    public Server() {
        threadPool = Executors.newFixedThreadPool(MAX_THREAD);
        try {
            serverSocket = new ServerSocket(TCP_PORT);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    public void start() {
        try {
            logger.info("Server started successfully");
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("A new client connect {}", socket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public synchronized void broadcast(@NotNull String msg) {
        outStreamSet.forEach(out -> out.println(msg));
    }

    class ClientHandler implements Runnable {
        private User user;
        private TcpServer tcpServer;

        public ClientHandler(@NotNull Socket socket) {
            tcpServer = new TcpServer(socket);
            outStreamSet.add(tcpServer.getPw());
        }

        private void login() throws IOException {
            String name = tcpServer.getBr().readLine();
            String passwd = tcpServer.getBr().readLine();
            user = new User(name, passwd);
            logger.info("User [{}{}] is online", user.getName(),
                    tcpServer.getSocket().getInetAddress());
            broadcast(String.format("%s is online", user.getName()));
        }

        private void close() {
            broadcast(String.format("%s is offline", user.getName()));
            outStreamSet.remove(tcpServer.getPw());
            try {
                tcpServer.getSocket().close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                login();

            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                close();
            }
        }
    }
}
