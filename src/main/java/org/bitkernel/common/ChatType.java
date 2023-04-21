package org.bitkernel.common;

import com.sun.istack.internal.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public enum ChatType {
    ONLINE_USERS("-ol", "online users", "-ol"),
    PRIVATE_MSG("-pm", "private message", "-pm@chen@hello"),
    FILE_TRANSFER("-f", "file transfer", "-f@chen@file"),
    ACCEPTED_FILES("-af", "accepted file list", "-af"),
    EXIT("-q", "exit", "-q"),
    START_LISTEN("-sl", "start listen", "-sl"),
    SOUT("-sout", "output", "-sout@content"),
    HELP("-h", "command prompt", "-h"),
    NEW_CHANNEL("-nc", "New channel", "-nc@content");
    public final String cmd;
    public final String description;
    public final String example;
    public final static String sym = "@";

    @NotNull
    public String toString() {
        return String.format("%s, %s, %s", cmd, description, example);
    }

    public static final Map<String, ChatType> typeToEnumMap;
    public static final Set<ChatType> menu;

    static {
        typeToEnumMap = new LinkedHashMap<>();
        for (ChatType chatType : ChatType.values()) {
            typeToEnumMap.put(chatType.cmd, chatType);
        }

        menu = new LinkedHashSet<>();
        menu.add(ONLINE_USERS);
        menu.add(PRIVATE_MSG);
        menu.add(FILE_TRANSFER);
        menu.add(ACCEPTED_FILES);
        menu.add(HELP);
        menu.add(EXIT);
    }

    ChatType(@NotNull String cmd, @NotNull String des,
             @NotNull String example) {
        this.cmd = cmd;
        this.description = des;
        this.example = example;
    }
}
