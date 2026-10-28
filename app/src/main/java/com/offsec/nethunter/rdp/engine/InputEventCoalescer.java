package com.offsec.nethunter.rdp.engine;

import android.os.Handler;
import android.os.Looper;

/**
 * High-Speed Input Event Coalescer & Throttler.
 * Prevents TCP socket buffer saturation caused by high-polling-rate touch screens (120Hz-240Hz).
 * Ensures mouse coordinates are dispatched at exactly the screen refresh rate (60Hz / 16.6ms),
 * discarding intermediate lag-inducing backlog.
 */
public class InputEventCoalescer {

    public interface OnDispatchListener {
        void onDispatchPointer(int x, int y, int buttonMask);
    }

    private final OnDispatchListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long intervalMs = 16; // 60 FPS Target (16.6ms)

    private int latestX = 0;
    private int latestY = 0;
    private int latestButtons = 0;
    private boolean isScheduled = false;

    private final Runnable dispatchRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (InputEventCoalescer.this) {
                isScheduled = false;
                if (listener != null) {
                    listener.onDispatchPointer(latestX, latestY, latestButtons);
                }
            }
        }
    };

    public InputEventCoalescer(OnDispatchListener listener) {
        this.listener = listener;
    }

    /**
     * Submit raw pointer movement coordinates.
     * Intermediate redundant coordinates are coalesced automatically.
     */
    public synchronized void queuePointerMove(int x, int y, int buttonMask) {
        this.latestX = x;
        this.latestY = y;
        this.latestButtons = buttonMask;

        if (!isScheduled) {
            isScheduled = true;
            handler.postDelayed(dispatchRunnable, intervalMs);
        }
    }

    /**
     * Instant flush for urgent events like Click Down/Up or Keypresses.
     */
    public synchronized void flushImmediate(int x, int y, int buttonMask) {
        handler.removeCallbacks(dispatchRunnable);
        isScheduled = false;
        this.latestX = x;
        this.latestY = y;
        this.latestButtons = buttonMask;
        if (listener != null) {
            listener.onDispatchPointer(x, y, buttonMask);
        }
    }
}
