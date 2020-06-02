package ru.kpfu.itis.diploma.backend.util;

/**
 * alarm!!! кинет ошибку после Integer.MAX_VALUE кадров :)
 */
public class FpsCounter {
    private static final int PRECISION = 100;

    private final long[] timestamps = new long[PRECISION];
    private volatile int index = -1;

    public int framesCount() {
        return index;
    }

    public synchronized void newFrame() {
        timestamps[++index % timestamps.length] = System.currentTimeMillis();
    }

    public synchronized double fps() {
        if (index < 1) {
            return 0;
        } else if (index >= timestamps.length) {
            long prev = timestamps[(index + 1) % timestamps.length];
            long cur = timestamps[index % timestamps.length];
            return 1000.0 * timestamps.length / (cur - prev);
        } else {
            return 1000.0 * index / (timestamps[index] - timestamps[0]);
        }
    }
}
