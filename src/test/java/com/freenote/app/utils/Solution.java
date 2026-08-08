package com.freenote.app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Solution {
    public int minimumSize(int[] nums, int maxOperations) {
        int min = 1;
        int max = -1;
        for (int val: nums){
            if (max < 0 || max < val){
                max = val;
            }
        }
        int result = max;
        while (min <= max){
            int mid = min + (max - min) / 2;
            int op = 0;
            for (int val : nums) {
                op += (val - 1) / mid;
            }
//            System.out.println(mid);
            if (op <= maxOperations){
                result = mid;
                max = mid - 1;
            }
            else {
                min = mid + 1;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        Solution solution = new Solution();
//        System.out.println(solution.minimumSize(new int[]{9}, 200));
//        System.out.println(solution.minimumSize(new int[]{9}, 2));
//        System.out.println(solution.minimumSize(new int[]{2,4,8,2}, 4));
        System.out.println(solution.minimumSize(new int[]{1000000000,1000000000,1000000000}, 1000000000));
    }

    private int countPath(int[][] dp, int row, int col, int m, int n){
        if (dp[row][col] != 0){
            return dp[row][col];
        }
        dp[row][col] = countPath(dp, row + 1, col, m , n) + countPath(dp, row, col + 1, m , n) + countPath(dp, row + 1, col + 1, m , n);
        return dp[row][col];
    }
    public int uniquePaths(int m, int n) {
        int[][] dp = new int[m + 1][n + 1];
        dp[m][n] = 1;
        dp[m][n + 1] = 1;
        dp[m + 1][n] = 1;
        return countPath(dp, 0, 0, m, n);
    }


    public String removeStars(String s) {
        Stack<Character> stack = new Stack<>();
        for (char c : s.toCharArray()){
            if (c == '*'){
                if (!stack.isEmpty()){
                    stack.pop();
                }
            }
            else {
                stack.push(c);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (char c : stack){
            sb.append(c);
        }
        return sb.toString();
    }


    public int[] asteroidCollision(int[] asteroids) {
        Stack<Integer> stack = new Stack<>();
        for (int ast : asteroids){
            if (ast > 0){
                stack.push(ast);
            }
            else {
                while (!stack.isEmpty() && stack.peek() > 0 && stack.peek() < -ast){
                    stack.pop();
                }
                if (!stack.isEmpty() && stack.peek() == -ast){
                    stack.pop();
                }
                else if (stack.isEmpty() || stack.peek() < 0){
                    stack.push(ast);
                }
            }
        }
        int[] result = new int[stack.size()];
        for (int i = result.length - 1; i >= 0; i--){
            result[i] = stack.pop();
        }
        return result;
    }

    public void longestOne(int[] nums, int k){
        List<ArrayList<Integer>> res = new ArrayList<>();
        for (int i = 0; i < nums.length; i++){

        }
    }
}
