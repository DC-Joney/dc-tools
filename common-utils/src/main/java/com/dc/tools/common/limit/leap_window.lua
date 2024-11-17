-- redisKey
local redisKey = KEYS[1]

--时间戳
local time_stamp = redis.call("time")[1]

-- 每个窗口的长度
local windowLengthInMs = tonumber(ARGV[1])

-- window窗口的整体大小（毫秒）
local intervalInMs = tonumber(ARGV[2])

-- 窗口的长度
local windowLength = tonumber(ARGV[3])

-- 窗口内允许访问的次数
local acceptCount = tonumber(ARGV[4])

-- 数组默认值为0
local initValue = "0"

redis.log(redis.LOG_WARNING, windowLengthInMs)
redis.log(redis.LOG_WARNING, tostring(ARGV[1]))
redis.log(redis.LOG_WARNING, tonumber(ARGV[1], 10))
redis.log(redis.LOG_WARNING, type(ARGV[1]))
redis.log(redis.LOG_WARNING, type(ARGV[1]) == 'string')

-- 计算当前时间戳对应的数组下标
local function calculateTimeIdx(timeMillis)
    local timeId = timeMillis / windowLengthInMs;
    return timeId % timeMillis;
end



-- 计算当前时间戳对应滑动窗口开始时间
local function calculateWindowStart(timeMillis)
    redis.log(redis.LOG_WARNING, "time_stamp: ".. tostring(time_stamp))
    redis.log(redis.LOG_WARNING, "timeMillis: ".. tostring(timeMillis))
    redis.log(redis.LOG_WARNING, "windowLength11: ".. tostring(windowLengthInMs))
    return timeMillis - timeMillis % ARGV[1];
end

--计算当前时间开对应的窗口的开始事件
local windowStart = calculateWindowStart(time_stamp)

--计算当前时间在窗口中的index
local index = calculateTimeIdx(time_stamp)

local function resetWindow(bucket)
    bucket.windowStart = windowStart
    bucket.count = 0
end

-- 获取当前窗口
local function currentWindow(timeMillis)

    local bucketJson = redis.call("lindex", redisKey, index)

    local bucket;

    if bucketJson ~= initValue then
        bucket = cjson.decode(bucketJson);
    else
        bucket = 0
    end

    --    WindowWrap<T> window = new WindowWrap<T>(windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
    if bucket == 0 then
        bucket = {}
        bucket.windowStart = windowStart
        bucket.windowLengthInMs = windowLengthInMs
        bucket.count = 0
        return bucket;
    elseif bucket.windowStart == windowStart then
        return bucket
    elseif bucket.windowStart < windowStart then
        resetWindow(bucket)
        return bucket
    end

    return nil
end

local function addCount(timeMillis)
    local bucket = currentWindow(timeMillis)

    if bucket then
        bucket.count = bucket.count + 1
        local bucketJson = cjson.encode(bucket);
        redis.call("lset", redisKey, index, bucketJson)
    end
end

local function isLimit(timeMillis)

    if timeMillis < 0 then
        return -1
    end

    local currentTime = redis.call("TIME")

    -- 每次调用该脚本时将过期时间向后延续24小时
    redis.call("expire", redisKey, 24 * 60 * 60)

    -- 初始化数组
    initArray()

    -- 添加计数
    addCount(timeMillis)

    local size = redis.call("llen", redisKey)
    local count = 0

    for i, bucketJson in ipairs(redis.call("lrange", redisKey, 0, size)) do
        repeat
            if bucketJson and bucketJson ~= initValue then
                local bucket = json.decode(bucketJson)
                if (timeMillis - bucket.windowStart) > intervalInMs then
                    break
                end
                --统计所有的访问数
                count = count + bucket.count
            end
        until true
    end

    return acceptCount <= count and 1 or -1
end

-- 初始化数组
function initArray ()
    local exists = redis.call("exists", redisKey)

    -- 如果redis不存在这个key，则初始化将每个数组位置设置为0
    if exists == 0 then
        for i = 0, windowLength - 1 do
            redis.call("lpush", redisKey, i, initValue)
        end
    end

end

return isLimit(time_stamp)
