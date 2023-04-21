package org.bitkernel.common;

import com.sun.istack.internal.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class User {
    @Getter
    private String name;
    @Getter
    private String ip;
    @Getter
    private int receivePort;
    @Getter
    private int sendPort;

    @NotNull
    public String toString() {
        return name + "@" + ip + "@" + receivePort + "@" + sendPort;
    }

    public static User parse(@NotNull String str) {
        String[] split = str.split("@");
        String name = split[0].trim();
        String ip = split[1].trim();
        int receivePort = Integer.parseInt(split[2].trim());
        int sendPort = Integer.parseInt(split[3].trim());
        return new User(name, ip, receivePort, sendPort);
    }
}
