package com.hmdp;

import net.bytebuddy.description.type.TypeDefinition;

public class MThread implements Runnable{
    @Override
    public void run() {


    }

    public void test() throws InterruptedException {
        Thread.sleep(1);
        wait(100);
    }
}
