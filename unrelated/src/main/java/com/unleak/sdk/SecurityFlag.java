package com.unleak.sdk;

import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

public class SecurityFlag {
    public int code(int[] nums, int maxOperations){
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        Arrays.stream(nums).forEach(maxHeap::add);
        while (maxOperations > 0){
            int maxVal = maxHeap.peek();
            System.out.println("max: " + maxVal);int pieces = maxOperations + 1;

            if (maxVal < 2 || pieces <= 1) {
                return maxVal;
            }

            int maxPossibleFirst = (maxVal + pieces - 1) / pieces;

            System.out.println("first: " + maxPossibleFirst);
            int remainder = maxVal - maxPossibleFirst;

            maxHeap.remove(maxVal);
            maxHeap.add(remainder);
            maxHeap.add(maxPossibleFirst);
            maxOperations--;
        }
        return maxHeap.peek();
    }

    public static void main(String[] args) {
        SecurityFlag sf = new SecurityFlag();
        System.out.println(sf.code(new int[]{1000000000,1000000000,1000000000}, 1000000000));
//        System.out.println(sf.code(new int[]{9}, 2));
    }
}
