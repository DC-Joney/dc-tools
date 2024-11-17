package com.dc.property.archaius;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.AbstractConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 并发配置
 *
 * @author zy
 */
public class ConcurrentSettableConfig extends AbstractConfig implements SettableConfig {


    private final ConcurrentHashMap<String, Object> configMap = new ConcurrentHashMap<>();

    public ConcurrentSettableConfig(String name) {
        super(name);
    }

    public ConcurrentSettableConfig() {
        super(generateUniqueName("concurrent-settable-"));
    }

    @Override
    public  <T> void setProperty(String propName, T propValue) {
        configMap.put(propName, propValue);
        notifyConfigUpdated(this);
    }

    @Override
    public void clearProperty(String propName) {
        configMap.remove(propName);
    }

    @Override
    public boolean containsKey(String key) {
        return configMap.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return configMap.isEmpty();
    }

    @Override
    public Object getRawProperty(String key) {
        return configMap.get(key);
    }

    @Override
    public Iterator<String> getKeys() {
        return configMap.keySet().iterator();
    }

    @Override
    public Iterable<String> keys() {
        return configMap.keySet();
    }

    @Override
    public void setProperties(Properties src) {
        if (null != src) {
            synchronized (this) {
                for (Map.Entry<Object, Object> prop : src.entrySet()) {
                    configMap.put(prop.getKey().toString(), prop.getValue());
                }
                notifyConfigUpdated(this);
            }
        }
    }

    @Override
    public void setProperties(Config src) {
        if (null != src) {
            synchronized (this) {
                src.forEachProperty(configMap::put);
                notifyConfigUpdated(this);
            }
        }
    }

    @Override
    public void forEachProperty(BiConsumer<String, Object> consumer) {
        configMap.forEach(consumer);
    }
}
