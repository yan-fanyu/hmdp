![img_76.png](img_76.png)


# d第一版 不加锁 库存超卖
```java
// 第一版 不加锁
@Override
@Transactional
public Result seckillVoucher(Long voucherId) {
    // 1 查询优惠券
    SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    // 2 判断秒杀是否开始
    if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
        return Result.fail("秒杀尚未开始");
    }
    // 3 判断秒杀是否已经结束
    if(voucher.getEndTime().isBefore(LocalDateTime.now())){
        return Result.fail("秒杀已经结束");
    }
    // 4 判断库存是否充足
    if(voucher.getStock() < 1){
        return Result.fail("库存不足");
    }



    boolean success = seckillVoucherService.update().
            setSql("stock = stock - 1")
            .eq("voucher_id", voucherId).update();

    if (!success) {
        return Result.fail("库存不足");
    }
    // 1 创建订单
    VoucherOrder voucherOrder = new VoucherOrder();
    // 2 订单 id
    long orderId = redisIdWorker.nextId("order");
    voucherOrder.setId(orderId);
    // 3 用户 id
    Long userId = UserHolder.getUser().getId();
    voucherOrder.setUserId(userId);
    // 4 代金券 id
    voucherOrder.setVoucherId(voucherId);
    // 5 持久化到数据库
    save(voucherOrder);

    return Result.ok(orderId);
}

```

# 第二版 使用版本号实现乐观锁 解决超卖
![img_77.png](img_77.png)

进一步优化，使用 stock 作为版本号
![img_78.png](img_78.png)


优化：
stock 作为版本号 只要判断是否大于0 即可
而不需要判断与之前查到的 stock 是否相等
```java
// 5 扣减库存
boolean success = seckillVoucherService.update()
        .setSql("stock = stock - 1")
        .eq("voucher_id", voucherId)
        .ge("stock", 0)
        .update();
```
![img_79.png](img_79.png)

缺失：实际上使用了mysql的锁解决并发超卖问题，数据库压力很大

# 第三版 实现一人一单
一个人只能消费同一张优惠券一次
 ![img_80.png](img_80.png)

下面的代码仍然存在并发冲突问题， 一个人依然可以下多单

```java
// 第三版 在 CAS 乐观锁 解决库存超卖问题 的基础上 实现一人一单
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1 获取优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2 判断秒杀是否开始
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }
        // 3 判断秒杀是否结束
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }
        // 4 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }


        Long userId = UserHolder.getUser().getId();

        // 4 先判断一人一单
        Integer count = query()
                .eq("voucher_id", voucherId)
                .eq("user_id", userId)
                .count();
        if(count > 0){
            return Result.fail("不允许重复下单");
        }

        // 5 在执行扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .ge("stock", 0)
                .update();
        if(!success){
            return Result.fail("库存不足");
        }

        // 6 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1 订单 id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.3 用户 id

        voucherOrder.setUserId(userId);
        // 6.4 代金券 id
        voucherOrder.setVoucherId(voucherId);
        // 6.5 持久化到数据库
        save(voucherOrder);

        return Result.ok(orderId);
    }
```

# 第五版 在分布式场景下  基于 Redis 实现分布式锁
Synchronized 是锁不住 多台服务的多台JVM的
相同的一个用户在多台机器上登录

所以引入 redis 分布式锁


必须保证 设置锁 + 设置锁过期时间 为原子操作

```redis
set lock thread1 nx ex 10
```

```java
// 第五版 在 CAS 乐观锁 解决库存超卖问题 的基础上 利用简单 Redis 分布式锁 解决分布式下 一人一单问题
   @Override
   public Result seckillVoucher(Long voucherId) {
       // 1 获取优惠券
       SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
       // 2 判断秒杀是否开始
       if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
           return Result.fail("秒杀尚未开始");
       }
       // 3 判断秒杀是否结束
       if(voucher.getEndTime().isBefore(LocalDateTime.now())){
           return Result.fail("秒杀已经结束");
       }
       // 4 判断库存是否充足
       if (voucher.getStock() < 1) {
           return Result.fail("库存不足");
       }


       Long userId = UserHolder.getUser().getId();
       SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
       boolean isLock = lock.tryLock(1200);
       if (!isLock) {
           return Result.fail("不允许重复下单");
       }
       try {
           // 获取代理对象
           IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
           return proxy.createVoucherOrder(voucherId);
       }finally {
           // 释放锁
           lock.unlock();
       }

   }
```

# 第六版 解决业务超时 redis分布式锁提前释放
导致该业务未执行完成 锁提前释放 另外的业务拿到锁
而等到当前业务执行完成 后 ，释放了正在使用该所的其他业务

![img_81.png](img_81.png)

解决方法：在释放锁的时候判断一下锁的标识与自己是否一致
![img_82.png](img_82.png)

注意：标识不可以直接使用线程id 因为多台JVM的线程id 可能会一致重复
使用 UUID + 线程id 作为 分布式锁 的持有者标志

```java
package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程表示
        long id = Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().
                setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);
        // 防止空指针
        return Boolean.TRUE.equals(success);
    }


    @Override
    public void unlock() {
        // 获取线程表示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁的标识
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断标识是否一致  判断是否是自己的锁
        if(threadId.equals(id)){
            // 释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}



```




# 第七版 GC 导致分布式锁失效、
释放锁阻塞 导致锁被其他业务拿走
阻塞完成后 该业务释放其他业务的锁


根本原因：判断锁标识和释放锁不是原子操作

解决方法 Lua 脚本 编写多条 redis 命令 原子执行

```java
package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程表示
        long id = Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().
                setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);
        // 防止空指针
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 调用 lua 脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }

    @Override
    public void unlockLua() {

    }

//    @Override
//    public void unlockLua() {
//        // 调用 lua 脚本
//        stringRedisTemplate.execute(
//                UNLOCK_SCRIPT,
//                Collections.singletonList(KEY_PREFIX + name),
//                ID_PREFIX + Thread.currentThread().getId());
//    }



//    @Override
//    public void unlock() {
//        // 获取线程表示
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        // 获取锁的标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        // 判断标识是否一致  判断是否是自己的锁
//        if(threadId.equals(id)){
//            // 释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}

```



# 优化 Redis Set nx 实现分布式锁的 问题 -> 使用 Redisson

问题1 不可重入
问题2 不可重试
问题3 超时释放
问题4 主从一致

![img_83.png](img_83.png)

# Redisson 快速入门 -> 之前学的可以不用

使用 Redisson 的锁 替换 自己实现的分布式锁
