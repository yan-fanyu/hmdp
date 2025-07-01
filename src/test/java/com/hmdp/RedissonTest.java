package com.hmdp;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        // 1 尝试获取锁 参数1 获取锁的最大等待时长(在此期间内会重试) 参数2 锁自动释放的时间 参数3 时间单位
        boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
        // 2 判断是否成功获取到锁
        if (isLock) {
            try{
                System.out.println("T1 执行业务");
            }finally {
                // 释放锁
                lock.unlock();
            }
        }
    }
}
















