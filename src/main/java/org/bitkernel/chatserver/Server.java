package org.bitkernel.chatserver;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        private Socket socket;
        private User user;
        private PrintWriter pw;
        private BufferedReader br;


        public ClientHandler(@NotNull Socket socket) {
            try {
                this.socket = socket;
                OutputStream out = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                pw = new PrintWriter(osw, true);
                outStreamSet.add(pw);

                InputStream in = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
                br = new BufferedReader(isr);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        private void login() throws IOException {
            String name = br.readLine();
            String passwd = br.readLine();
            user = new User(name, passwd);
            logger.info("User [{}{}] is online",
                    user.getName(), socket.getInetAddress());
            broadcast(String.format("%s is online", user.getName()));
        }

        @Override
        public void run() {
            try {
                login();
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                outStreamSet.remove(pw);
                broadcast(String.format("%s is offline", user.getName()));
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }
}
