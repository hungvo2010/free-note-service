package com.freenote.app.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainThreadIterator {
    private static final Logger log = LogManager.getLogger(MainThreadIterator.class);

    public static void main(String[] args) {
        ThreadedIterator iterator = new ThreadedIterator();
        Runnable firstRunnable = () -> {
            while (iterator.hasNext()) {
                log.info("Thread: {}", iterator.next());
            }
        };
        Thread threadA = new Thread(firstRunnable);
        Thread threadB = new Thread(firstRunnable);
        Thread threadC = new Thread(firstRunnable);
        Thread threadD = new Thread(firstRunnable);
        threadA.start();
        threadB.start();
        threadC.start();
        threadD.start();
    }
}
