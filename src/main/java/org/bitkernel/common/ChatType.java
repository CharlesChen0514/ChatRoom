package org.bitkernel.common;

import com.sun.istack.internal.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public enum ChatType {
    HEART_BEAT(-1, "heart beat detection"),
    LOGIN(0, "login"),
    UDP_ONLINE_USERS(1, "udp online users"),
    TCP_ONLINE_USERS(2, "tcp online users"),
    UDP_PRIVATE_MSG(3, "udp private message"),
    TCP_PRIVATE_MSG(4, "tcp private message"),
    UDP_BROADCAST(5, "udp broadcast"),
    TCP_BROADCAST(6, "tcp broadcast"),
    FILE_TRANSFER(7, "file transfer"),
    EXIT(8, "exit"),
    NEW_USER(9, "");
    public final int type;
    public final String description;

    @NotNull
    public String toString() {
        return String.format("%d) %s", type, description);
    }

    public static final Map<Integer, ChatType> typeToEnumMap;
    public static final Set<ChatType> menu;

    static {
        typeToEnumMap = new LinkedHashMap<>();
        for (ChatType chatType : ChatType.values()) {
            typeToEnumMap.put(chatType.type, chatType);
        }

        menu = new LinkedHashSet<>();
        menu.add(UDP_ONLINE_USERS);
        menu.add(TCP_ONLINE_USERS);
        menu.add(UDP_PRIVATE_MSG);
        menu.add(TCP_PRIVATE_MSG);
        menu.add(UDP_BROADCAST);
        menu.add(TCP_BROADCAST);
        menu.add(FILE_TRANSFER);
        menu.add(EXIT);
    }

    ChatType(int type, @NotNull String des) {
        this.type = type;
        this.description = des;
    }
}
