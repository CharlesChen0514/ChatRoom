package org.bitkernel.common;

import com.alibaba.fastjson.JSONObject;
import com.sun.istack.internal.NotNull;

public class JsonUtil {
    @NotNull
    public static Data parseData(@NotNull String str) {
        JSONObject jsonObject = JSONObject.parseObject(str);
        User user = JSONObject.toJavaObject(jsonObject.getJSONObject("user"), User.class);
        User toUser = JSONObject.toJavaObject(jsonObject.getJSONObject("toUser"), User.class);
        int type = jsonObject.getIntValue("type");
        String msg = jsonObject.getString("msg");
        return new Data(type, user, toUser, msg);
    }
}
