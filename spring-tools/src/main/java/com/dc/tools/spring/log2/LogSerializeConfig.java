package com.dc.tools.spring.log2;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;

import java.util.Map;

public class LogSerializeConfig {

    public static final SerializeConfig serializeConfig = new SerializeConfig();
    static {
        serializeConfig.addFilter(Map.class, JsonPropertyLengthFilter.getInstance());
        serializeConfig.addFilter(JSONObject.class, JsonPropertyLengthFilter.getInstance());
    }

    public static SerializeConfig getSerializeConfig() {
        return serializeConfig;
    }
}
