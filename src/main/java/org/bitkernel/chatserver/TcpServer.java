package org.bitkernel.chatserver;

import com.sun.istack.internal.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
public class TcpServer {
    @Getter
    private Socket socket;
    @Getter
    private PrintWriter pw;
    @Getter
    private BufferedReader br;

    public TcpServer(@NotNull Socket socket) {
        try {
            this.socket = socket;
            OutputStream out = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            pw = new PrintWriter(osw, true);

            InputStream in = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
