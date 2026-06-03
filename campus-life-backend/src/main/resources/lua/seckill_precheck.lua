-- KEYS[1] stockKey
-- KEYS[2] userSetKey
-- KEYS[3] beginKey
-- KEYS[4] endKey
-- ARGV[1] userId
-- ARGV[2] nowEpochSecond

local userId = ARGV[1]
local now = tonumber(ARGV[2])

local beginAt = tonumber(redis.call('GET', KEYS[3]) or '0')
if beginAt > 0 and now < beginAt then
    return 3
end

local endAt = tonumber(redis.call('GET', KEYS[4]) or '0')
if endAt > 0 and now > endAt then
    return 4
end

local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock <= 0 then
    return 1
end

if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
    return 2
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], userId)
return 0
