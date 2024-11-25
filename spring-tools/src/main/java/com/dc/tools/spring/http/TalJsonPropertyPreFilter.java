package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用于过滤属性
 *
 * @author zy
 */
@Deprecated
public class TalJsonPropertyPreFilter implements PropertyPreFilter {

    private static final TalJsonPropertyPreFilter INSTANCE = new TalJsonPropertyPreFilter();

    private static final SerializeConfig serializeConfig;
    static {
        serializeConfig = new SerializeConfig();
        serializeConfig.addFilter(Map.class, TalJsonPropertyPreFilter.INSTANCE);
    }


    private final List<JsonNameFilter> jsonNameFilters = new CopyOnWriteArrayList<>();


    @Override
    public boolean apply(JSONSerializer serializer, Object object, String name) {
        //如果是jsonObject类型
        if (JSONObject.class.isAssignableFrom(object.getClass())) {
            JSONObject dataObject = (JSONObject) object;
            if (RequestAttributeUtils.containsMarkOrigin(dataObject)) {
                return jsonNameFilters.stream()
                        .noneMatch(jsonNameFilter -> jsonNameFilter.support(dataObject, name));
            }
        }

        return true;
    }

    public static TalJsonPropertyPreFilter getInstance() {
        return INSTANCE;
    }

    public TalJsonPropertyPreFilter addFilter(JsonNameFilter filter) {
        this.jsonNameFilters.add(filter);
        return this;
    }

    public static SerializeConfig getSerializeConfig() {
        return serializeConfig;
    }

    public interface JsonNameFilter {

        boolean support(JSONObject jsonObject, String name);

    }
}
