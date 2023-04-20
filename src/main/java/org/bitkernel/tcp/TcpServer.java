package org.bitkernel.tcp;

import com.sun.istack.internal.NotNull;

import java.net.Socket;

public class TcpServer extends Tcp {
    public static final int PORT = 9091;

    public TcpServer(@NotNull Socket socket) {
        super(socket);
    }
}
