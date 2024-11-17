--入参部分
local tokens_key = KEYS[1]
--令牌桶刷新的时间戳。
local timestamp_key = KEYS[2]
--redis.log(redis.LOG_WARNING, "tokens_key " .. tokens_key)

--令牌生产的速率
local rate = tonumber(ARGV[1])
--令牌桶的容量
local capacity = tonumber(ARGV[2])
--当前时间戳
local now = tonumber(ARGV[3])
--当前请求的令牌数，默认为1
local requested = tonumber(ARGV[4])

--填满空桶需要的时间
local fill_time = capacity/rate
--两倍的填满时间，作为key的失效时间。防止key太多，占用空间
local ttl = math.floor(fill_time*2)

--redis.log(redis.LOG_WARNING, "rate " .. ARGV[1])
--redis.log(redis.LOG_WARNING, "capacity " .. ARGV[2])
--redis.log(redis.LOG_WARNING, "now " .. ARGV[3])
--redis.log(redis.LOG_WARNING, "requested " .. ARGV[4])
--redis.log(redis.LOG_WARNING, "filltime " .. fill_time)
--redis.log(redis.LOG_WARNING, "ttl " .. ttl)

--当前桶中剩余令牌数。如果key不存在，初始化桶的容量数，即默认为满
local last_tokens = tonumber(redis.call("get", tokens_key))
if last_tokens == nil then
    last_tokens = capacity
end
--redis.log(redis.LOG_WARNING, "last_tokens " .. last_tokens)

--上次刷新时间。如果key不存在，初始化为0
local last_refreshed = tonumber(redis.call("get", timestamp_key))
if last_refreshed == nil then
    last_refreshed = 0
end
--redis.log(redis.LOG_WARNING, "last_refreshed " .. last_refreshed)

--距离上次限流的时间差
local delta = math.max(0, now - last_refreshed)
--时间差内能够恢复多少令牌
local filled_tokens = math.min(capacity, last_tokens + (delta * rate))
--申请的令牌足够
local allowed = filled_tokens >= requested
local new_tokens = filled_tokens

--申请结果标识，0表示失败，1表示成功
local allowed_num = 0
--如果申请令牌通过，重新计算剩余令牌数。
if allowed then
    new_tokens = filled_tokens - requested
    allowed_num = 1
end

--redis.log(redis.LOG_WARNING, "delta " .. delta)
--redis.log(redis.LOG_WARNING, "filled_tokens " .. filled_tokens)
--redis.log(redis.LOG_WARNING, "allowed_num " .. allowed_num)
--redis.log(redis.LOG_WARNING, "new_tokens " .. new_tokens)

--将本次申请后剩余令牌数，和key更新时间，写回redis，并设置过期时间
redis.call("setex", tokens_key, ttl, new_tokens)
--将key更新时间，写回redis，并设置过期时间

--如果间隔时间 > capacity / rate 表示已经生产过令牌了，则重置时间，或者当timestamp_key过期的时候
if delta > capacity / rate or redis.call("exists", timestamp_key) == 0 then
    redis.call("setex", timestamp_key, ttl, now)
end

--返回令牌申请结果，和剩余令牌数
return allowed_num