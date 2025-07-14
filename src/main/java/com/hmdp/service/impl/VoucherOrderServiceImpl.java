package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.hmdp.utils.RedisConstants.LOCK_ORDER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    // 注入 全局唯一 id 生成器
    // 可以传入参数 prefix 作为不同业务的 全局唯一 id生成器
    // 此处主要用于生成 全局唯一 优惠券订单id
    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RedissonClient redissonClient;

    // 获取代理对象
    private IVoucherOrderService proxy;


    // 加载 lua 脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 下面两行代码 -> 阻塞队列 + 线程池 在 seckillVoucher 判断用户具有秒杀资格后 异步处理下单的剩余操作
    // 保存异步下单任务到阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 创建线程池 创建订单、下单 并持久化到数据库
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            while (true) {
                try {
                    // 1 从队列获取一个订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2 创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }


    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 1 获取用户
        Long userId = voucherOrder.getUserId();
        // 2 创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 3 获取锁
        // 使用无参方法 失败直接返回
        boolean isLock = lock.tryLock();
        // 4 判断锁是否成功
        if(!isLock){
            // 获取锁失败 返回错误或者重试
            log.error("不允许重复下单");
            return;
        }try {
            // 创建订单 更新库存 保存订单到数据库
            proxy.createVoucherOrder(voucherOrder);
        }finally {
            // 释放锁
            lock.unlock();
        }
    }

    // 第七版 使用 Lua 脚本 异步下单优化  在redis实现 库存判断 + 一人一单
    // 终极方案
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 1 执行 lua 脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        // 判断结果是否是 0
        int r = result.intValue();
        if(r != 0){
            // 不是0 代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2 为 0 有购买资格  把下单信息保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3 订单 id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.4 用户 id
        voucherOrder.setUserId(userId);
        // 2.5 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6 放入阻塞队列
        orderTasks.add(voucherOrder);

        // 3 获取代理对象  初始化成员变量proxy
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // 4 返回订单 id
        return Result.ok(orderId);
    }


//    private class VoucherOrderHandler implements Runnable{
//        String queueName = "stream.orders";
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    // 1 获取订单中的信息
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
//                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
//                    );
//                    if(list == null || list.isEmpty()){
//                        continue;
//                    }
//                    // 3 解析消息
//                    MapRecord<String, Object, Object> record = list.get(0);
//                    Map<Object, Object> values = record.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
//                    // 4 获取成功 可以下单
//                    handleVoucherOrder(voucherOrder);
//                    // ACK 确认 消息
//                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
//                } catch (Exception e) {
//                    log.error("处理订单异常", e);
//                    handelPendingList();
//                }
//            }
//        }
//
//        private void handleVoucherOrder(VoucherOrder voucherOrder) {
//            // 1 获取用户
//            Long userId = voucherOrder.getUserId();
//            // 2 创建锁对象
//            RLock lock = redissonClient.getLock("lock:order:" + userId);
//            // 3 获取锁
//            // 使用无参方法 失败直接返回
//            boolean isLock = lock.tryLock();
//            // 4 判断锁是否成功
//            if(!isLock){
//                // 获取锁失败 返回错误或者重试
//                log.error("不允许重复下单");
//                return;
//            }try {
//                proxy.createVoucherOrder(voucherOrder);
//            }finally {
//                // 释放锁
//                lock.unlock();
//            }
//        }
//
//
//
//        private void handelPendingList() {
//            while (true) {
//                try {
//                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
//                            Consumer.from("g1", "c1"),
//                            StreamReadOptions.empty().count(1),
//                            StreamOffset.create(queueName, ReadOffset.from("0"))
//                    );
//                    if(list == null || list.isEmpty()){
//                        break;
//                    }
//                    // 3 解析消息
//                    MapRecord<String, Object, Object> record = list.get(0);
//                    Map<Object, Object> values = record.getValue();
//                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
//                    // 4 获取成功 可以下单
//                    handleVoucherOrder(voucherOrder);
//                    // ACK 确认 消息
//                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
//                } catch (Exception e) {
//                    log.error("处理 pending-list 订单异常", e);
//                    try {
//                        Thread.sleep(20);
//                    } catch (InterruptedException interruptedException) {
//                        interruptedException.printStackTrace();
//                    }
//                }
//            }
//        }
//    }


//    // 第六版 库存字段 > 0 使用 CSA 解决超卖 + 利用 Redisson 分布式锁 代替 synchronized 解决分布式下 一人一单问题
//    // 终极方案
//    @Override
//    public Result seckillVoucher(Long voucherId) throws InterruptedException {
//        // 1 获取优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//        // 使用 Redisson 分布式锁
//        RLock lock = redissonClient.getLock("lock:redisson:order:" + userId);
//        // 使用无参方法  只尝试获取一次锁  失败即返回
//        boolean isLock = lock.tryLock(10L, TimeUnit.SECONDS);
//        if (!isLock) {
//            return Result.fail("不允许重复下单");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            lock.unlock();
//        }
//    }










//    // 第五版 在 CAS 乐观锁 解决库存超卖问题 的基础上 利用简单 Redis 分布式锁 解决分布式下 一人一单问题
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1 获取优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//
//
//        Long userId = UserHolder.getUser().getId();
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        boolean isLock = lock.tryLock(1200);
//        if (!isLock) {
//            return Result.fail("不允许重复下单");
//        }
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            lock.unlock();
//        }
//
//    }




//    // 第三版 在 CAS 乐观锁 解决库存超卖问题 的基础上 实现一人一单  仍然存在问题
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1 获取优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//
//
//        Long userId = UserHolder.getUser().getId();
//
//        // 4 先判断一人一单
//        Integer count = query()
//                .eq("voucher_id", voucherId)
//                .eq("user_id", userId)
//                .count();
//        if(count > 0){
//            return Result.fail("不允许重复下单");
//        }
//
//        // 5 在执行扣减库存
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock - 1")
//                .eq("voucher_id", voucherId)
//                .ge("stock", 0)
//                .update();
//        if(!success){
//            return Result.fail("库存不足");
//        }
//
//        // 6 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 6.1 订单 id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        // 6.3 用户 id
//
//        voucherOrder.setUserId(userId);
//        // 6.4 代金券 id
//        voucherOrder.setVoucherId(voucherId);
//        // 6.5 持久化到数据库
//        save(voucherOrder);
//
//        return Result.ok(orderId);
//    }


//    // 第二版 使用 stock 字段 实现 CAS 乐观锁 实现秒杀库存超卖问题
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1 获取优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//        // 5 扣减库存
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock - 1")
//                .eq("voucher_id", voucherId)
//                .ge("stock", 0)
//                .update();
//        if(!success){
//            return Result.fail("库存不足");
//        }
//        // 6 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 2 订单 id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        // 3 用户 id
//        Long userId = UserHolder.getUser().getId();
//        voucherOrder.setUserId(userId);
//        // 4 代金券 id
//        voucherOrder.setVoucherId(voucherId);
//        // 5 持久化到数据库
//        save(voucherOrder);
//
//        return Result.ok(orderId);
//    }

    // 第四版 在 CAS 乐观锁 解决库存超卖问题 的基础上 利用 Synchronized+代理对象 实现一人一单
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1 获取优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//
//
//        Long userId = UserHolder.getUser().getId();
//
//        synchronized (userId.toString().intern()){
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }
//    }

