package com.hmdp.test;

public class SimpleOptimisticLock {
    private int value;      // 要保护的数据
    private int version;    // 版本号

    // 更新数据的方法
    public boolean update(int newValue, int expectedVersion) {
        if (this.version != expectedVersion) {
            // 版本不一致，说明数据已被其他线程修改
            return false;   // 更新失败
        }

        this.value = newValue;
        this.version++;     // 版本号增加
        return true;        // 更新成功
    }

    // 获取当前数据和版本号
    public int[] get() {
        return new int[]{value, version};
    }
}