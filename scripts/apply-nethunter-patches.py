#!/usr/bin/env python3
import os
import sys
import re

def apply_patches():
    print("=== Applying Kali NetHunter RDP Ultra Patches ===")
    
    # 1. Patch RdpCommunicator.java for Ultra-Low Bandwidth, Client-Side Caching & Dynamic Settings
    rdp_comm_path = 'remoteClientLib/src/main/java/com/undatech/opaque/RdpCommunicator.java'
    if os.path.exists(rdp_comm_path):
        with open(rdp_comm_path, 'r') as f:
            content = f.read()
        # In Ultra-Low Bandwidth mode, disable video streaming frames (RemoteFX / GFX / H264)
        # so RDP switches to lightweight GDI vector drawing orders and client-side bitmap/glyph caching!
        content = content.replace('performanceFlags.setRemoteFX(enableRemoteFx);', 'performanceFlags.setRemoteFX(false);')
        content = content.replace('performanceFlags.setGfx(enableGfx);', 'performanceFlags.setGfx(false);')
        content = content.replace('performanceFlags.setH264(enableGfxH264);', 'performanceFlags.setH264(false);')
        # Do not stream desktop wallpaper over the network; client renders local high-res XFCE desktop directly!
        content = content.replace('performanceFlags.setWallpaper(wallpaper);', 'performanceFlags.setWallpaper(false);')
        content = content.replace('performanceFlags.setFontSmoothing(fontSmoothing);', 'performanceFlags.setFontSmoothing(true);')
        content = content.replace('performanceFlags.setDesktopComposition(desktopComposition);', 'performanceFlags.setDesktopComposition(false);')
        # Draw outline on window drag instead of flooding video frames over the wire
        content = content.replace('performanceFlags.setFullWindowDrag(fullWindowDrag);', 'performanceFlags.setFullWindowDrag(false);')
        content = content.replace('performanceFlags.setMenuAnimations(menuAnimations);', 'performanceFlags.setMenuAnimations(false);')
        content = content.replace('performanceFlags.setTheming(theming);', 'performanceFlags.setTheming(false);')
        content = content.replace('advancedSettings.setRedirectSound(redirectSound);', 'advancedSettings.setRedirectSound(1);')
        content = re.sub(r'.*performanceFlags\.setGlyphCache.*', 'performanceFlags.setGlyphCache(true);', content)

        old_save_settings = """    private void saveConnectionSettings(int width, int height, int bpp) {
        connection.setRdpResType(RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);
        connection.setRdpWidth(width);
        connection.setRdpHeight(height);
        connection.setRdpColor(bpp);
        connection.save(context);
        updateScreenSettings(width, height, bpp);
    }"""
        new_save_settings = """    private void saveConnectionSettings(int width, int height, int bpp) {
        if (connection.getRdpResType() != RemoteClientLibConstants.RDP_GEOM_SELECT_AUTO) {
            connection.setRdpResType(RemoteClientLibConstants.RDP_GEOM_SELECT_AUTO);
        }
        connection.setRdpWidth(Math.max(width, height));
        connection.setRdpHeight(Math.min(width, height));
        connection.setRdpColor(bpp);
        connection.save(context);
        updateScreenSettings(width, height, bpp);
    }"""
        if old_save_settings in content:
            content = content.replace(old_save_settings, new_save_settings)
            print("[✓] Patched saveConnectionSettings in RdpCommunicator.java")

        with open(rdp_comm_path, 'w') as f:
            f.write(content)
        print("[✓] Patched Ultra-Low Bandwidth caching & performance flags into RdpCommunicator.java")

    # 2. Patch Manifests for fullSensor & seamless configChanges
    manifest1 = "freeaRDP-app/src/main/AndroidManifest.xml"
    if os.path.exists(manifest1):
        with open(manifest1, "r") as f:
            c = f.read()
        c = c.replace('android:configChanges="keyboardHidden"',
                      'android:screenOrientation="fullSensor"\n            android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|keyboard"')
        c = c.replace('android:screenOrientation="fullUser"', 'android:screenOrientation="fullSensor"')
        c = c.replace('android:configChanges="orientation|keyboardHidden"',
                      'android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|keyboard"')
        c = c.replace('android:name="com.iiordanov.bVNC.RemoteCanvasActivity"',
                      'android:name="com.iiordanov.bVNC.RemoteCanvasActivity"\n            android:screenOrientation="fullSensor"')
        with open(manifest1, "w") as f:
            f.write(c)
        print("[✓] Patched freeaRDP-app AndroidManifest.xml for fullSensor & configChanges")

    manifest2 = "bVNC/src/main/AndroidManifest.xml"
    if os.path.exists(manifest2):
        with open(manifest2, "r") as f:
            c2 = f.read()
        c2 = c2.replace('android:screenOrientation="fullUser"', 'android:screenOrientation="fullSensor"')
        with open(manifest2, "w") as f:
            f.write(c2)
        print("[✓] Patched bVNC AndroidManifest.xml for fullSensor")

    # 3. Patch RemoteCanvasActivity.java: Full sensor auto-rotation, dynamic auto-desktop reconnect
    rca_path = "bVNC/src/main/java/com/iiordanov/bVNC/RemoteCanvasActivity.java"
    if os.path.exists(rca_path):
        with open(rca_path, "r") as f:
            rca = f.read()

        old_settings = """            if (Utils.querySharedPreferenceBoolean(this, Constants.forceLandscapeTag))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);"""
        new_settings = """            if (Utils.querySharedPreferenceBoolean(this, Constants.forceLandscapeTag)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }"""
        if old_settings in rca:
            rca = rca.replace(old_settings, new_settings)

        trigger_code = """    private int currentOrientation = android.content.res.Configuration.ORIENTATION_UNDEFINED;
    private long lastRotationReconnectTime = 0;
    private final Runnable orientationReconnectRunnable = () -> {
        reconnectDesktopForNewOrientation();
    };

    public void reconnectDesktopForNewOrientation() {
        try {
            if (remoteConnection != null && remoteConnection.maintainConnection) {
                android.util.Log.i(TAG, "reconnectDesktopForNewOrientation: Re-negotiating desktop resolution for new orientation");
                new Thread(() -> {
                    try {
                        remoteConnection.closeConnection();
                        Thread.sleep(250);
                    } catch (Throwable ignored) {}
                    if (handler != null) {
                        handler.post(() -> {
                            try {
                                handler.sendEmptyMessage(com.undatech.opaque.RemoteClientLibConstants.REINIT_SESSION);
                            } catch (Throwable ignored) {}
                        });
                    }
                }).start();
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "reconnectDesktopForNewOrientation error: " + t.getMessage());
        }
    }

    public void triggerRotationCorrection() {
        if (handler != null) {
            handler.post(rotationCorrector);
        }
    }

    private final Runnable rotationCorrector = () -> {"""
        if "private final Runnable rotationCorrector = () -> {" in rca:
            rca = rca.replace("private final Runnable rotationCorrector = () -> {", trigger_code)

        if "remoteConnection.correctAfterRotation();" in rca:
            rca = rca.replace("remoteConnection.correctAfterRotation();",
                              "remoteConnection.correctAfterRotation();\n        canvas.postInvalidate();")

        if 'Log.d(TAG, "OnCreate complete");' in rca:
            rca = rca.replace('Log.d(TAG, "OnCreate complete");',
                              'currentOrientation = getResources().getConfiguration().orientation;\n        Log.d(TAG, "OnCreate complete");')

        old_config = """    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        controlImmersive();
        try {
            setExtraKeysVisibility(View.GONE, false);
            handler.postDelayed(rotationCorrector, 300);
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onConfigurationChanged");
        }
    }"""
        new_config = """    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        controlImmersive();
        try {
            setExtraKeysVisibility(View.GONE, false);
            handler.postDelayed(rotationCorrector, 300);

            if (newConfig.orientation != currentOrientation &&
                (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ||
                 newConfig.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)) {

                int prevOrientation = currentOrientation;
                currentOrientation = newConfig.orientation;
                if (prevOrientation != android.content.res.Configuration.ORIENTATION_UNDEFINED) {
                    long now = System.currentTimeMillis();
                    if (now - lastRotationReconnectTime > 1200) {
                        lastRotationReconnectTime = now;
                        if (handler != null) {
                            handler.removeCallbacks(orientationReconnectRunnable);
                            handler.postDelayed(orientationReconnectRunnable, 600);
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Ignoring NullPointerException during onConfigurationChanged");
        }
    }"""
        if old_config in rca:
            rca = rca.replace(old_config, new_config)

        old_fit = """        } else if (itemId == R.id.itemZoomable || itemId == R.id.itemOneToOne || itemId == R.id.itemFitToScreen) {
            AbstractScaling.getById(item.getItemId()).setScaleTypeForActivity(this);
            item.setChecked(true);
            showPanningState(false);
            return true;"""
        new_fit = """        } else if (itemId == R.id.itemZoomable || itemId == R.id.itemOneToOne || itemId == R.id.itemFitToScreen) {
            AbstractScaling.getById(item.getItemId()).setScaleTypeForActivity(this);
            item.setChecked(true);
            showPanningState(false);
            if (itemId == R.id.itemFitToScreen) {
                try {
                    boolean isScreenLandscape = (canvas.getWidth() >= canvas.getHeight());
                    boolean isDesktopLandscape = (canvas.getImageWidth() >= canvas.getImageHeight());
                    if (isScreenLandscape != isDesktopLandscape) {
                        reconnectDesktopForNewOrientation();
                    }
                } catch (Throwable ignored) {}
            }
            return true;"""
        if old_fit in rca:
            rca = rca.replace(old_fit, new_fit)

        with open(rca_path, "w") as f:
            f.write(rca)
        print("[✓] Patched RemoteCanvasActivity.java for dynamic auto-desktop rotation & reconnect")

    # 4. Patch RemoteCanvas.java: Auto re-fit on size/orientation change & dynamic desktop geometry
    rc_path = "bVNC/src/main/java/com/iiordanov/bVNC/RemoteCanvas.java"
    if os.path.exists(rc_path):
        with open(rc_path, "r") as f:
            rc = f.read()

        old_size_changed = """    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        if (w > 0 && h > 0) {
            synchronized (this) {
                this.notify();
            }
        }
    }"""
        new_size_changed = """    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        if (w > 0 && h > 0) {
            synchronized (this) {
                this.notify();
            }
            if (oldW > 0 && oldH > 0 && (w != oldW || h != oldH)) {
                post(() -> {
                    if (getContext() instanceof RemoteCanvasActivity) {
                        try {
                            ((RemoteCanvasActivity) getContext()).triggerRotationCorrection();
                        } catch (Throwable ignored) {}
                    }
                });
            }
        }
    }"""
        if old_size_changed in rc:
            rc = rc.replace(old_size_changed, new_size_changed)

        old_desired = """    @Override
    public int getDesiredWidth() {
        int w = getRemoteWidth(getWidth(), getHeight());
        if (!connection.isRequestingNewDisplayResolution() &&
                connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM) {
            w = connection.getRdpWidth();
        }
        Log.d(TAG, "Width requested: " + w);
        return w;
    }

    /**
     * Retrieves the requested remote height.
     */
    @Override
    public int getDesiredHeight() {
        int h = getRemoteHeight(getWidth(), getHeight());
        if (!connection.isRequestingNewDisplayResolution() &&
                connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM) {
            h = connection.getRdpHeight();
        }
        Log.d(TAG, "Height requested: " + h);
        return h;
    }"""
        new_desired = """    @Override
    public int getDesiredWidth() {
        int w = getRemoteWidth(getWidth(), getHeight());
        Log.d(TAG, "Width requested: " + w);
        return w;
    }

    /**
     * Retrieves the requested remote height.
     */
    @Override
    public int getDesiredHeight() {
        int h = getRemoteHeight(getWidth(), getHeight());
        Log.d(TAG, "Height requested: " + h);
        return h;
    }"""
        if old_desired in rc:
            rc = rc.replace(old_desired, new_desired)

        old_geom_calc = """    public int getRemoteWidth(int viewWidth, int viewHeight) {
        int remoteWidth;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2) {
            remoteWidth = reqWidth;
        } else if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteWidth = Math.min(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteWidth = Math.max(viewWidth, viewHeight);
        } else {
            remoteWidth = viewWidth;
        }
        // We make the resolution even if it is odd.
        if (remoteWidth % 2 == 1) remoteWidth--;
        return remoteWidth;
    }

    /**
     * Retrieves the requested remote height.
     */
    public int getRemoteHeight(int viewWidth, int viewHeight) {
        int remoteHeight;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2) {
            remoteHeight = reqHeight;
        } else if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_NATIVE_PORTRAIT) {
            remoteHeight = Math.max(viewWidth, viewHeight);
        } else if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_NATIVE_LANDSCAPE) {
            remoteHeight = Math.min(viewWidth, viewHeight);
        } else {
            remoteHeight = viewHeight;
        }
        // We make the resolution even if it is odd.
        if (remoteHeight % 2 == 1) remoteHeight--;
        return remoteHeight;
    }"""
        new_geom_calc = """    public int getRemoteWidth(int viewWidth, int viewHeight) {
        int remoteWidth;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        boolean isLandscape = (viewWidth >= viewHeight);
        if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2 && reqWidth != reqHeight) {
            remoteWidth = isLandscape ? Math.max(reqWidth, reqHeight) : Math.min(reqWidth, reqHeight);
        } else {
            int baseW = (viewWidth > 0) ? viewWidth : getWidth();
            int baseH = (viewHeight > 0) ? viewHeight : getHeight();
            if (baseW <= 0 || baseH <= 0) {
                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                baseW = dm.widthPixels;
                baseH = dm.heightPixels;
            }
            remoteWidth = isLandscape ? Math.max(baseW, baseH) : Math.min(baseW, baseH);
        }
        // We make the resolution even if it is odd.
        if (remoteWidth % 2 == 1) remoteWidth--;
        return remoteWidth;
    }

    /**
     * Retrieves the requested remote height.
     */
    public int getRemoteHeight(int viewWidth, int viewHeight) {
        int remoteHeight;
        int reqWidth = connection.getRdpWidth();
        int reqHeight = connection.getRdpHeight();
        boolean isLandscape = (viewWidth >= viewHeight);
        if (connection.getRdpResType() == RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM &&
                reqWidth >= 2 && reqHeight >= 2 && reqWidth != reqHeight) {
            remoteHeight = isLandscape ? Math.min(reqWidth, reqHeight) : Math.max(reqWidth, reqHeight);
        } else {
            int baseW = (viewWidth > 0) ? viewWidth : getWidth();
            int baseH = (viewHeight > 0) ? viewHeight : getHeight();
            if (baseW <= 0 || baseH <= 0) {
                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                baseW = dm.widthPixels;
                baseH = dm.heightPixels;
            }
            remoteHeight = isLandscape ? Math.min(baseW, baseH) : Math.max(baseW, baseH);
        }
        // We make the resolution even if it is odd.
        if (remoteHeight % 2 == 1) remoteHeight--;
        return remoteHeight;
    }"""
        if old_geom_calc in rc:
            rc = rc.replace(old_geom_calc, new_geom_calc)

        with open(rc_path, "w") as f:
            f.write(rc)
        print("[✓] Patched RemoteCanvas.java for dynamic auto-desktop geometry")

    # 5. Patch LibFreeRDP.java for Ultra-Low Bandwidth Caching & /dynamic-resolution
    lfrdp_path = "remoteClientLib/jni/libs/deps/FreeRDP/client/Android/Studio/freeRDPCore/src/main/java/com/freerdp/freerdpcore/services/LibFreeRDP.java"
    if os.path.exists(lfrdp_path):
        with open(lfrdp_path, "r") as f:
            lfc = f.read()

        target_size = 'args.add(\n\t\t    String.format("/size:%dx%d", screenSettings.getWidth(), screenSettings.getHeight()));'
        if target_size in lfc and '/dynamic-resolution' not in lfc:
            lfc = lfc.replace(target_size, target_size + '\n\t\targs.add("/dynamic-resolution");')

        cache_switches = """\t\targs.add("/dynamic-resolution");
\t\targs.add("+bitmap-cache");
\t\targs.add("+glyph-cache");
\t\targs.add("+offscreen-cache");
\t\targs.add("+compression");
\t\targs.add("/compression-level:2");
\t\targs.add("/cache:codec:rfx,codec:nsc");"""

        if 'args.add("/gdi:sw");' in lfc and '+bitmap-cache' not in lfc:
            lfc = lfc.replace('args.add("/gdi:sw");', 'args.add("/gdi:sw");\n' + cache_switches)

        lfc = lfc.replace('args.add(addFlag("glyph-cache", false));', 'args.add(addFlag("glyph-cache", true));')
        lfc = lfc.replace('args.add(addFlag("bitmap-cache", false));', 'args.add(addFlag("bitmap-cache", true));')

        with open(lfrdp_path, "w") as f:
            f.write(lfc)
        print("[✓] Patched LibFreeRDP.java for Ultra-Low Bandwidth persistent caching & dynamic resolution")

    # 6. Patch ConnectionSettings.java: default to FIT_CENTER (Fit to screen) and AUTO geometry
    cs_path = "bVNC/src/main/java/com/undatech/opaque/ConnectionSettings.java"
    if os.path.exists(cs_path):
        with open(cs_path, "r") as f:
            cs = f.read()
        cs = cs.replace('private String scaleMode = "";',
                        'private String scaleMode = ImageView.ScaleType.FIT_CENTER.toString();')
        cs = cs.replace('private int rdpResType = RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM;',
                        'private int rdpResType = RemoteClientLibConstants.RDP_GEOM_SELECT_AUTO;')
        cs = cs.replace('scaleMode = sp.getString("scaleMode", ImageView.ScaleType.MATRIX.toString()).trim();',
                        'scaleMode = sp.getString("scaleMode", ImageView.ScaleType.FIT_CENTER.toString()).trim();')
        cs = cs.replace('rdpResType = sp.getInt("rdpResType", RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);',
                        'rdpResType = sp.getInt("rdpResType", RemoteClientLibConstants.RDP_GEOM_SELECT_AUTO);')
        with open(cs_path, "w") as f:
            f.write(cs)
        print("[✓] Patched ConnectionSettings.java for default FIT_CENTER & AUTO geometry")

    # 7. Patch ConnectionBean.java: default to FIT_CENTER
    cb_path = "bVNC/src/main/java/com/iiordanov/bVNC/ConnectionBean.java"
    if os.path.exists(cb_path):
        with open(cb_path, "r") as f:
            cb = f.read()
        cb = cb.replace('ScaleType scaling = ScaleType.MATRIX;', 'ScaleType scaling = ScaleType.FIT_CENTER;')
        cb = cb.replace('ScaleType.MATRIX.toString()', 'ScaleType.FIT_CENTER.toString()')
        with open(cb_path, "w") as f:
            f.write(cb)
        print("[✓] Patched ConnectionBean.java for default FIT_CENTER scaling")

    # 8. Patch arrays.xml: defaultScalingValues order
    arr_path = "bVNC/src/main/res/values/arrays.xml"
    if os.path.exists(arr_path):
        with open(arr_path, "r") as f:
            arr = f.read()
        old_arr = """    <string-array name="defaultScalingValues">
        <item>MATRIX</item> <!-- Must match ScaleType.MATRIX -->
        <item>FIT_CENTER</item> <!-- Must match ScaleType.FIT_CENTER -->
        <item>CENTER</item> <!-- Must match ScaleType.CENTER -->
    </string-array>
    <string-array name="defaultScalingEntries">
        <item>@string/scaling_zoomable</item>
        <item>@string/fit_to_screen</item>
        <item>@string/one_to_one</item>
    </string-array>"""
        new_arr = """    <string-array name="defaultScalingValues">
        <item>FIT_CENTER</item> <!-- Must match ScaleType.FIT_CENTER -->
        <item>MATRIX</item> <!-- Must match ScaleType.MATRIX -->
        <item>CENTER</item> <!-- Must match ScaleType.CENTER -->
    </string-array>
    <string-array name="defaultScalingEntries">
        <item>@string/fit_to_screen</item>
        <item>@string/scaling_zoomable</item>
        <item>@string/one_to_one</item>
    </string-array>"""
        if old_arr in arr:
            arr = arr.replace(old_arr, new_arr)
        with open(arr_path, "w") as f:
            f.write(arr)
        print("[✓] Patched arrays.xml for default FIT_CENTER order")

    # 9. Patch strings.xml for rdp_geometry
    str_path = "bVNC/src/main/res/values/strings.xml"
    if os.path.exists(str_path):
        with open(str_path, "r") as f:
            s = f.read()
        old_geom = """    <string-array name="rdp_geometry" translatable="false">
        <item>@string/rdp_geom_native_landscape</item>
        <item>@string/rdp_geom_native_portrait</item>
        <item>@string/rdp_geom_custom</item>
    </string-array>"""
        new_geom = """    <string-array name="rdp_geometry" translatable="false">
        <item>@string/rdp_geom_automatic</item>
        <item>@string/rdp_geom_native_landscape</item>
        <item>@string/rdp_geom_native_portrait</item>
        <item>@string/rdp_geom_custom</item>
    </string-array>"""
        if old_geom in s:
            s = s.replace(old_geom, new_geom)
        with open(str_path, "w") as f:
            f.write(s)
        print("[✓] Patched strings.xml for rdp_geometry")

    # 10. Patch UltraCompactBitmapData.java: Render embedded local NetHunter XFCE desktop image
    ucb_path = "bVNC/src/main/java/com/iiordanov/bVNC/UltraCompactBitmapData.java"
    if os.path.exists(ucb_path):
        with open(ucb_path, "r") as f:
            ucb = f.read()
        target_canvas = "memGraphics = new Canvas(mbitmap);\n        drawable.startDrawing();"
        replacement_canvas = """memGraphics = new Canvas(mbitmap);
        try {
            if (c != null && c.getContext() != null) {
                android.content.Context ctx = c.getContext();
                int resId = ctx.getResources().getIdentifier("nethunter_xfce_desktop", "drawable", ctx.getPackageName());
                if (resId != 0) {
                    Bitmap localBg = android.graphics.BitmapFactory.decodeResource(ctx.getResources(), resId);
                    if (localBg != null) {
                        android.graphics.Rect src = new android.graphics.Rect(0, 0, localBg.getWidth(), localBg.getHeight());
                        android.graphics.Rect dst = new android.graphics.Rect(0, 0, bitmapwidth, bitmapheight);
                        memGraphics.drawBitmap(localBg, src, dst, null);
                        localBg.recycle();
                    }
                }
            }
        } catch (Throwable ignored) {}
        drawable.startDrawing();"""
        if target_canvas in ucb:
            ucb = ucb.replace(target_canvas, replacement_canvas)
            with open(ucb_path, "w") as f:
                f.write(ucb)
            print("[✓] Patched UltraCompactBitmapData.java with embedded XFCE desktop canvas")

    # 11. Patch ConnectionLoader.java: Pre-populate Local NetHunter Desktop (Offline) & Remote Ultra-Cache RDP
    cl_path = "bVNC/src/main/java/com/undatech/opaque/util/ConnectionLoader.java"
    if os.path.exists(cl_path):
        with open(cl_path, "r") as f:
            cl = f.read()
        target_empty = """            if (connections.isEmpty()) {
                Log.i(TAG, "No connections in the database");
            } else {"""
        replacement_empty = """            if (connections.isEmpty()) {
                Log.i(TAG, "No connections in the database, populating defaults");
                try {
                    ConnectionBean localConn = new ConnectionBean(appContext);
                    localConn.setNickname("🐉 Local NetHunter Desktop (Offline 0 MB)");
                    localConn.setAddress("127.0.0.1");
                    localConn.setPort(5901);
                    localConn.setUserName("kali");
                    localConn.save(appContext);
                    connections.add(localConn);

                    ConnectionBean remoteConn = new ConnectionBean(appContext);
                    remoteConn.setNickname("🌐 Remote RDP (Ultra-Cache Low Bandwidth)");
                    remoteConn.setAddress("192.168.1.100");
                    remoteConn.setPort(3389);
                    remoteConn.setUserName("Administrator");
                    remoteConn.setDesktopBackground(false);
                    remoteConn.save(appContext);
                    connections.add(remoteConn);
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to create default profiles", t);
                }
            }
            if (true) {"""
        if target_empty in cl:
            cl = cl.replace(target_empty, replacement_empty)
            with open(cl_path, "w") as f:
                f.write(cl)
            print("[✓] Patched ConnectionLoader.java with default Local NetHunter & Ultra-Cache profiles")

    print("=== All NetHunter Patches Applied Successfully! ===")

if __name__ == '__main__':
    apply_patches()
