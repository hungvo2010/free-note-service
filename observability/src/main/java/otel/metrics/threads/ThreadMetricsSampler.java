package otel.metrics.threads;

import otel.sdk.config.AppProperties;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.EnumMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * High-frequency thread sampler: refreshes thread count snapshots every
 * {@code thread.metrics.sampling.interval.ms} (default 1000ms) regardless of
 * the Prometheus scrape interval. Gauges read the latest snapshot, so exposed
 * values are at most ~1s stale even if Prometheus scrapes every 15s.
 *
 * <p>Besides the aggregate counts, each pass also tallies live platform
 * threads per {@link Thread.State} so the state breakdown can be exported as
 * a single gauge tagged with a {@code state} attribute.
 */
public class ThreadMetricsSampler {
    private static final Logger log = Logger.getLogger(ThreadMetricsSampler.class.getName());
    private static final ThreadMetricsSampler INSTANCE = new ThreadMetricsSampler();

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final VirtualThreadTracker virtualThreadTracker = new VirtualThreadTracker();

    private final AtomicLong platformThreads = new AtomicLong(0);
    private final AtomicLong daemonThreads = new AtomicLong(0);
    private final AtomicLong peakThreads = new AtomicLong(0);
    private final AtomicLong virtualThreads = new AtomicLong(0);
    private final EnumMap<Thread.State, AtomicLong> threadStateCounts = new EnumMap<>(Thread.State.class);

    /** Updated on every successful sampling pass; exposed as a gauge for health monitoring. */
    private volatile long lastSampleTimeNanos = System.nanoTime();

    private ScheduledExecutorService scheduler;
    private boolean shutdownHookRegistered;

    private ThreadMetricsSampler() {
        for (Thread.State state : Thread.State.values()) {
            threadStateCounts.put(state, new AtomicLong(0));
        }
    }

    public static ThreadMetricsSampler getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        virtualThreadTracker.start();
        long intervalMs = AppProperties.getIntOrDefault("thread.metrics.sampling.interval.ms", 1000);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "thread-metrics-sampler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::sampleSafely, 0, intervalMs, TimeUnit.MILLISECONDS);
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "thread-metrics-sampler-shutdown"));
            shutdownHookRegistered = true;
        }
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        virtualThreadTracker.stop();
    }

    private void sampleSafely() {
        try {
            platformThreads.set(threadMXBean.getThreadCount());
            daemonThreads.set(threadMXBean.getDaemonThreadCount());
            peakThreads.set(threadMXBean.getPeakThreadCount());
            virtualThreads.set(virtualThreadTracker.getLiveVirtualThreads());
            sampleThreadStates();
            lastSampleTimeNanos = System.nanoTime();
        } catch (Exception e) {
            // Log so the failure is visible; do NOT catch Error — let it
            // propagate and kill the scheduler so the problem is noticed.
            log.warning("Thread metrics sampling failed — metrics will be stale: " + e);
        }
    }

    /**
     * Counts live platform threads grouped by {@link Thread.State}. The
     * {@link ThreadMXBean} does not see virtual threads (they are tracked
     * separately via JFR) and only enumerates live threads, so NEW and
     * TERMINATED are expected to stay at ~0.
     */
    private void sampleThreadStates() {
        EnumMap<Thread.State, Long> snapshot = new EnumMap<>(Thread.State.class);
        for (Thread.State state : Thread.State.values()) {
            snapshot.put(state, 0L);
        }
        for (long id : threadMXBean.getAllThreadIds()) {
            ThreadInfo info = threadMXBean.getThreadInfo(id);
            if (info == null) {
                // thread died between getAllThreadIds() and getThreadInfo()
                continue;
            }
            snapshot.merge(info.getThreadState(), 1L, Long::sum);
        }
        snapshot.forEach((state, count) -> threadStateCounts.get(state).set(count));
    }

    public long getPlatformThreads() {
        return platformThreads.get();
    }

    public long getDaemonThreads() {
        return daemonThreads.get();
    }

    public long getPeakThreads() {
        return peakThreads.get();
    }

    public long getVirtualThreads() {
        return virtualThreads.get();
    }

    /**
     * Latest sampled number of live platform threads in the given state.
     */
    public long getThreadStateCount(Thread.State state) {
        return threadStateCounts.get(state).get();
    }

    /**
     * Seconds since the last successful sampling pass.
     * Register as a gauge so Prometheus can alert when sampling is stuck
     * (e.g., when the value exceeds 2× the configured sampling interval).
     */
    public long getSecondsSinceLastSample() {
        return (System.nanoTime() - lastSampleTimeNanos) / 1_000_000_000L;
    }
}
