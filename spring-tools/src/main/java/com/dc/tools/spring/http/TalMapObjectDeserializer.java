package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.MapDeserializer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.MapSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;

import java.io.IOException;
import java.lang.reflect.Type;

@Deprecated
public class TalMapObjectDeserializer implements ObjectDeserializer, ObjectSerializer {

    public static final String ORIGIN = "origin:tal";

    private static final TalMapObjectDeserializer INSTANCE = new TalMapObjectDeserializer();

    private static final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(()-> false);

    private static final ParserConfig talParserConfig;
    static {
        talParserConfig = new ParserConfig();
        talParserConfig.putDeserializer(JSONObject.class, INSTANCE);
    }


    private final MapDeserializer deserializer = MapDeserializer.instance;
    private final MapSerializer mapSerializer = MapSerializer.instance;



    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        T instance = deserializer.deserialze(parser, type, fieldName);
        if (instance instanceof JSONObject) {
            ((JSONObject) instance).put(ORIGIN, ORIGIN);
        }

        return instance;
    }

    @Override
    public int getFastMatchToken() {
        return deserializer.getFastMatchToken();
    }

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        boolean talOrigin = false;
        try {
            if (object instanceof JSONObject && ((JSONObject) object).containsKey(ORIGIN)) {
                talOrigin = true;
                threadLocal.set(true);
            }
            mapSerializer.write(serializer, object, fieldName, fieldType, features);
        } finally {
            if (talOrigin) {
                ((JSONObject) object).remove(ORIGIN);
                threadLocal.set(false);
            }
        }

    }

    public static boolean isOrigin(){
        return threadLocal.get();
    }

    public static TalMapObjectDeserializer getInstance() {
        return INSTANCE;
    }

    public static ParserConfig getTalParserConfig() {
        return talParserConfig;
    }


}