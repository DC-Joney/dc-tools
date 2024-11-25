package com.dc.tools.spring.http;

import cn.hutool.core.map.MapUtil;
import com.dc.tools.spring.bean.BeanUtils;
import com.dc.tools.spring.bean.CopyIgnoreAll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于请求数据
 *
 * @author zy
 */
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequest<T> {

    /**
     * 请求的id
     */
    private String globalId;

    /**
     * 超时时间
     */
    private int resourceTimeoutMills;


    @CopyIgnoreAll
    private T body;


    /**
     * 用于存储扩展信息
     */
    private final Map<String, Object> extensions = new HashMap<>();

    public HttpRequest<T> addExtension(String key, Object value) {
        extensions.put(key, value);
        return this;
    }

    public <V> V getOrDefault(String key, Class<V> valueClass, V defaultValue) {
        V v = MapUtil.get(extensions, key, valueClass);
        if (v == null) {
            v = defaultValue;
        }

        return v;
    }

    @SuppressWarnings("unchecked")
    public <V> HttpRequest<V> mutate(V body) {
        return BeanUtils.copyPropertiesWithClass(this, HttpRequest.class)
                .setBody(body);
    }

}
