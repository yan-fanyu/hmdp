package com.hmdp;

public class Test {
    public static void main(String[] args) {
        TThread thread1 = new TThread("T1");
        TThread thread2 = new TThread("T2");
        thread1.start();
        thread2.start();
    }
}
