package com.hmdp.test;

public class Main2 {

    public static void main(String[] args) {
        SimpleOptimisticLock lock = new SimpleOptimisticLock();

        // 线程1尝试更新
        new Thread(() -> {
            int[] current = lock.get();
            System.out.println("线程1读取: value=" + current[0] + ", version=" + current[1]);

            // 模拟处理时间
            try { Thread.sleep(100); } catch (InterruptedException e) {}

            boolean success = lock.update(100, current[1]);
            System.out.println("线程1更新" + (success ? "成功" : "失败"));
        }).start();

        // 线程2尝试更新
        new Thread(() -> {
            int[] current = lock.get();
            System.out.println("线程2读取: value=" + current[0] + ", version=" + current[1]);

            boolean success = lock.update(200, current[1]);
            System.out.println("线程2更新" + (success ? "成功" : "失败"));
        }).start();
    }
}