package com.offsec.nethunter.rdp.input;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.offsec.nethunter.rdp.engine.HardwareSurfaceView;
import com.offsec.nethunter.rdp.engine.InputEventCoalescer;

/**
 * NetHunter KeX Precision Touchpad Controller.
 * Provides virtual trackpad emulation:
 * - Relative pointer motion with acceleration curve
 * - Tap to left click
 * - 2-finger tap to right click
 * - 2-finger drag to scroll wheel
 * - Long press / double-tap-and-hold to drag
 */
public class TouchpadController implements View.OnTouchListener {

    public interface RdpInputBridge {
        void sendPointerEvent(int x, int y, int buttonMask);
        void sendScrollEvent(int delta);
    }

    // RDP Pointer Button Masks
    public static final int PTR_BUTTON_NONE = 0x0000;
    public static final int PTR_BUTTON_LEFT = 0x1000;
    public static final int PTR_BUTTON_RIGHT = 0x2000;
    public static final int PTR_BUTTON_MIDDLE = 0x4000;

    private final HardwareSurfaceView surfaceView;
    private final ImageView cursorView;
    private final RdpInputBridge inputBridge;
    private final InputEventCoalescer coalescer;

    // Virtual cursor position in remote desktop coordinates (0..remoteWidth, 0..remoteHeight)
    private float cursorX = 960f;
    private float cursorY = 540f;

    // Tracking state
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isDragging = false;
    private long lastTapTime = 0;
    private int pointerCount = 0;

    public TouchpadController(Context context, HardwareSurfaceView surfaceView, ImageView cursorView, RdpInputBridge bridge) {
        this.surfaceView = surfaceView;
        this.cursorView = cursorView;
        this.inputBridge = bridge;
        this.coalescer = new InputEventCoalescer(new InputEventCoalescer.OnDispatchListener() {
            @Override
            public void onDispatchPointer(int x, int y, int buttonMask) {
                if (inputBridge != null) {
                    inputBridge.sendPointerEvent(x, y, buttonMask);
                }
            }
        });

        cursorView.setVisibility(View.VISIBLE);
        updateCursorVisual();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        pointerCount = event.getPointerCount();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                long now = SystemClock.uptimeMillis();
                if (now - lastTapTime < 250) {
                    // Double tap: start drag
                    isDragging = true;
                    coalescer.flushImmediate((int) cursorX, (int) cursorY, PTR_BUTTON_LEFT);
                }
                lastTapTime = now;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 2) {
                    // 2-Finger tap detection initialized
                    lastTouchX = (event.getX(0) + event.getX(1)) / 2f;
                    lastTouchY = (event.getY(0) + event.getY(1)) / 2f;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 1) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();

                    // Apply precision acceleration curve
                    float distance = (float) Math.hypot(dx, dy);
                    float accel = 1.0f + (distance / 25.0f);
                    if (accel > 3.0f) accel = 3.0f;

                    cursorX += dx * accel;
                    cursorY += dy * accel;

                    clampCursor();
                    updateCursorVisual();

                    int btn = isDragging ? PTR_BUTTON_LEFT : PTR_BUTTON_NONE;
                    coalescer.queuePointerMove((int) cursorX, (int) cursorY, btn);
                } else if (pointerCount == 2) {
                    // 2-Finger Scroll
                    float currentMidY = (event.getY(0) + event.getY(1)) / 2f;
                    float scrollDelta = currentMidY - lastTouchY;
                    lastTouchY = currentMidY;

                    if (Math.abs(scrollDelta) > 10f) {
                        int delta = scrollDelta > 0 ? 120 : -120;
                        if (inputBridge != null) {
                            inputBridge.sendScrollEvent(delta);
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    isDragging = false;
                    coalescer.flushImmediate((int) cursorX, (int) cursorY, PTR_BUTTON_NONE);
                } else if (SystemClock.uptimeMillis() - lastTapTime < 180) {
                    // Single Click
                    sendTapClick(PTR_BUTTON_LEFT);
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                if (pointerCount == 2 && (event.getEventTime() - event.getDownTime() < 250)) {
                    // 2-Finger tap ➔ Right Click
                    sendTapClick(PTR_BUTTON_RIGHT);
                }
                return true;
        }
        return false;
    }

    private void sendTapClick(final int button) {
        coalescer.flushImmediate((int) cursorX, (int) cursorY, button);
        surfaceView.postDelayed(new Runnable() {
            @Override
            public void run() {
                coalescer.flushImmediate((int) cursorX, (int) cursorY, PTR_BUTTON_NONE);
            }
        }, 50);
    }

    private void clampCursor() {
        int maxW = surfaceView.getRemoteWidth();
        int maxH = surfaceView.getRemoteHeight();
        if (cursorX < 0) cursorX = 0;
        if (cursorY < 0) cursorY = 0;
        if (cursorX > maxW) cursorX = maxW;
        if (cursorY > maxH) cursorY = maxH;
    }

    private void updateCursorVisual() {
        Rect dst = surfaceView.getDstRect();
        if (dst.width() <= 0 || dst.height() <= 0) return;

        float screenX = dst.left + (cursorX / surfaceView.getRemoteWidth()) * dst.width();
        float screenY = dst.top + (cursorY / surfaceView.getRemoteHeight()) * dst.height();

        cursorView.setX(screenX);
        cursorView.setY(screenY);
    }
}
