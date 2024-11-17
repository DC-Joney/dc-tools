package com.dc.tools.spring.utils;

import com.dc.tools.common.utils.StringUtils;
import com.dc.tools.spring.exception.JsonSerializeException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;

/**
 *
 * 用于对Json转换进行操作
 * @author zhangyang
 */
public class JsonMapper {
    private static final Logger logger = LoggerFactory.getLogger(JsonMapper.class);
    public static final JsonMapper INSTANCE = new JsonMapper();
    private final ObjectMapper mapper;

    public JsonMapper() {
        this((JsonInclude.Include) null);
    }

    public JsonMapper(JsonInclude.Include include) {
        this.mapper = new ObjectMapper();
        if (include != null) {
            this.mapper.setSerializationInclusion(include);
        }

        this.mapper.setSerializationInclusion(include)
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        this.mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    public static JsonMapper nonNullMapper() {
        return new JsonMapper(JsonInclude.Include.NON_NULL);
    }

    public static JsonMapper nonEmptyMapper() {
        return new JsonMapper(JsonInclude.Include.NON_EMPTY);
    }

    public static JsonMapper defaultMapper() {
        return new JsonMapper();
    }

    public String toJson(Object object) {
        try {
            return this.mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializeException("Format json error, cause is:", e);
        }
    }

    public byte[] toJsonBytes(Object object) {
        try {
            return this.mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new JsonSerializeException("Format json error, cause is:", e);
        }
    }

    public <T> T fromJson(String jsonString, Class<T> clazz)  {
        try {
            if (StringUtils.isEmpty(jsonString)) {
                return null;
            } else {
                return this.mapper.readValue(jsonString, clazz);
            }
        } catch (IOException e) {
            throw new JsonSerializeException("Parse json error, cause is:", e);
        }
    }


    public <T> T fromJson(String jsonString, TypeReference<T> typeReference) throws IOException {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        } else {
            return this.mapper.readValue(jsonString, typeReference);
        }
    }

    public <T> T fromJson(String jsonString, JavaType javaType) throws IOException {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        } else {
            return this.mapper.readValue(jsonString, javaType);
        }
    }

    public JavaType buildCollectionType(Class<? extends Collection<?>> collectionClass, Class<?> elementClass) {
        return this.mapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    public JavaType buildMapType(Class<? extends Map<?, ?>> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return this.mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
    }

    public void update(String jsonString, Object object) {
        try {
            this.mapper.readerForUpdating(object).readValue(jsonString);
        } catch (IOException var4) {
            logger.warn("update json string:" + jsonString + " to object:" + object + " error.", var4);
        }
    }

    public String toJsonP(String functionName, Object object) throws JsonProcessingException {
        return this.toJson(new JSONPObject(functionName, object));
    }

    public void enableEnumUseToString() {
        this.mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        this.mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    public ObjectMapper getMapper() {
        return this.mapper;
    }
}
