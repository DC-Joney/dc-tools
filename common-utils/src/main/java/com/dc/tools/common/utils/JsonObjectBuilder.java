package com.dc.tools.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * 用于构建复杂的json，通过Builder的方式提供构建功能
 *
 * @author zhangyang
 */
@SuppressWarnings("all")
public class JsonObjectBuilder {

    private final JSONObject jsonObject;

    private static final JsonObjectBuilder DUMMY = new JsonObjectBuilder();

    private final JsonObjectBuilder parent;

    private int depth = 0;

    private JsonObjectBuilder() {
        this.jsonObject = null;
        this.parent = null;
    }

    public JsonObjectBuilder(JsonObjectBuilder parent) {
        this.jsonObject = new JSONObject(true);
        this.parent = parent;
        this.depth = parent.depth++;
    }

    public JsonObjectBuilder(JsonObjectBuilder parent, JSONObject jsonObject) {
        this.jsonObject = jsonObject == null ? new JSONObject() : jsonObject;
        this.parent = parent;
        this.depth = parent.depth++;
    }

    public static JsonObjectBuilder empty() {
        return JsonObjectBuilder.DUMMY;
    }

    public boolean isEmpty() {
        return this == DUMMY;
    }

    public JsonObjectBuilder put(String name, Object value) {
        Object newElement = value;
        if (newElement instanceof JsonObjectBuilder) {
            JsonObjectBuilder builder = (JsonObjectBuilder) newElement;
            newElement = builder.build();
        }

        jsonObject.put(name, newElement);
        return this;
    }

    public JsonObjectBuilder put(String name, JsonObjectBuilder builder) {
        JSONObject newElement = builder.build();
        jsonObject.put(name, newElement);
        return this;
    }


    public JsonObjectBuilder putAll(JSONObject jsonObject) {
        jsonObject.putAll(jsonObject);
        return this;
    }

    public JsonObjectBuilder putIfAbsent(String name, Object value) {
        jsonObject.putIfAbsent(name, value);
        return this;
    }

    public JsonObjectBuilder putArray(String name, JSONArray jsonArray) {
        jsonObject.put(name, jsonArray);
        return this;
    }

    public JsonObjectBuilder putArray(String name, JsonArrayBuilder arrayBuilder) {
        jsonObject.put(name, arrayBuilder.build());
        return this;
    }

    public JsonArrayBuilder findArray(String name, boolean needCopy) {
        JSONArray jsonArray = jsonObject.getJSONArray(name);
        JsonArrayBuilder arrayBuilder = jsonArray == null || !needCopy ?
                JsonArrayBuilder.create(this, name, jsonArray) : JsonArrayBuilder.createNew(this, name, jsonArray);
        jsonObject.put(name, arrayBuilder.jsonArray);
        return arrayBuilder;
    }

    public JsonArrayBuilder findArray(String name) {
        return findArray(name, false);
    }

    public JSONArray findJsonArray(String name) {
        return jsonObject.getJSONArray(name);
    }


    public JsonObjectBuilder newChild(String name) {
        JSONObject jsonObject = this.jsonObject.getJSONObject(name);
        JsonObjectBuilder builder = new JsonObjectBuilder(this, jsonObject);
        this.jsonObject.put(name, builder.jsonObject);
        return builder;
    }

    public JsonObjectBuilder parent() {
        if (parent == DUMMY)
            return null;

        return parent;
    }

    public JsonObjectBuilder replaceName(String oldKey, String newKey) {
        Object json = this.jsonObject.remove(oldKey);
        if (json != null) {
            this.jsonObject.put(newKey, json);
        }

        return this;
    }

    public JsonObjectBuilder replaceValue(String key, Object newValue) {
        this.jsonObject.put(key, newValue);
        return this;
    }

    public <V> JsonObjectBuilder replaceValue(String key, BiFunction<JSONObject, String, V> replaceFunction) {
        V newValue = replaceFunction.apply(this.jsonObject, key);
        //如果新值为null则忽略新值
        if (newValue != null) {
            this.jsonObject.put(key, newValue);
        }

        return this;
    }


    public JSONObject build() {
        JsonObjectBuilder parent = this;
        //找到最顶层节点
        while (parent.parent != DUMMY) {
            parent = parent.parent;
        }

        return parent.jsonObject;
    }

    public static JsonObjectBuilder create() {
        return new JsonObjectBuilder(DUMMY);
    }


    public static JsonObjectBuilder fromJsonObject(JsonObjectBuilder parent, JSONObject jsonObject) {
        return new JsonObjectBuilder(parent, jsonObject);
    }

