package com.avni.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Counts clicks within the last second. Fed from {@code MouseMixin} on the
 * render thread; read from the HUD on the same thread, so no synchronization
 * is needed.
 */
public class CpsTracker {
    public static final CpsTracker LEFT = new CpsTracker();
    public static final CpsTracker RIGHT = new CpsTracker();

    private final Deque<Long> clicks = new ArrayDeque<>();

    public void click() {
        clicks.addLast(System.currentTimeMillis());
    }

    public int cps() {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
            clicks.pollFirst();
        }
        return clicks.size();
    }
}
