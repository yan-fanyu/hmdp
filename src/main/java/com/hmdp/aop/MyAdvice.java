//package com.hmdp.aop;
//
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Before;
//import org.aspectj.lang.annotation.Pointcut;
//import org.springframework.context.annotation.Bean;
//import org.springframework.stereotype.Component;
//
//// 定义切面类
//@Component
//@Aspect
//public class MyAdvice {
//    // 定义切入点
//    @Pointcut("execution(void com.hmdp.dao.Book.print())")
//    private void pt(){
//
//    }
//
//    // 绑定切入点与通知
//    @Before("pt()")
//    public void method(){
//        System.out.println(System.currentTimeMillis());
//    }
//
//    @Around("")
//    public void test(){
//
//    }
//}
