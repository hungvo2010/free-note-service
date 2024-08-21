package com.freenote.app.test.TestIteratorModified;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestIteratorModified {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        List<Integer> list = new ArrayList<>();
        list.iterator();
        for (int i=0; i < 10000; ++i){
            list.add(i);
        }
        List<Integer> synchronizedS = Collections.synchronizedList(list);
        ExecutorService executorService  = Executors.newFixedThreadPool(100);
        Runnable traverseTask = () -> {
           for (int i: synchronizedS){
               System.out.println(i);
           }
        };
        executorService.execute(traverseTask);
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
            executorService.execute(removeTask);
            executorService.execute(addTask);
        }
    }
}
