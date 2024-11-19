package com.dc.tools.spring.log;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.dc.tools.spring.log.json.JsonPropertyLengthFilter;

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
