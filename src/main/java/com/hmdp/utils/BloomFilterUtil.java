package com.hmdp.utils;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * BloomFilterConfig : 布隆过滤器配置文件
 *
 * @author zyw
 * @create 2024-01-10  14:43
 */

@Slf4j
@Component
public class BloomFilterUtil {

    // 布隆过滤器的初始大小
    @Value("${bloomFilter.MIN_EXPECTED_INSERTIONS}")
    private int MIN_EXPECTED_INSERTIONS;

    //预期插入数据量
    private int EXPECTED_INSERTIONS;

    //布隆过滤器的错误率
    @Value("${bloomFilter.bloomFilterErrorRate}")
    private double FPP;

    //布隆过滤器的最大使用率
    @Value("${bloomFilter.maximumUtilization}")
    private double maximumUtilization;

    //布隆过滤器的最小使用率
    @Value("${bloomFilter.minimumUtilization}")
    private double minimumUtilization;

    // 布隆过滤器的初始序列号
    @Value("${bloomFilter.RBloomFilterSequence}")
    public int RBloomFilterSequence;

    // 布隆过滤器的容量自适应定时任务频率
    private static final String CRON_EXPANSION = "0 0/5 * * * ?";

    public BloomFilter<String> bloomFilter;

    public RBloomFilter<String> rBloomFilter;

    @Resource
    private RedissonClient redissonClient;


    /**
     * 初始化基于JVM本地缓存构建布隆过滤器
     */
    @PostConstruct
    public void builBloomFilter() {
        EXPECTED_INSERTIONS = MIN_EXPECTED_INSERTIONS;
        // 创建并返回BloomFilter对象
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("UTF-8")), EXPECTED_INSERTIONS, FPP);
    }

    /**
     * 初始化基于Redis防止数据库查询的布隆过滤器
     *
     * @return RBloomFilter<String> 布隆过滤器
     */
    @PostConstruct
    public void buidUserRegisterCachePenetrationBloomFilter() {
        EXPECTED_INSERTIONS = MIN_EXPECTED_INSERTIONS;
        RBloomFilter<String> cachePenetrationBloomFilter = getRBloomFilter();
        cachePenetrationBloomFilter.tryInit(EXPECTED_INSERTIONS, FPP);
        initRBloomFilter(cachePenetrationBloomFilter);
        rBloomFilter = cachePenetrationBloomFilter;
    }

    /**
     * 布隆过滤器初始化
     */
    private void initRBloomFilter(RBloomFilter<String> cachePenetrationBloomFilter) {
        List<String> names = Arrays.asList("黄袁哲", "杨清涵", "陈国宇", "李康", "舒适玉", "王昊", "王浩然");
        names.parallelStream().forEach(cachePenetrationBloomFilter::add);
    }


    /**
     * 获取布隆过滤器
     */
    public RBloomFilter<String> getRBloomFilter() {
        try {
            RBloomFilter<String> bloomFilter;
            //布隆过滤器序号判断
            bloomFilter = redissonClient.getBloomFilter("filter");


            if (bloomFilter == null) {
                throw new IllegalStateException("布隆过滤器不存在于Redis中");
            }

            return bloomFilter;
        } catch (Exception e) {
            log.error("从Redis获取布隆过滤器失败", e);
            throw new IllegalStateException("从Redis获取布隆过滤器失败", e);
        }
    }

    /**
     * 定时动态扩容任务配置
     */
    @Scheduled(cron = CRON_EXPANSION)
    public void dilatation() {
        // 获取当前布隆过滤器实例
        RBloomFilter<String> cachePenetrationBloomFilter = getRBloomFilter();
        // 计算当前装载因子
        long count = cachePenetrationBloomFilter.count();
        double loadFactor = (double) count / EXPECTED_INSERTIONS;
        // 检查是否需要扩容
        if (loadFactor > maximumUtilization) {
            log.info("布隆过滤器开始进行扩容", "插入元素数", count, "期望插入元素数", EXPECTED_INSERTIONS);
            // 将期望插入元素数翻倍
            EXPECTED_INSERTIONS *= 2;
            try {
                // 更新布隆过滤器序列号
                RBloomFilterSequence = RBloomFilterSequence == 1 ? 2 : 1;
                //获取新布隆过滤器实例
                RBloomFilter<String> newRBloomFilter = getRBloomFilter();
                // 尝试使用新的期望插入元素数初始化布隆过滤器
                newRBloomFilter.tryInit(EXPECTED_INSERTIONS, FPP);
                //数据初始化
                initRBloomFilter(newRBloomFilter);
                //切换成新布隆过滤器
                rBloomFilter = newRBloomFilter;
                //清除旧的缓存数据
                cachePenetrationBloomFilter.delete();
                log.info("当前布隆过滤器序号：" + RBloomFilterSequence);
            } catch (Exception e) {
                log.error("布隆过滤器初始化过程中出现异常", e);
            }
            log.info("布隆过滤器完成扩容，新容量为：" + EXPECTED_INSERTIONS);
        } else {
            log.info("当前布隆过滤器未达到扩容/缩容条件");
        }
    }
}
