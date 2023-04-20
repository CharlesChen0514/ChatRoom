package org.bitkernel.common;

import com.sun.istack.internal.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Data {
    @Getter
    private int type;
    @Getter
    private User user;
    @Getter
    private User toUser;
    @Getter
    private String msg;

    public Data(int type, @NotNull User user) {
        this.type = type;
        this.user = user;
    }

    public Data(@NotNull User user, @NotNull String msg) {
        this.user = user;
        this.msg = msg;
    }

    public Data(int type, @NotNull User user, @NotNull String msg) {
        this(type, user);
        this.msg = msg;
    }

    public Data() {
        type = -1;
    }
}
