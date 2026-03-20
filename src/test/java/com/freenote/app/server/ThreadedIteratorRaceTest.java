package com.freenote.app.server;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.fail;

public class ThreadedIteratorRaceTest {


@Test
public void testRace() throws Exception {
        ThreadedIterator iterator = new ThreadedIterator();

        // Use 4 threads to increase collisions
        int threadCount = 4;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Runnable task = () -> {
            try {
                while (true) {
                    try {
                        if (!iterator.hasNext()) {
                            break;
                        }
                        iterator.next();
                    } catch (IllegalStateException e) {
                        fail("Race condition detected: IllegalStateException from next()", e);
                    }
                }
            } finally {
                latch.countDown();
            }
        };

        // Start all threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(task);
        }

        // Wait for all to finish
        latch.await();
        executor.shutdown();
    }
}

