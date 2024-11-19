package com.dc.tools.spring.log;

import com.alibaba.fastjson.serializer.BeanContext;
import com.alibaba.fastjson.serializer.ContextValueFilter;
import com.dc.tools.spring.log.annotation.LogIgnore;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于在Log2序列化时去掉部分无用字段
 *
 * @author zy
 */
public class LogIgnoreFiledSerializeFilter implements ContextValueFilter {

    public static final LogIgnoreFiledSerializeFilter INSTANCE = new LogIgnoreFiledSerializeFilter();

    private static final Map<Field, Boolean> ignoreCache = new ConcurrentHashMap<>();

    @Override
    public Object process(BeanContext context, Object object, String name, Object value) {
        //如果field为null则直接返回
        if (context.getField() == null)
            return value;

        //添加对应的缓存
        Boolean exists = ignoreCache.computeIfAbsent(context.getField(), jsonField -> {
            //获取判断field上是否存在朱姐
            LogIgnore annotation = AnnotationUtils.findAnnotation(jsonField, LogIgnore.class);
            return annotation == null;
        });


        return !exists ? null : value;
    }
}