    public static JsonObjectBuilder fromJsonObject(JSONObject jsonObject) {
        return new JsonObjectBuilder(JsonObjectBuilder.DUMMY, jsonObject);
    }

    public static JsonObjectBuilder fromNewObject(JSONObject jsonObject) {
        JSONObject newObject = new JSONObject();
        newObject.putAll(jsonObject);
        return new JsonObjectBuilder(DUMMY, newObject);
    }

    public JsonObjectBuilder findObject(String data) {
        return JsonObjectBuilder.fromJsonObject(this, this.jsonObject.getJSONObject(data));
    }

    public JsonObjectBuilder findNewObject(String data) {
        return JsonObjectBuilder.fromNewObject(this.jsonObject.getJSONObject(data));
    }

    public JsonObjectBuilder remove(String key) {
        this.jsonObject.remove(key);
        return this;
    }


    /**
     * 用于构建JsonArray类型的json
     */
    public static class JsonArrayBuilder implements Iterable<BiConsumer<Integer, JSONArray>> {

        private JSONArray jsonArray;

        private static final JsonArrayBuilder DUMMY = new JsonArrayBuilder();

        private final JsonArrayBuilder parent;

        private final JsonObjectBuilder parentBuilder;

        private int depth = 0;

        private final String attributeName;

        public JsonArrayBuilder() {
            this.jsonArray = null;
            this.parent = null;
            this.parentBuilder = null;
            this.attributeName = null;
        }

        public JsonArrayBuilder(JsonArrayBuilder parent, JsonObjectBuilder parentBuilder, String attributeName, JSONArray jsonArray) {
            this.jsonArray = jsonArray == null ? new JSONArray() : jsonArray;
            this.parent = parent;
            this.depth = parent.depth++;
            this.parentBuilder = parentBuilder;
            this.attributeName = attributeName;
        }

        public JsonArrayBuilder map(UnaryOperator<JSONArray> operator) {
            JSONArray originArray = this.jsonArray;
            this.jsonArray = operator.apply(this.jsonArray);
            if (parent != DUMMY) {
                parent.remove(originArray);
                parent.add(this.jsonArray);
            }

            //将当前数据重新放入到parentObject中
            if (parentBuilder != null && parentBuilder != JsonObjectBuilder.DUMMY && StringUtils.hasText(attributeName)) {
                parentBuilder.put(attributeName, jsonArray);
            }

            return this;
        }

        public JsonArrayBuilder add(Object element) {
            Object newElement = element;
            if (element instanceof JsonObjectBuilder) {
                JsonObjectBuilder builder = (JsonObjectBuilder) element;
                newElement = builder.build();
            }

            this.jsonArray.add(newElement);
            return this;
        }

        public JsonArrayBuilder add(JsonObjectBuilder element) {
            this.jsonArray.add(element.build());
            return this;
        }

        public JsonArrayBuilder add(int index, Object element) {
            Object newElement = element;
            if (element instanceof JsonObjectBuilder) {
                JsonObjectBuilder builder = (JsonObjectBuilder) element;
                newElement = builder.build();
            }

            this.jsonArray.add(index, newElement);
            return this;
        }


        public <R> JsonArrayBuilder replace(int index, BiFunction<JSONArray, Integer, R> function) {
            R newElement = function.apply(jsonArray, index);
            this.jsonArray.remove(index);
            this.jsonArray.add(index, newElement);
            return this;
        }

        public JsonArrayBuilder addAll(Collection<Object> elements) {
            this.jsonArray.addAll(elements);
            return this;
        }


        public <R> JsonArrayBuilder element(BiFunction<JSONArray, Integer, R> function) {
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                R newValue = function.apply(jsonArray, i);
                newArray.add(i, newValue);
            }

            this.jsonArray.clear();
            this.jsonArray.addAll(newArray);

            //将当前数据重新放入到parentObject中
            if (parentBuilder != null && parentBuilder != JsonObjectBuilder.DUMMY && StringUtils.hasText(attributeName)) {
                parentBuilder.put(attributeName, this.jsonArray);
            }

            return this;
        }

        public JsonArrayBuilder newChild() {
            JsonArrayBuilder builder = new JsonArrayBuilder(this, parentBuilder, null, null);
            this.jsonArray.add(builder.jsonArray);
            return builder;
        }

        public JsonArrayBuilder fromSource(JsonArrayBuilder arrayBuilder) {
            this.jsonArray.addAll(arrayBuilder.jsonArray);
            return this;
        }


