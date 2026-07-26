package otel.metrics.threads;

import jdk.jfr.consumer.RecordingStream;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks live virtual threads via JFR events emitted by the JVM itself
 * (jdk.VirtualThreadStart / jdk.VirtualThreadEnd). No manual instrumentation
 * at thread-creation sites is required.
 *
 * Note: only virtual threads started AFTER {@link #start()} are observed,
 * so this must be started eagerly at application boot.
 */
public class VirtualThreadTracker {
    private final AtomicLong liveVirtualThreads = new AtomicLong(0);
    private final AtomicLong totalVirtualThreadsStarted = new AtomicLong(0);
    private RecordingStream recordingStream;
    private Thread streamThread;

    public synchronized void start() {
        if (recordingStream != null) {
            return;
        }
        recordingStream = new RecordingStream();
        recordingStream.enable("jdk.VirtualThreadStart");
        recordingStream.enable("jdk.VirtualThreadEnd");
        recordingStream.onEvent("jdk.VirtualThreadStart", event -> {
            liveVirtualThreads.incrementAndGet();
            totalVirtualThreadsStarted.incrementAndGet();
        });
        recordingStream.onEvent("jdk.VirtualThreadEnd", event -> liveVirtualThreads.decrementAndGet());
        streamThread = new Thread(recordingStream::start, "virtual-thread-tracker-jfr");
        streamThread.setDaemon(true);
        streamThread.start();
    }

    public synchronized void stop() {
        if (recordingStream != null) {
            recordingStream.close();
            recordingStream = null;
        }
    }

    public long getLiveVirtualThreads() {
        return liveVirtualThreads.get();
    }

    public long getTotalVirtualThreadsStarted() {
        return totalVirtualThreadsStarted.get();
    }
}
