package com.dc.property.archaius;

import com.dc.tools.common.BootService;
import com.dc.tools.common.spi.CommonServiceLoader;
import com.netflix.archaius.CustomDecoder;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.api.*;
import com.netflix.archaius.config.DefaultLayeredConfig;
import com.netflix.archaius.config.EnvironmentConfig;
import com.netflix.archaius.config.SystemConfig;

import java.util.List;

/**
 * Manage all dynamic properties
 *
 * @author zy
 */
public class ConfigManager implements BootService {

    private final ConcurrentSettableConfig settableConfig = new ConcurrentSettableConfig();

    private DefaultPropertyFactory propertyFactory;

    private List<TypeConverter.Factory> factories;

    public ConfigManager() {

    }

    @Override
    public void startUp() {
        List<TypeConverter.Factory> factoryList = CommonServiceLoader.load(TypeConverter.Factory.class, this.getClass().getClassLoader())
                .sort();

        List<TypeConverter.Factory> allFactories = io.vavr.collection.List.ofAll(DefaultDecoder.DEFAULT_FACTORIES)
                .appendAll(factoryList).asJava();

        CustomDecoder decoder = CustomDecoder.create(allFactories);

        DefaultLayeredConfig config = new DefaultLayeredConfig();
        config.addConfig(Layer.of("system", 2), new SystemConfig());
        config.addConfig(Layer.of("environments", 1), new EnvironmentConfig());
        config.addConfig(Layer.of("application", 0), settableConfig);
        config.addListener(new DefaultConfigListener());
        config.setDecoder(decoder);
        this.propertyFactory = new DefaultPropertyFactory(config);
    }

    @Override
    public void shutdown() {
        propertyFactory.invalidate();
    }


    public <T> Property<T> getProperty(String property, Class<T> classType) {
        return propertyFactory.get(property, classType);
    }

    public <T> Property<List<T>> getListProperty(String property, Class<T> classType) {
        return propertyFactory.getList(property, classType);
    }


    public <T> void putProperty(String propertyKey, T value) {
        settableConfig.setProperty(propertyKey, value);
    }

    static class DefaultConfigListener implements ConfigListener {

        @Override
        public void onConfigAdded(Config config) {

        }

        @Override
        public void onConfigRemoved(Config config) {

        }

        @Override
        public void onConfigUpdated(Config config) {

        }

        @Override
        public void onError(Throwable error, Config config) {

        }
    }

}
