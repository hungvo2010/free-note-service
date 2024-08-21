package com.freenote.app.test.TestIteratorModified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestIteratorModified {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<Integer> list = Arrays.asList(11, 2, 43, 2, 4, 2, 4, 5, 3);
        List<Integer> synchronizedS = Collections.synchronizedList(list);
        ExecutorService executorService  = Executors.newFixedThreadPool(100);
        Runnable traverseTask = () -> {
           for (int i: list){
               System.out.println(i);
           }
        };
        Runnable removeTask = () -> {
            int i = (int)Math.floor(list.size() * Math.random() );
            synchronizedS.remove(i);
        };
        Runnable addTask = () -> {
            int newVal = (int)Math.floor(list.size() * Math.random() );
            synchronizedS.add(newVal);
        };
        List<CompletableFuture<?>> all = new ArrayList<>()    ;
        for (int i = 0; i< 100;++i){
            all.add(CompletableFuture.runAsync(removeTask));
            all.add(CompletableFuture.runAsync(addTask));
            all.add(CompletableFuture.runAsync(traverseTask));
        }
        CompletableFuture.allOf(all.toArray(new CompletableFuture[0]))
                .get();
    }
}
