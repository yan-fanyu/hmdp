package com.hmdp.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    // todo 面试 生成全局唯一id
    /*
    采用64 bit 的 long 作为全局唯一id
    第一位为 符号位 0
    然后31位 为时间戳
    最后32bit 为自增长的序列号 表示当天的数据量
    使用 redis 实现全局性
    long count = stringRedisTemplate.opsForValue()
                    .increment("icr:" + keyPrefix + ":" + date);
    redis 中的 key 设计为 "icr:" + keyPrefix + ":" + date

    即每天一个 redis key


     */


    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 1 bit 符号  +  31 bit 时间戳  +  32 bit 序列号
    public long nextId(String keyPrefix){
        // 1 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2 生成序列号
        // 2.1 获取当前日期 精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2 自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);


        // 3 拼接并返回
        return timestamp << COUNT_BITS | count;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second = " + second);
    }

}
