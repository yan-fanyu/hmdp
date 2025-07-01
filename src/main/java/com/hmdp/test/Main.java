package com.hmdp.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException {
        Person p = new Student();

        Class pClass = p.getClass();

        // 获取类的所有方法
        Method[] methods = pClass.getMethods();
        for(Method method: methods){
            System.out.println(method);
        }

        // 展示类所有的字段
        Field[] fields = pClass.getDeclaredFields();
        for(Field field: fields){
            System.out.println(field);
        }

        // 展示类所有的构造方法
        Constructor[] constructors = pClass.getDeclaredConstructors();
        System.out.println("Constructors");
        for(Constructor constructor: constructors){
            System.out.println(constructor);
        }
    }
}
