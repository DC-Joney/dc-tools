package com.dc.tools.common.utils;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用于传递过滤器上下文信息
 * @author zy
 */
public class WriteContext {

    private final Map<Object, Object> context = new ConcurrentHashMap<>();


    public WriteContext put(String key, Object value){
        context.put(key, value);
        return this;
    }

    public WriteContext putAll(ContextView ctx){
        Map<Object, Object> contextMap = ctx.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return putAll(contextMap);
    }

    public WriteContext putAll(Context context){
        Map<Object, Object> contextMap = context.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return putAll(contextMap);
    }

    public WriteContext putAll(Map<Object, Object> another){
        context.putAll(another);
        return this;
    }

    public WriteContext putAll(WriteContext writeContext){
        if (writeContext != null) {
            context.putAll(writeContext.context);
        }
        return this;
    }


    public <V> V get(Object key, Class<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public <V> V get(Object key, TypeReference<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public <V> V getOrDefault(Object key, V defaultValue, Class<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }

    public <V> V getOrDefault(Object key, V defaultValue, TypeReference<V> valueClass) {
        return MapUtil.get(context, key, valueClass);
    }
}
