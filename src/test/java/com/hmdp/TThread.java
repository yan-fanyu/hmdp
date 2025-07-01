package com.hmdp;

import java.util.*;

public class TThread extends Thread{
    private String name;

    public TThread(String name) {
        this.name = name;




    }

    public void run() {
        for (int i = 0; i < 1000; i++) {
            System.out.println(name + "-" + i);
        }
    }

    public static void main(String[] args) {
//        Set<Integer> tset = new TreeSet<>();
//        Integer[] arr = new Integer[100];
//        Arrays.sort(arr, new Comparator<Integer>() {
//            @Override
//            public int compare(Integer o1, Integer o2) {
//                return o1 - o2;
//            }
//        });
//        Set<int[]> set1 = new TreeSet<>(new Comparator<int[]>(){
//            @Override
//            public int compare(int[] o1, int[] o2) {
//                return o1[0] - o2[0];
//            }
//        });
//        set1.add(new int[10]);
//        set1.add(new int[10]);
//        set1.add(new int[10]);
//
//        System.out.println(set1.size());








    }


}