//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1 获取用户
//        Long userId = UserHolder.getUser().getId();
//        // 2 订单 id
//        long orderId = redisIdWorker.nextId("order");
//        // 3 执行 lua 脚本
//        Long result = stringRedisTemplate.execute(
//                SECKILL_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(), userId.toString(), String.valueOf(orderId)
//        );
//        // 2 判断结果是否是 0
//        int r = result.intValue();
//        if(r != 0){
//            // 不是0 代表没有购买资格
//            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
//        }
//        // 3 获取代理对象
//        proxy = (IVoucherOrderService) AopContext.currentProxy();
//        // 4 返回订单 id
//        return Result.ok(orderId);
//    }



//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        // 1 查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否已经结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if(voucher.getStock() < 1){
//            return Result.fail("库存不足");
//        }
//        Long userId = UserHolder.getUser().getId();
//
//        // 创建锁对象
//        //SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
//        RLock lock = redissonClient.getLock(LOCK_ORDER_KEY + userId);
//        // 获取锁
//        boolean isLock = lock.tryLock();
//        if(!isLock){
//            // 获取锁失败 返回错误或者重试
//            return Result.fail("不允许重复下单");
//        }try {
//            // 获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            // 释放锁
//            lock.unlock();
//        }
//
//    }

//    // 第一版 不加锁
//    @Override
//    @Transactional
//    public Result seckillVoucher(Long voucherId) {
//        // 1 查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        // 2 判断秒杀是否开始
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        // 3 判断秒杀是否已经结束
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        // 4 判断库存是否充足
//        if(voucher.getStock() < 1){
//            return Result.fail("库存不足");
//        }
//
//
//
//        boolean success = seckillVoucherService.update().
//                setSql("stock = stock - 1")
//                .eq("voucher_id", voucherId).update();
//
//        if (!success) {
//            return Result.fail("库存不足");
//        }
//        // 1 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 2 订单 id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        // 3 用户 id
//        Long userId = UserHolder.getUser().getId();
//        voucherOrder.setUserId(userId);
//        // 4 代金券 id
//        voucherOrder.setVoucherId(voucherId);
//        // 5 持久化到数据库
//        save(voucherOrder);
//
//        return Result.ok(orderId);
//    }

//    // 第四版 在 CAS 乐观锁 解决库存超卖问题 的基础上 利用 Synchronized+代理对象 实现一人一单
//    @Transactional
//    @Override
//    public Result createVoucherOrder(Long voucherId){
//
//        Long userId = UserHolder.getUser().getId();
//        // 4 先判断一人一单
//        Integer count = query()
//                .eq("voucher_id", voucherId)
//                .eq("user_id", userId)
//                .count();
//        if(count > 0){
//            return Result.fail("不允许重复下单");
//        }
//
//        // 5 在执行扣减库存
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock - 1")
//                .eq("voucher_id", voucherId)
//                .ge("stock", 0)
//                .update();
//        if(!success){
//            return Result.fail("库存不足");
//        }
//
//        // 6 创建订单
//        VoucherOrder voucherOrder = new VoucherOrder();
//        // 6.1 订单 id
//        long orderId = redisIdWorker.nextId("order");
//        voucherOrder.setId(orderId);
//        // 6.3 用户 id
//
//        voucherOrder.setUserId(userId);
//        // 6.4 代金券 id
//        voucherOrder.setVoucherId(voucherId);
//        // 6.5 持久化到数据库
//        save(voucherOrder);
//
//        return Result.ok(orderId);
//    }



    // 秒杀优化
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5 一人一单
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();


        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            // 用户已经购买过了
            log.error("用户已经购买过一次");
            return;
        }

        // 6 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足");
            return;
        }

        save(voucherOrder);
    }
}
