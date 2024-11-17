-- 用于保证消息幂等，如果消息存在失败可重复消费，当达到最大失败次数，则忽略改消息
if redis.call('exists', KEYS[1]) == 0 then
    redis.call('hset', KEYS[1], KEYS[2], ARGV[3]);
    redis.call('hset', KEYS[1], KEYS[3], 0);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return 1;

elseif redis.call('hget', KEYS[1], KEYS[2]) > 0 then
    return 0;

elseif redis.call('hget', KEYS[1], KEYS[2]) < 0 then
    local fail_count = tonumber(redis.call('hget', KEYS[1], KEYS[3]));

    if fail_count >= ARGV[2] then
        return 0;
    end

    redis.call('hincrby', KEYS[1], KEYS[3], 1);
    return 1;
end ;

return 0;


-- 当消息消费成功时设置成功状态
if redis.call('exists', KEYS[1]) == 1 then
    redis.call('hset', KEYS[1], KEYS[2], ARGV[1]);
    return 1;
end
return 0;


local value = ARGV[1]
redis.log(redis.LOG_WARNING, "value:"..value)



-- 当消息消费成功时设置失败状态
if redis.call('exists', KEYS[1]) == 1 then
    redis.call('hincrby', KEYS[1], KEYS[3], 1);
    return 1;
end
return 0;