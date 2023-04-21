package org.bitkernel.common;

import com.sun.istack.internal.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.bitkernel.common.ChatType.sym;

@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class User {
    @Getter
    private String name;
    @Getter
    private String ip;
    @Getter
    private int receivePort;
    @Getter
    private int sendPort;
    @Getter
    private int listenPort;

    @NotNull
    public String toString() {
        return name + sym
                + ip + sym
                + receivePort + sym
                + sendPort + sym
                + listenPort;
    }

    @NotNull
    public static User parse(@NotNull String str) {
        String[] split = str.split(sym);
        if (split.length != 5) {
            logger.error("Error user string format: {}", str);
            return new User();
        }
        String name = split[0].trim();
        String ip = split[1].trim();
        int receivePort = Integer.parseInt(split[2].trim());
        int sendPort = Integer.parseInt(split[3].trim());
        int listenPort = Integer.parseInt(split[4].trim());
        return new User(name, ip, receivePort, sendPort, listenPort);
    }
}
