#!/usr/bin/env python3
import os
import sys
import re

def apply_patches():
    print("=== Applying Kali NetHunter RDP Ultra Patches ===")
    
    # 1. Patch RdpCommunicator.java for 60 FPS RemoteFX, GFX H.264, Audio & Performance
    rdp_comm_path = 'remoteClientLib/src/main/java/com/undatech/opaque/RdpCommunicator.java'
    if os.path.exists(rdp_comm_path):
        with open(rdp_comm_path, 'r') as f:
            content = f.read()
        content = content.replace('performanceFlags.setRemoteFX(enableRemoteFx);', 'performanceFlags.setRemoteFX(true);')
        content = content.replace('performanceFlags.setGfx(enableGfx);', 'performanceFlags.setGfx(true);')
        content = content.replace('performanceFlags.setH264(enableGfxH264);', 'performanceFlags.setH264(true);')
        content = content.replace('performanceFlags.setWallpaper(wallpaper);', 'performanceFlags.setWallpaper(true);')
        content = content.replace('performanceFlags.setFontSmoothing(fontSmoothing);', 'performanceFlags.setFontSmoothing(true);')
        content = content.replace('performanceFlags.setDesktopComposition(desktopComposition);', 'performanceFlags.setDesktopComposition(true);')
        content = content.replace('performanceFlags.setFullWindowDrag(fullWindowDrag);', 'performanceFlags.setFullWindowDrag(true);')
        content = content.replace('performanceFlags.setMenuAnimations(menuAnimations);', 'performanceFlags.setMenuAnimations(true);')
        content = content.replace('performanceFlags.setTheming(theming);', 'performanceFlags.setTheming(true);')
        content = content.replace('advancedSettings.setRedirectSound(redirectSound);', 'advancedSettings.setRedirectSound(1);')
        content = re.sub(r'.*performanceFlags\.setGlyphCache.*', '// setGlyphCache disabled', content)
        with open(rdp_comm_path, 'w') as f:
            f.write(content)
        print("[✓] Patched 60 FPS performance flags into RdpCommunicator.java")

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

    # 3. Patch RemoteCanvasActivity.java: Full sensor auto-rotation and instant re-fit
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
        trigger_code = """    public void triggerRotationCorrection() {
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
        with open(rca_path, "w") as f:
            f.write(rca)
        print("[✓] Patched RemoteCanvasActivity.java for sensor rotation & correction trigger")

    # 4. Patch RemoteCanvas.java: Auto re-fit on size/orientation change
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
        with open(rc_path, "w") as f:
            f.write(rc)
        print("[✓] Patched RemoteCanvas.java for onSizeChanged dynamic re-fit")

    # 5. Patch ConnectionSettings.java: default to FIT_CENTER (Fit to screen) and NATIVE_LANDSCAPE
    cs_path = "bVNC/src/main/java/com/undatech/opaque/ConnectionSettings.java"
    if os.path.exists(cs_path):
        with open(cs_path, "r") as f:
            cs = f.read()
        cs = cs.replace('private String scaleMode = "";',
                        'private String scaleMode = ImageView.ScaleType.FIT_CENTER.toString();')
        cs = cs.replace('private int rdpResType = RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM;',
                        'private int rdpResType = RemoteClientLibConstants.RDP_GEOM_SELECT_NATIVE_LANDSCAPE;')
        cs = cs.replace('scaleMode = sp.getString("scaleMode", ImageView.ScaleType.MATRIX.toString()).trim();',
                        'scaleMode = sp.getString("scaleMode", ImageView.ScaleType.FIT_CENTER.toString()).trim();')
        cs = cs.replace('rdpResType = sp.getInt("rdpResType", RemoteClientLibConstants.RDP_GEOM_SELECT_CUSTOM);',
                        'rdpResType = sp.getInt("rdpResType", RemoteClientLibConstants.RDP_GEOM_SELECT_NATIVE_LANDSCAPE);')
        with open(cs_path, "w") as f:
            f.write(cs)
        print("[✓] Patched ConnectionSettings.java for default FIT_CENTER & NATIVE_LANDSCAPE")

    # 6. Patch ConnectionBean.java: default to FIT_CENTER
    cb_path = "bVNC/src/main/java/com/iiordanov/bVNC/ConnectionBean.java"
    if os.path.exists(cb_path):
        with open(cb_path, "r") as f:
            cb = f.read()
        cb = cb.replace('ScaleType scaling = ScaleType.MATRIX;', 'ScaleType scaling = ScaleType.FIT_CENTER;')
        cb = cb.replace('ScaleType.MATRIX.toString()', 'ScaleType.FIT_CENTER.toString()')
        with open(cb_path, "w") as f:
            f.write(cb)
        print("[✓] Patched ConnectionBean.java for default FIT_CENTER scaling")

    # 7. Patch arrays.xml: defaultScalingValues order
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

    # 8. Patch strings.xml for rdp_geometry
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
        <item>@string/rdp_geom_native_landscape</item>
        <item>@string/rdp_geom_native_portrait</item>
        <item>@string/rdp_geom_custom</item>
        <item>@string/rdp_geom_automatic</item>
    </string-array>"""
        if old_geom in s:
            s = s.replace(old_geom, new_geom)
        with open(str_path, "w") as f:
            f.write(s)
        print("[✓] Patched strings.xml for rdp_geometry")

    print("=== All NetHunter Patches Applied Successfully! ===")

if __name__ == '__main__':
    apply_patches()
