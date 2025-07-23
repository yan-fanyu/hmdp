//package com.hmdp.service.impl;
//
//
//
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//
///**
// * RBloomFilterService : 基于Redis的布隆过滤器服务
// *
// * @author zyw
// * @create 2024-01-10  15:38
// */
//
//@Service
//@Slf4j
//public class BloomFilterService {
//
//
//
//    /**
//     * 新增数据到布隆过滤器中
//     *
//     * @param element
//     */
//    public String add(String element) {
//        return bloomFilterUtil.rBloomFilter.add(element) ? "插入成功" : "插入失败";
//    }
//
//    /**
//     * 检查数据是否存在布隆过滤器中
//     *
//     * @param element
//     * @return
//     */
//    public String check(String element) {
//        log.info("序号：{}", bloomFilterUtil.RBloomFilterSequence);
//        log.info("元素个数：{}", bloomFilterUtil.rBloomFilter.count());
//        log.info("期望插入数：{}", bloomFilterUtil.rBloomFilter.getExpectedInsertions());
//        log.info("假阳性概率：{}", bloomFilterUtil.rBloomFilter.getFalseProbability());
//        return bloomFilterUtil.rBloomFilter.contains(element) ? "存在" : "不存在";
//    }
//}
