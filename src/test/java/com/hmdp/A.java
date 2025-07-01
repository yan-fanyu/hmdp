package com.hmdp;

import java.util.HashMap;
import java.util.Map;

class A {
    public void q(){
        int n = 100;
        int l = n;
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i+1;
        }
        int j = 1;
        while(true){
            boolean f = false;
            for (int i = 0; i < n; i++) {
                if(arr[i] == 0){
                    continue;
                }
                if(j % 3 == 0){
                    arr[i] = 0;
                    l--;
                }
                if(l == 1){
                    f = true;
                    break;
                }
                j++;
            }
            if(f){
                break;
            }


        }
        for(int i = 0; i < n; i++){
            if(arr[i] != 0){
                System.out.println(arr[i]);
            }
        }
    }
    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<>();


    }
}