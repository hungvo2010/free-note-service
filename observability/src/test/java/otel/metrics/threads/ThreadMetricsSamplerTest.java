package otel.metrics.threads;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadMetricsSamplerTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static ThreadMetricsSampler sampler;

    @BeforeAll
    static void startSampler() {
        sampler = ThreadMetricsSampler.getInstance();
        sampler.start();
    }

    @AfterAll
    static void stopSampler() {
        sampler.stop();
    }

    @Test
    void samplesPlatformThreads() throws InterruptedException {
        waitUntil(() -> sampler.getPlatformThreads() > 0);
        assertTrue(sampler.getPlatformThreads() >= 1, "platform thread count should be >= 1");
        assertTrue(sampler.getPeakThreads() >= sampler.getPlatformThreads()
                || sampler.getPeakThreads() > 0, "peak threads should be populated");
    }

    @Test
    void samplesThreadStates() throws InterruptedException {
        // the sampler thread itself is running during every sampling pass
        waitUntil(() -> sampler.getThreadStateCount(Thread.State.RUNNABLE) >= 1);

        long total = 0;
        for (Thread.State state : Thread.State.values()) {
            long count = sampler.getThreadStateCount(state);
            assertTrue(count >= 0, "state count must not be negative: " + state);
            total += count;
        }
        assertTrue(total >= 1, "per-state counts should account for live platform threads");
        // the state breakdown is sampled in the same pass as the platform count,
        // so both should roughly agree (threads may start/die mid-pass)
        assertTrue(Math.abs(total - sampler.getPlatformThreads()) <= 5,
                "state breakdown (" + total + ") should roughly match platform count ("
                        + sampler.getPlatformThreads() + ")");
    }

    @Test
    void detectsBlockedThreads() throws InterruptedException {
        Object lock = new Object();
        Thread blocked = new Thread(() -> {
            synchronized (lock) {
                // acquiring the monitor puts this thread in BLOCKED until the test releases it
            }
        }, "test-blocked-thread");

        synchronized (lock) {
            blocked.start();
            waitUntil(() -> sampler.getThreadStateCount(Thread.State.BLOCKED) >= 1);
        }
        blocked.join(10_000);
    }

    @Test
    void tracksVirtualThreadLifecycle() throws InterruptedException {
        CountDownLatch hold = new CountDownLatch(1);
        Thread vThread = Thread.ofVirtual().name("test-vt").start(() -> {
            try {
                hold.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        // virtual thread count should rise while the virtual thread is alive
        waitUntil(() -> sampler.getVirtualThreads() >= 1);

        hold.countDown();
        vThread.join(10_000);

        // and fall back after it terminates
        waitUntil(() -> sampler.getVirtualThreads() == 0);
    }

    private static void waitUntil(Check check) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (check.pass()) {
                return;
            }
            Thread.sleep(100);
        }
        assertTrue(check.pass(), "condition not met within " + TIMEOUT);
    }

    @FunctionalInterface
    private interface Check {
        boolean pass();
    }
}
