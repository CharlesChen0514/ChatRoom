package org.bitkernel.chatserver;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class User {
    @Getter
    private String name;
    @Getter
    private String passwd;

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("passwd", passwd);
        return json.toJSONString();
    }
}
