//import java.util.HashMap;
//import java.util.Map;
//
//class Solution {
//    public int cn2(int x){
//        return x * (x - 1) / 2;
//    }
//    public int countTrapezoids(int[][] points) {
//        int MOD = 1_000_000_007;
//        Map<Integer, Integer> map = new HashMap<>();
//        int mmm = 0;
//        for(int[] p: points){
//            int x = p[0];
//            int y = p[1];
//            map.put(y, map.getOrDefault(y, 0) + 1);
//        }
//        long ans = 0;
//
//        int[] arr = map.e
//                .filter(e ->  >= 2)
//                .mapToInt(Map.Entry::getKey)
//                .toArray();
//        int res = 0;
//        for (int i = 0; i < arr.length; ++i) {
//            int y1 = arr[i];
//            int m = yToXs.get(y1).size();
//            for (int j = i + 1; j < arr.length; ++j) {
//                int y2 = arr[j];
//                int n = yToXs.get(y2).size();
//                long cnt = (long) m * (m - 1) / 2 % MOD;
//                cnt = cnt * (n * (n - 1) / 2 % MOD) % MOD;
//                res = (res + (int) cnt) % MOD;
//            }
//        }
//
//        return res;
//    }
//}©leetcode