        public JsonArrayBuilder parent() {
            if (parent == DUMMY)
                return null;

            return parent;
        }

        public JsonObjectBuilder parentObject() {
            if (parentBuilder == JsonObjectBuilder.DUMMY)
                return null;

            return parentBuilder;
        }

        public JsonArrayBuilder copyTo(JsonArrayBuilder arrayBuilder) {
            this.jsonArray.addAll(arrayBuilder.jsonArray);
            return this;
        }

        @SuppressWarnings("unchecked")
        public <R> R lastObject(Class<R> elementClass) {
            if (jsonArray.size() == 0)
                return null;

            Object object = this.jsonArray.get(jsonArray.size() - 1);
            if (elementClass.isAssignableFrom(object.getClass()))
                return (R) object;

            return null;
        }

        @SuppressWarnings("unchecked")
        public <R> R firstObject(Class<R> elementClass) {
            Object object = this.jsonArray.get(0);
            if (elementClass.isAssignableFrom(object.getClass()))
                return (R) object;

            return null;
        }

        @SuppressWarnings("unchecked")
        public <R> R indexOf(int index, Class<R> elementClass) {
            Object object = this.jsonArray.get(index);
            if (elementClass.isAssignableFrom(object.getClass()))
                return (R) object;

            return null;
        }

        public JsonArrayBuilder pushLast(Object element) {
            return add(element);
        }

        public JsonArrayBuilder pushFirst(Object element) {
            return add(0, element);
        }

        public JsonArrayBuilder removeIndex(int index) {
            this.jsonArray.remove(index);
            return this;
        }

        public JsonArrayBuilder remove(Object object) {
            this.jsonArray.remove(object);
            return this;
        }

        public JSONArray currentArray() {
            return this.jsonArray;
        }

        public JSONArray build() {
            JsonArrayBuilder parent = this;
            //找到最顶层节点
            while (parent.parent != DUMMY) {
                parent = parent.parent;
            }

            return parent.jsonArray;
        }

        private static JsonArrayBuilder create(JsonObjectBuilder builder, String name, JSONArray jsonArray) {
            return new JsonArrayBuilder(DUMMY, builder, name, jsonArray);
        }

        private static JsonArrayBuilder createNew(JsonObjectBuilder builder, String name, JSONArray jsonArray) {
            JSONArray newArray = new JSONArray();
            newArray.addAll(jsonArray);
            return new JsonArrayBuilder(DUMMY, builder, name, newArray);
        }


        public static JsonArrayBuilder from(JSONArray jsonArray) {
            return new JsonArrayBuilder(DUMMY, JsonObjectBuilder.DUMMY, null, jsonArray);
        }

        public static JsonArrayBuilder fromNew(JSONArray jsonArray) {
            JSONArray newArray = new JSONArray();
            newArray.addAll(jsonArray);
            return new JsonArrayBuilder(DUMMY, JsonObjectBuilder.DUMMY, null, newArray);
        }

        public static JsonArrayBuilder create() {
            return new JsonArrayBuilder(DUMMY, JsonObjectBuilder.DUMMY, null, null);
        }

        public static JsonArrayBuilder from(Object... element) {
            JsonArrayBuilder arrayBuilder = new JsonArrayBuilder(DUMMY, JsonObjectBuilder.DUMMY, null, null);
            return arrayBuilder.addAll(Arrays.asList(element));
        }

        public JsonArrayBuilder forEach(BiConsumer<Integer, JSONArray> consumer) {
            for (int i = 0; i < jsonArray.size(); i++) {
                consumer.accept(i, jsonArray);
            }

            return this;
        }

        public JsonArrayBuilder childArray(int index) {
            JSONArray childArray = indexOf(index, JSONArray.class);
            return new JsonArrayBuilder(this, parentBuilder, null, childArray);
        }

        public JsonArrayBuilder childNewArray(int index) {
            JSONArray childArray = indexOf(index, JSONArray.class);
            JSONArray jsonArray = new JSONArray();
            jsonArray.addAll(childArray);
            return new JsonArrayBuilder(this, parentBuilder, null, jsonArray);
        }

        @Override
        public Iterator<BiConsumer<Integer, JSONArray>> iterator() {

            int length = jsonArray.size() - 1;
            int index = 0;

            return new Iterator<BiConsumer<Integer, JSONArray>>() {
                @Override
                public boolean hasNext() {
                    return index < length;
                }

                @Override
                public BiConsumer<Integer, JSONArray> next() {
                    return null;
                }
            };
        }
    }

}
