//package com.hmdp.controller;
//
//
//import cn.hutool.bloomfilter.BloomFilterUtil;
//import com.hmdp.service.impl.BloomFilterService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.annotation.Resource;
//
//
//@RestController
//@RequestMapping("bloom")
//public class BloomFilterController {
//
//    @Resource
//    private BloomFilterService bloomFilterService;
//
//
//    @GetMapping("/add")
//    public void add(@RequestParam String element){
//        bloomFilterService.add(element);
//    }
//
//    @GetMapping("/check")
//    public String check(@RequestParam String element){
//        return bloomFilterService.check(element);
//    }
//
//
//}
