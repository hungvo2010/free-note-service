package com.freenote.app.server;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

public class ThreadedIterator implements Iterator<Integer> {
    private int value;

    @Override
    public synchronized boolean hasNext() {try {
        Thread.sleep(new Random().nextInt(1, 5)); // FORCE timing gap
    } catch (InterruptedException ignored) {}
        return value < 10000;
    }

    @Override
    public synchronized Integer next() { try {
        Thread.sleep(new Random().nextInt(1, 5)); // FORCE timing gap
    } catch (InterruptedException ignored) {}
        if (value >= 10000) {
            throw new IllegalStateException("No more elements");
        }
        value += 1;
        return value;
    }

    @Override
    public void remove() {
        Iterator.super.remove();
    }

    @Override
    public void forEachRemaining(Consumer action) {
        Iterator.super.forEachRemaining(action);
    }
}
