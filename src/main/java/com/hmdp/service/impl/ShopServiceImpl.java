package com.hmdp.service.impl;

import cn.hutool.bloomfilter.BloomFilter;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.serialize.JSONDeserializer;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.print.ServiceUI;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
//        Shop shop = cacheClient.
//                queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);



        // 逻辑过期解决缓存击穿问题
        Shop shop = cacheClient.
                queryWithLoginExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if(shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        // 1 更新数据库
        updateById(shop);
        // 2 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);

        return Result.ok();
    }

//    // 添加锁
//    public boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    // 释放锁
//    public void unlock(String key){
//        stringRedisTemplate.delete(key);
//    }

//    public Shop queryWithPassThrough(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        // 1 从 redis 查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//        // 2 判断 redis 是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3 redis 中存在
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        // 判断是否是空值
//        if(shopJson != null){
//            return null;
//        }
//        // 4 不存在 根据 id 查询数据库
//        Shop shop = getById(id);
//        // 5 不存在 返回错误
//        if(shop == null){
//            // 将空值写入 redis
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            // 返回错误信息
//            return null;
//        }
//        //  6 存在 写入 redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        // 7 返回
//        return shop;
//    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//
//    public Shop queryWithLoginExpire(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        // 1 从 redis 查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//        // 2 判断 redis 是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            return null;
//        }
//
//        // 4 命中 需要先把 json 反序列化 为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        // 5 判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())){
//            // 5.1 未过期 返回店铺信息系
//            return shop;
//        }
//
//        // 5.2 过期 需要缓存重建
//        // 6 缓存重建
//        // 6.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        // 6.2 判断是否获取锁成功
//        if(isLock){
//            // 6.3 成功 开启独立线程 实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    this.saveShop2Redis(id, 20L);
//                }catch (Exception e){
//                    throw new RuntimeException(e);
//                }finally {
//                    unlock(lockKey);
//                }
//            });
//        }
//        return shop;
//    }
//
//    public Shop queryWithMutex(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        // 1 从 redis 查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get("cache:shop:" + id);
//        // 2 判断 redis 是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 3 redis 中存在
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        // 判断是否是空值
//        if(shopJson != null){
//            return null;
//        }
//
//
//        // 4 实现缓存重建
//        // 4.1 获取互斥锁
//        String lockKey = "lock:shop:" + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            // 4.2 判断是否获取成功
//            if(!isLock){
//                // 4.3 失败 休眠并重启
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            // 4.4 成功 根据 id 查询数据库
//            shop = getById(id);
//            // 模拟重建的延时
//            Thread.sleep(200);
//
//            // 5 不存在 返回错误
//            if(shop == null){
//                // 将空值写入 redis
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                // 返回错误信息
//                return null;
//            }
//            //  6 存在 写入 redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        }catch (InterruptedException e){
//            throw new RuntimeException(e);
//        }finally {
//            // 7 释放互斥锁
//            unlock(lockKey);
//        }
//        // 8 返回
//        return shop;
//    }
//
//    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//        // 1 查询店铺信息
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        // 2 封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        // 3 写入 redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(redisData));
//
//    }
}





