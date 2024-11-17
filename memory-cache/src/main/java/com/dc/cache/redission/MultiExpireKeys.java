package com.dc.cache.redission;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.MapOptions;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用于多过期key的情况，因为在一些情况下可能需要{@code  keys} 或者是 {@code  scan} 等方式取出对应的key <br/>
 * <p>
 * 所以通过 {@link  RMapCache} 来支持批量操作
 *
 * <ul>
 *     <li>{@code keys} 命令会导致redis阻塞，尤其是在上千万或者是上亿key的场景下</li>
 *     <li>{@code scan} 命令虽然不会阻塞redis，但是会导致执行时间边长，对业务不太友好</li>
 * </ul>
 *
 * @author zy
 */
public class MultiExpireKeys implements InitializingBean {

    private static final String REDIS_KEY_PREFIX = "multi_expire_keys_%s";


    private final RedissonClient redissonClient;

    private final String redisKey;

    private RMapCache<String, Object> multiKeys;

    private final ConversionService registry;

    public MultiExpireKeys(RedissonClient redissonClient, String multiKey) {
        this.redisKey = String.format(REDIS_KEY_PREFIX, multiKey);
        this.redissonClient = redissonClient;
        this.registry = DefaultConversionService.getSharedInstance();
        afterPropertiesSet();
    }

    @Override
    public void afterPropertiesSet() {
        MapOptions<String, Object> mapOptions = MapOptions.<String, Object>defaults()
                .writeMode(MapOptions.WriteMode.WRITE_BEHIND);

        multiKeys = redissonClient.getMapCache(this.redisKey, mapOptions);
    }

    /**
     * 添加对应的value
     *
     * @param key      需要添加的key
     * @param value    需要添加的value
     * @param duration 过期时间
     */
    public void putValue(String key, Object value, Duration duration) {
        multiKeys.put(key, value, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取对应的数据
     *
     * @param key 需要添加的key
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, TypeDescriptor descriptor) {
        Object value = multiKeys.getOrDefault(key, null);
        if (value == null) {
            return null;
        }

        TypeDescriptor sourceDescriptor = TypeDescriptor.valueOf(value.getClass());
        if (registry.canConvert(TypeDescriptor.valueOf(value.getClass()), descriptor)) {
            return (T) registry.convert(value, sourceDescriptor, descriptor);
        }

        throw new ConversionFailedException(sourceDescriptor, descriptor, value,
                new IllegalArgumentException(StrUtil.format("The value type {} cannot be converted to type {}", sourceDescriptor, descriptor)));
    }

    /**
     * 删除对应的redis key，并且从zset中也清除
     *
     * @param key 需要被删除的key
     */
    public void delete(String key) {
        multiKeys.fastRemove(key);
    }

    /**
     * 通过hkeys 取出所有的key
     */
    public Set<String> readAllKeys() {
        return multiKeys.readAllKeySet();
    }

    /**
     * 通过scan的方式取出所有的key
     */
    public Set<String> scanAllKeys() {
        return multiKeys.readAllKeySet();
    }

    /**
     * 删除内部所有的元素
     */
    public void deleteAll() {
        multiKeys.delete();
    }


    public RMapCache<String, Object> getMultiKeys() {
        return multiKeys;
    }


}
