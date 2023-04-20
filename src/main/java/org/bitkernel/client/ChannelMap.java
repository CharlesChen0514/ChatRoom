package org.bitkernel.client;

import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChannelMap {
    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    private static int MAX_SIZE = 100;

    public Channel get(@NotNull String user) {
        return channelMap.get(user);
    }

    public void insert(@NotNull String user, @NotNull Channel channel) {
        if (channelMap.size() >= MAX_SIZE) {
            logger.debug("exceed max size");
        } else {
            channelMap.put(user, channel);
        }
    }
}
