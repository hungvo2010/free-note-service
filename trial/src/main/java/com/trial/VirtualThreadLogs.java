package com.trial;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class VirtualThreadLogs {
    public static volatile int[] volatileArray = new int[1]; // volatile in array reference, not the element itself

    public static void main(String[] args) throws InterruptedException {
        var virtualThreadLogs = new VirtualThreadLogs();
        ExecutorService virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        int count = 0;
        for (int i = 0; i < 1000; i++) {
            var compareValue = virtualThreadLogs.compareValue(virtualExecutorService);
            if (compareValue) {
                count++;
            }
        }
        System.out.println("Count of all values are equal / 1000: " + count);
        virtualExecutorService.shutdown();
    }
    public boolean compareValue(ExecutorService virtualExecutorService) throws InterruptedException {
        var values = new AtomicInteger(0);
        var noLockArrays = new int[1];
        volatileArray = new int[1];
        List<Callable<Void>> tasks = IntStream.range(0, 1000)
                .mapToObj(i -> (Callable<Void>) () -> {
                    values.incrementAndGet();
                    noLockArrays[0]++;
                    volatileArray[0]++;
                    return null;
                })
                .toList();

        virtualExecutorService.invokeAll(tasks);
//        System.out.println("atomic values: " + values.get());
//        System.out.println("no lock arrays: " + noLockArrays[0]);
//        System.out.println("volatile arrays: " + volatileArray[0]);
        return values.get() == noLockArrays[0];
    }
}
