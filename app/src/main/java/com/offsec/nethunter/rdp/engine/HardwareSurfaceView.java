package com.offsec.nethunter.rdp.engine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 60 FPS Hardware-Accelerated Remote Desktop Surface.
 * Bypasses Android UI hierarchy and draws on a dedicated background thread.
 * Eliminates garbage collection churn and UI frame drops.
 */
public class HardwareSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "HardwareSurfaceView";

    private SurfaceHolder surfaceHolder;
    private Thread renderThread;
    private volatile boolean isRunning = false;

    // Remote Framebuffer Bitmap (updated by RDP decoder)
    private Bitmap remoteFramebuffer;
    private final Object frameLock = new Object();
    private boolean hasNewFrame = false;

    // Dimensions
    private int remoteWidth = 1920;
    private int remoteHeight = 1080;
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    public HardwareSurfaceView(Context context) {
        super(context);
        init();
    }

    public HardwareSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    public void initDimensions(int width, int height) {
        this.remoteWidth = width;
        this.remoteHeight = height;
        synchronized (frameLock) {
            if (remoteFramebuffer != null && !remoteFramebuffer.isRecycled()) {
                remoteFramebuffer.recycle();
            }
            remoteFramebuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            srcRect.set(0, 0, width, height);
        }
    }

    /**
     * Called when a new decoded frame is ready from the RDP pipeline.
     */
    public void updateFrame(Bitmap newBitmap) {
        synchronized (frameLock) {
            this.remoteFramebuffer = newBitmap;
            this.hasNewFrame = true;
            frameLock.notifyAll();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        renderThread = new Thread(this, "RDP-60FPS-RenderThread");
        renderThread.setPriority(Thread.MAX_PRIORITY);
        renderThread.start();
        Log.d(TAG, "Hardware Surface created, render thread started.");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Calculate aspect ratio preserving destination rect
        float scale = Math.min((float) width / remoteWidth, (float) height / remoteHeight);
        int scaledW = (int) (remoteWidth * scale);
        int scaledH = (int) (remoteHeight * scale);
        int offsetX = (width - scaledW) / 2;
        int offsetY = (height - scaledH) / 2;

        synchronized (frameLock) {
            dstRect.set(offsetX, offsetY, offsetX + scaledW, offsetY + scaledH);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        if (renderThread != null) {
            synchronized (frameLock) {
                frameLock.notifyAll();
            }
            try {
                renderThread.join(500);
            } catch (InterruptedException ignored) {}
        }
        Log.d(TAG, "Hardware Surface destroyed.");
    }

    @Override
    public void run() {
        while (isRunning) {
            synchronized (frameLock) {
                while (isRunning && !hasNewFrame) {
                    try {
                        frameLock.wait(16); // ~60 FPS polling window
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                hasNewFrame = false;
            }

            if (!isRunning) break;

            Canvas canvas = null;
            try {
                if (surfaceHolder != null && surfaceHolder.getSurface() != null && surfaceHolder.getSurface().isValid()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            canvas = surfaceHolder.lockHardwareCanvas();
                        } catch (Throwable t) {
                            canvas = null;
                        }
                    }
                    if (canvas == null) {
                        try {
                            canvas = surfaceHolder.lockCanvas();
                        } catch (Throwable t) {
                            canvas = null;
                        }
                    }
                    if (canvas != null) {
                        canvas.drawColor(Color.BLACK);
                        synchronized (frameLock) {
                            if (remoteFramebuffer != null && !remoteFramebuffer.isRecycled()) {
                                canvas.drawBitmap(remoteFramebuffer, srcRect, dstRect, paint);
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "Hardware render error: " + e.getMessage());
            } finally {
                if (canvas != null && surfaceHolder != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public Rect getDstRect() {
        return dstRect;
    }

    public int getRemoteWidth() {
        return remoteWidth;
    }

    public int getRemoteHeight() {
        return remoteHeight;
    }
}
