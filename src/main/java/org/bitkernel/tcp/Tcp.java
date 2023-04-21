package org.bitkernel.tcp;

import com.sun.istack.internal.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitkernel.server.Server;
import org.springframework.util.StopWatch;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.bitkernel.client.Client.getTime;

@Slf4j
@NoArgsConstructor
public class Tcp {
    @Getter
    protected Socket socket;
    @Getter
    private BufferedReader br;
    @Getter
    private PrintWriter pw;
    private ServerSocket serverSocket;

    public Tcp(@NotNull Socket socket) {
        this.socket = socket;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public Tcp(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static boolean testServer() {
        try (Socket ignored = new Socket(Server.ip, Server.TCP_LISTEN_PORT)) {
            return true;
        } catch (IOException e) {
            logger.error("Server is unavailable, please start the server.");
            System.out.println("Server is unavailable, please start the server.");
            return false;
        }
    }

    public void acceptFile(@NotNull String from,
                           @NotNull String pos) {
        class DownLoadFile implements Runnable {

            @Override
            public void run() {
                try {
                    StopWatch watch = new StopWatch();
                    String startTime = getTime();
                    watch.start();
                    Socket socket = serverSocket.accept();
                    DataInputStream is = new DataInputStream(socket.getInputStream());
                    String fileName = is.readUTF();
                    String outputPath = pos + fileName;
                    FileOutputStream fos = new FileOutputStream(outputPath);
                    byte[] b = new byte[1024];
                    long total = 0;
                    int length;
                    while ((length = is.read(b)) != -1) {
                        total += length;
                        fos.write(b, 0, length);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    socket.close();
                    watch.stop();
                    String endTime = getTime();
                    serverSocket.close();
                    long ms = watch.getTotalTimeMillis();
                    System.out.printf("Successfully accept file from [%s]%n", from);
                    System.out.printf("File name [%s], file size [%s bytes], store in [%s]%n",
                            fileName, total, outputPath);
                    System.out.printf("Start time [%s], end time [%s], total time [%d]%n", startTime, endTime, ms);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        Thread t1 = new Thread(new DownLoadFile());
        t1.start();
    }

    public void pushFile(@NotNull String toName, @NotNull String filePath,
                         @NotNull int port) {
        class UpLoadFile implements Runnable {
            @Override
            public void run() {
                try {
                    StopWatch watch = new StopWatch();
                    String startTime = getTime();
                    watch.start();
                    File file = new File(filePath);
                    socket = new Socket(Server.ip, port);

                    DataInputStream fis = new DataInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(filePath))));
                    DataOutputStream ps = new DataOutputStream(socket.getOutputStream());
                    ps.writeUTF(file.getName());
                    ps.flush();

                    int bufferSize = 8192;
                    byte[] buf = new byte[bufferSize];
                    while (true) {
                        int read = fis.read(buf);
                        if (read == -1) {
                            break;
                        }
                        ps.write(buf, 0, read);
                    }
                    ps.flush();
                    fis.close();
                    socket.close();
                    watch.stop();
                    String endTime = getTime();
                    long ms = watch.getTotalTimeMillis();
                    System.out.printf("Successfully transfer file to [%s]%n", toName);
                    System.out.printf("File name [%s], file size [%s bytes]%n", file.getName(), file.length());
                    System.out.printf("Start time [%s], end time [%s], total time [%d]%n", startTime, endTime, ms);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Thread t1 = new Thread(new UpLoadFile());
        t1.start();
    }

    public void send(@NotNull String msg) {
        logger.debug("Tcp send message {} to {}", msg, socket.getRemoteSocketAddress());
        pw.println(msg);
    }

    public void close() {
        try {
            br.close();
            pw.close();
            socket.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @NotNull
    public String receive() {
        try {
            String msg = br.readLine();
            if (msg != null) {
                return msg;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
