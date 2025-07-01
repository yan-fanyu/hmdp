package com.hmdp;

import org.apache.tomcat.jni.Time;

public class Wait {
    public void test() throws InterruptedException {
        this.wait();

        Time.sleep(1000);
        System.out.println("Test");


    }
}
