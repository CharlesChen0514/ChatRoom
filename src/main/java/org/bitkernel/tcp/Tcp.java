package org.bitkernel.tcp;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Slf4j
public class Tcp {
    @Getter
    protected Socket socket;
    @Getter
    private BufferedReader br;
    @Getter
    private PrintWriter pw;

    public Tcp(@NotNull Socket socket) {
        this.socket = socket;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void startReceiver() {
        Thread t1 = new Thread(new Receiver());
        t1.start();
        logger.info("Start tcp receiver");
    }

    public void send(@NotNull String msg) {
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

    class Receiver implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String msg = br.readLine();
                    if (msg != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

//    class Writer implements Runnable {
//        @Override
//        public void run() {
//            try  {
//
//            } catch (IOException e) {
//                logger.error(e.getMessage());
//            }
//        }
//    }
}
