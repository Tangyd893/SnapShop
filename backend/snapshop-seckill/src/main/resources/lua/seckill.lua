-- 秒杀库存预扣 Lua 脚本
-- KEYS[1]: seckill:stock:{activityId}:{skuId}
-- KEYS[2]: seckill:user:{activityId}:{skuId}:{userId}
-- ARGV[1]: userId
-- 返回值：
--   0: 库存不足/库存不存在
--   1: 用户已参与
--   2: 预扣成功

local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

-- 检查库存是否存在
local stock = redis.call('get', stockKey)
if not stock then
    return 0
end

-- 检查库存是否充足
if tonumber(stock) <= 0 then
    return 0
end

-- 检查用户是否已参与
local participated = redis.call('exists', userKey)
if participated == 1 then
    return 1
end

-- 扣减库存
redis.call('decr', stockKey)
-- 标记用户已参与
redis.call('set', userKey, userId)
-- 设置用户标记过期时间（24小时，覆盖活动周期）
redis.call('expire', userKey, 86400)

return 2
