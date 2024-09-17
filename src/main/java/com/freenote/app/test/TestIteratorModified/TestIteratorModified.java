package com.freenote.app.test.TestIteratorModified;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class TestIteratorModified {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<Integer> list = new ArrayList<>();
        var res = Collections.synchronizedList(list);
//        list.iterator();
//        for (int i = 0; i < 10000; ++i) {
//            list.add(i);
//        }
        List<Integer> synchronizedS = Collections.synchronizedList(list);
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        Runnable traverseTask = () -> {
            for (int i : synchronizedS) {
                System.out.println(i);
            }
        };
//        executorService.execute(traverseTask);
        Runnable removeTask = () -> {
            int i = (int) Math.floor(list.size() * Math.random());
            synchronizedS.remove(i);
        };
        Runnable addTask = () -> {
            int newVal = (int) Math.floor(list.size() * Math.random());
            synchronizedS.add(newVal);
        };
        Runnable addItem = () -> {
            System.out.println(Thread.currentThread().getId());
            list.add(10);
        };
        List<CompletableFuture<?>> all = new ArrayList<>();
        List<Callable<Object>> futures = new ArrayList<Callable<Object>>();
        for (int i = 0; i < 10000; ++i) {
//            executorService.execute(removeTask);
//            executorService.execute(addTask);
            var future = Executors.callable(addItem);
            futures.add(future);
        }
        executorService.invokeAll(futures);
//        Runtime.getRuntime().availableProcessors();
        System.out.println(list.size());
    }
}
