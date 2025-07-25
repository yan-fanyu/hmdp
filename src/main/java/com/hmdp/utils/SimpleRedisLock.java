//package com.hmdp.utils;
//
//import cn.hutool.core.lang.UUID;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.script.DefaultRedisScript;
//
//
//import java.util.Collections;
//import java.util.concurrent.TimeUnit;
//
//public class SimpleRedisLock implements ILock{
//    private String name;
//    private StringRedisTemplate stringRedisTemplate;
//
//    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
//        this.name = name;
//        this.stringRedisTemplate = stringRedisTemplate;
//    }
//
//    private static final String KEY_PREFIX = "lock:";
//    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
//    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
//    static {
//        UNLOCK_SCRIPT = new DefaultRedisScript<>();
//        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
//        UNLOCK_SCRIPT.setResultType(Long.class);
//    }
//
//    @Override
//    public boolean tryLock(long timeoutSec) {
//        // 获取线程表示
//        long id = Thread.currentThread().getId();
//        // 获取锁
//        Boolean success = stringRedisTemplate.opsForValue().
//                setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);
//        // 防止空指针
//        return Boolean.TRUE.equals(success);
//    }
//
//    @Override
//    public void unlock() {
//        // 调用 lua 脚本
//        stringRedisTemplate.execute(
//                UNLOCK_SCRIPT,
//                Collections.singletonList(KEY_PREFIX + name),
//                ID_PREFIX + Thread.currentThread().getId());
//    }
//
//    @Override
//    public void unlockLua() {
//
//    }
//
////    @Override
////    public void unlockLua() {
////        // 调用 lua 脚本
////        stringRedisTemplate.execute(
////                UNLOCK_SCRIPT,
////                Collections.singletonList(KEY_PREFIX + name),
////                ID_PREFIX + Thread.currentThread().getId());
////    }
//
//
//
////    @Override
////    public void unlock() {
////        // 获取线程表示
////        String threadId = ID_PREFIX + Thread.currentThread().getId();
////        // 获取锁的标识
////        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
////        // 判断标识是否一致  判断是否是自己的锁
////        if(threadId.equals(id)){
////            // 释放锁
////            stringRedisTemplate.delete(KEY_PREFIX + name);
////        }
////    }
//}
