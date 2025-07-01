--1 参数列表
--1.1 优惠券id
local voucherId = ARGV[1]
--1.2 用户id
local userID = ARGV[2]
--1.3 订单id
local orderID = ARGV[3]



--2 数据key
--2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
--2.2 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3 脚本业务
-- 3.1 判断库存是否充足
if(tonumber(redis.call('get', stockKey)) <= 0) then
    -- 3.2 库存不足 返回1
    return 1
end


-- 3.2 判断用户是否已经下过单
if(redis.call('sismember', orderKey, userID) == 1) then
    -- 3.3 存在 说明是重复下单 返回2
    return 2
end

-- 3.4 扣库存 stockKey + (-1)
redis.call('incrby', stockKey, -1)
-- 3.5 下单 保存用户
redis.call('sadd', orderKey, userID)
-- 3.6 发送消息到队列中
redis.call('xadd', 'streams.orders', '*', 'userId', userID, 'voucherId', voucherId, 'id', orderID)
return 0


