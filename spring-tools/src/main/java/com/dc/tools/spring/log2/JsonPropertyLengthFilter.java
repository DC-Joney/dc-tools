package com.dc.tools.spring.log2;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.PropertyFilter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用于过滤属性
 *
 * @author zy
 */
public class JsonPropertyLengthFilter implements PropertyFilter {

    private static final JsonPropertyLengthFilter INSTANCE = new JsonPropertyLengthFilter();

    private final List<JsonLengthFilter> filters = new CopyOnWriteArrayList<>();

    @Override
    public boolean apply(Object object, String name, Object value) {
        //如果是jsonObject类型
        if (JSONObject.class.isAssignableFrom(object.getClass())
                && value != null
                && String.class.isAssignableFrom(value.getClass())) {
            JSONObject dataObject = (JSONObject) object;
            return filters.stream()
                    .noneMatch(filter -> filter.support(dataObject, name, (String) value));
        }

        return true;
    }


    public static JsonPropertyLengthFilter getInstance() {
        return INSTANCE;
    }

    public JsonPropertyLengthFilter addFilter(JsonLengthFilter filter) {
        this.filters.add(filter);
        return this;
    }

    public interface JsonLengthFilter {

        boolean support(JSONObject jsonObject, String name, String value);

    }
}
